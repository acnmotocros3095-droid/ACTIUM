import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import ts from 'typescript';
process.env.TZ='America/La_Paz';
const memory=new Map();
globalThis.localStorage={getItem:k=>memory.get(k)??null,setItem:(k,v)=>memory.set(k,v)};
async function module(name){const raw=fs.readFileSync(new URL('../src/'+name+'.ts',import.meta.url),'utf8');const js=ts.transpileModule(raw,{compilerOptions:{module:ts.ModuleKind.ESNext,target:ts.ScriptTarget.ES2022}}).outputText;return import('data:text/javascript;base64,'+Buffer.from(js).toString('base64'))}
const {localDb:db,LOCAL_USER:uid,exportLocal,importLocal}=await module('local');
const {localDay,goalsAt,DEFAULT_GOALS,portion,weeklyWeight}=await module('metrics');
test('Weekly weight uses measured days equally and excludes older or future entries',()=>{
 const rows=[{measured_at:'2026-11-10T08:00:00-04:00',weight_kg:72},{measured_at:'2026-11-10T09:00:00-04:00',weight_kg:74},{measured_at:'2026-11-15T08:00:00-04:00',weight_kg:71},{measured_at:'2026-11-08T08:00:00-04:00',weight_kg:90},{measured_at:'2026-11-16T08:00:00-04:00',weight_kg:80}];
 assert.deepEqual(weeklyWeight(rows,'2026-11-15'),{days:2,average:72});
 assert.deepEqual(weeklyWeight([],'2026-11-15'),{days:0,average:null});
});
test('Local day respects midnight in Bolivia, rather than UTC prefix',()=>{
 assert.equal(localDay('2026-11-16T02:30:00Z'),'2026-11-15');
 assert.equal(localDay('2026-11-16T05:00:00Z'),'2026-11-16');
});
test('Goal history resets to defaults before earliest goal and preserves zero targets',()=>{
 const rows=[{effective_from:'2026-11-10',calories:2400,water_ml:0},{effective_from:'2026-11-20',calories:2500}];
 assert.deepEqual(goalsAt(rows,'2026-11-01'),DEFAULT_GOALS);
 assert.equal(goalsAt(rows,'2026-11-15').calories,2400);
 assert.equal(goalsAt(rows,'2026-11-15').water_ml,0);
});
test('Nutrition scales a 150 g portion from label values and rejects invalid input',()=>{
 const values={kcal:'200',protein_g:'10',carbs_g:'25',fat_g:'6'};
 assert.deepEqual(portion(values,150,'100g'),{kcal:300,protein_g:15,carbs_g:37.5,fat_g:9});
 assert.equal(portion(values,150,'portion').kcal,200);
 assert.throws(()=>portion({...values,kcal:'-2'},150,'100g'));
 assert.throws(()=>portion(values,0,'100g'));
});
test('Local save, upsert, export/restore and cascade preserve linked records',async()=>{
 memory.clear();
 const meal=await db.from('meals').insert({user_id:uid,meal_type:'breakfast'}).select('id').single();assert.equal(meal.error,null);
 await db.from('meal_items').insert({user_id:uid,meal_id:meal.data.id,food_name:'Prueba',grams:150,kcal:300,protein_g:15,carbs_g:30,fat_g:10});
 const before=exportLocal();
 await db.from('meals').delete().eq('id',meal.data.id);
 assert.equal((await db.from('meal_items').select()).data.length,0);
 importLocal(before);
 assert.equal((await db.from('meal_items').select()).data.length,1);
 const goal={user_id:uid,effective_from:'2026-11-15',...DEFAULT_GOALS};
 await db.from('daily_goals').upsert(goal);
 await db.from('daily_goals').upsert({...goal,calories:2400});
 assert.equal((await db.from('daily_goals').select()).data.length,1);
 assert.equal((await db.from('daily_goals').select()).data[0].calories,2400);
});
test('Invalid local values fail without overwriting previous data',async()=>{
 const before=exportLocal();
 const r=await db.from('water_entries').insert({user_id:uid,amount_ml:-250});
 assert.ok(r.error);assert.equal(exportLocal().replace(/"exported_at": "[^"]+"/,'x'),before.replace(/"exported_at": "[^"]+"/,'x'));
 const invalid=await db.from('sleep_entries').insert({user_id:uid,slept_at:'2026-11-15T20:00:00Z',woke_at:'2026-11-17T20:00:00Z',quality:4});
 assert.ok(invalid.error);
});
test('Restore rejects foreign owners, orphan links and corrupt numeric fields atomically',()=>{
 const before=JSON.parse(exportLocal());
 assert.throws(()=>importLocal(JSON.stringify({...before,tables:{water_entries:[{id:'a',user_id:'other',amount_ml:250,consumed_at:new Date().toISOString()}]}})));
 assert.throws(()=>importLocal(JSON.stringify({...before,tables:{meal_items:[{id:'x',user_id:uid,meal_id:'missing',food_name:'x',grams:100,kcal:0,protein_g:0,carbs_g:0,fat_g:0}]}})));
 assert.throws(()=>importLocal(JSON.stringify({...before,tables:{water_entries:[{id:'a',user_id:uid,amount_ml:'bad',consumed_at:new Date().toISOString()}]}})));
 assert.deepEqual(JSON.parse(exportLocal()).tables,before.tables);
});
