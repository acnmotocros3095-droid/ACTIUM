import fs from 'node:fs';
const config=JSON.parse(fs.readFileSync(new URL('../public-config.json',import.meta.url),'utf8'));
const {supabaseUrl:url,supabasePublishableKey:key}=config;
if(typeof url!=='string'||!url.startsWith('https://')||typeof key!=='string')throw Error('Falta la configuración pública de Supabase.');
if(!key.startsWith('sb_publishable_')){
 let role;try{role=JSON.parse(Buffer.from(key.split('.')[1],'base64url').toString()).role}catch{}
 if(role!=='anon')throw Error('Solo se permite una clave pública publishable o anon.');
}
fs.writeFileSync(new URL('../.env.production.local',import.meta.url),`VITE_SUPABASE_URL=${url}\nVITE_SUPABASE_ANON_KEY=${key}\n`);
console.log('Configuración pública preparada. Las claves privadas permanecen fuera del cliente.');
