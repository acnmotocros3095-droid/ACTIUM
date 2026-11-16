import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { db, isLocal } from './supabase';
import Progress, { localDay, goalsAt } from './Progress';
import RestTimer from './RestTimer';
import Personal from './Personal';
import { exportLocal, importLocal } from './local';
import { portion } from './metrics';
import { Activity, Apple, ChartNoAxesCombined, Droplets, Dumbbell, Moon, Camera, Bot, LogOut, Plus, Settings, Trash2, Upload, Menu } from 'lucide-react';
import type { User } from '@supabase/supabase-js';
type Page = 'Mi plan' | 'Asistente' | 'Inicio' | 'Comidas' | 'Entrenar' | 'Agua' | 'Sueño' | 'Medidas' | 'Multimedia' | 'Ajustes';
type Row = Record<string, any>;
const nav: [
    Page,
    any
][] = [['Inicio', Activity], ['Mi plan', Activity], ['Asistente', Bot], ['Comidas', Apple], ['Entrenar', Dumbbell], ['Agua', Droplets], ['Sueño', Moon], ['Medidas', ChartNoAxesCombined], ['Multimedia', Camera], ['Ajustes', Settings]];
const fmt = (n: number) => Number(n || 0).toLocaleString('es-BO', { maximumFractionDigits: 1 });
function dayISO() { const d = new Date(); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
export default function App() {
    const [user, setUser] = useState<User | null>(null), [ready, setReady] = useState(false), [page, setPage] = useState<Page>('Inicio'), [date, setDate] = useState(dayISO()), [rows, setRows] = useState<Record<string, Row[]>>({}), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [mobileNav, setMobileNav] = useState(false);
    const [email, setEmail] = useState(''), [password, setPassword] = useState(''), [signup, setSignup] = useState(false);
    const [goal, setGoal] = useState({ calories: 2300, protein_g: 150, fat_g: 65, carbs_g: 279, water_ml: 2500, sleep_minutes: 480 });
    const [food, setFood] = useState({ meal_type: 'breakfast', food_name: '', grams: '100', kcal: '', protein_g: '', carbs_g: '', fat_g: '' });
    const [exercise, setExercise] = useState({ name: '', muscle_group: 'Pecho' }), [setForm, setSetForm] = useState({ exercise_id: '', weight_kg: '', reps: '', rir: '2' });
    const [sleep, setSleep] = useState({ slept_at: '', woke_at: '', quality: '4' });
    const [measurement, setMeasurement] = useState({ weight_kg: '', waist_cm: '', chest_cm: '', arm_cm: '', thigh_cm: '' });
    const [file, setFile] = useState<File | null>(null), [mediaBucket, setMediaBucket] = useState('body-progress'), [viewType, setViewType] = useState('front'), [mealPhotoId, setMealPhotoId] = useState('');
    const [sessionId, setSessionId] = useState<string | null>(null);
    const [basis, setBasis] = useState('portion'), [consent, setConsent] = useState(false);
    const [restore, setRestore] = useState<File | null>(null), [restoreConfirmed, setRestoreConfirmed] = useState(false);
    const [routine, setRoutine] = useState({ weekday: '1', exercise_id: '', target_sets: '3', min_reps: '8', max_reps: '12', rest_seconds: '90' });
    async function backup() { try {
        const tables: Record<string, Row[]> = {};
        if (!isLocal) {
            for (const t of ['daily_goals', 'meals', 'meal_items', 'foods', 'exercises', 'workout_plans', 'plan_exercises', 'workout_sessions', 'workout_sets', 'water_entries', 'sleep_entries', 'body_measurements', 'media_assets', 'body_photos']) {
                let offset = 0;
                tables[t] = [];
                while (true) {
                    const { data, error } = await db.from(t).select('*').eq('user_id', user!.id).order('id').range(offset, offset + 499);
                    if (error)
                        throw error;
                    tables[t].push(...(data || []));
                    if ((data || []).length < 500)
                        break;
                    offset += 500;
                }
            }
        }
        const raw = isLocal ? JSON.parse(exportLocal()) : { app: 'ACTIUM', version: 2, mode: 'cloud-export', tables };
        raw.profile = JSON.parse(localStorage.getItem('actium.profile.' + user!.id) || 'null');
        const blob = new Blob([JSON.stringify(raw, null, 2)], { type: 'application/json' }), url = URL.createObjectURL(blob), a = document.createElement('a');
        a.href = url;
        a.download = 'ACTIUM-' + dayISO() + '.json';
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        setNotice('Copia descargada. Los archivos multimedia de Supabase se respaldan por separado.');
    }
    catch (e: any) {
        setError(e.message);
    } }
    async function restoreBackup() { if (!restore || !restoreConfirmed)
        return; await action(async () => { const text = await restore.text(); const parsed = JSON.parse(text); if (parsed.mode === 'cloud-export')
        throw Error('La exportación de nube es de consulta; no puede restaurarse como datos locales'); if (parsed.profile && (typeof parsed.profile !== 'object' || Array.isArray(parsed.profile) || Object.values(parsed.profile).some(v => typeof v !== 'string' && typeof v !== 'number')))
        throw Error('Perfil de copia no válido'); importLocal(text); if (parsed.profile)
        localStorage.setItem('actium.profile.' + user!.id, JSON.stringify(parsed.profile)); setRestore(null); setRestoreConfirmed(false); }); }
    function stamp() { return new Date(date + 'T' + new Date().toTimeString().slice(0, 8)).toISOString(); }
    async function saveRoutine(e: FormEvent) { e.preventDefault(); await action(async () => { let plan = data('workout_plans').find(p => p.active); if (!plan) {
        const result = await db.from('workout_plans').insert({ user_id: user!.id, name: 'Mi semana ACTIUM', active: true }).select('*').single();
        if (result.error)
            throw result.error;
        plan = result.data;
    } await insert('plan_exercises', { plan_id: plan!.id, exercise_id: routine.exercise_id, weekday: Number(routine.weekday), position: Math.max(-1, ...data('plan_exercises').filter(r => r.plan_id === plan!.id && r.weekday === Number(routine.weekday)).map(r => Number(r.position))) + 1, target_sets: Number(routine.target_sets), min_reps: Number(routine.min_reps), max_reps: Number(routine.max_reps), rest_seconds: Number(routine.rest_seconds) }); }); }
    const [coachQuestion, setCoachQuestion] = useState('¿Cómo va mi progreso en los últimos 14 días?'), [coachAnswer, setCoachAnswer] = useState(''), [coachLoading, setCoachLoading] = useState(false);
    async function askCoach(e: FormEvent) { e.preventDefault(); if (!consent || isLocal)
        return; setCoachLoading(true); setCoachAnswer(''); setError(''); try {
        const { data, error } = await db.functions.invoke('actium-coach', { body: { question: coachQuestion } });
        if (error)
            throw error;
        if (data?.error)
            throw Error(data.error);
        setCoachAnswer(data?.answer || 'Sin respuesta');
    }
    catch (e: any) {
        setError(e.message || String(e));
    }
    finally {
        setCoachLoading(false);
    } }
    async function refresh() {
        if (!user)
            return;
        try {
            const tables = ['workout_plans', 'plan_exercises', 'daily_goals', 'meals', 'meal_items', 'foods', 'exercises', 'workout_sessions', 'workout_sets', 'water_entries', 'sleep_entries', 'body_measurements', 'media_assets', 'body_photos'];
            const results = await Promise.all(tables.map(async (t) => { const { data, error } = await db.from(t).select('*').eq('user_id', user.id).order('id').range(0, 499); if (error)
                throw error; const complete = [...(data || [])]; if (!isLocal) {
                let offset = 500;
                while (complete.length === offset) {
                    const next = await db.from(t).select('*').eq('user_id', user.id).order('id').range(offset, offset + 499);
                    if (next.error)
                        throw next.error;
                    complete.push(...(next.data || []));
                    if ((next.data || []).length < 500)
                        break;
                    offset += 500;
                }
            } return [t, complete] as const; }));
            setRows(Object.fromEntries(results));
            setError('');
            const goals = results.find(r => r[0] === 'daily_goals')?.[1] || [];
            setGoal(goalsAt(goals, date));
            const active = results.find(r => r[0] === 'workout_sessions')?.[1].filter(r => !r.ended_at && localDay(r.started_at) === date).sort((a, b) => b.started_at.localeCompare(a.started_at))[0];
            setSessionId(active?.id || null);
        }
        catch (e: any) {
            setError(e.message || String(e));
        }
    }
    useEffect(() => { db.auth.getUser().then(({ data }) => { setUser(data.user); setReady(true); }); const { data: { subscription } } = db.auth.onAuthStateChange((_event, session) => setUser(session?.user || null)); return () => subscription.unsubscribe(); }, []);
    useEffect(() => { if (user)
        refresh();
    else
        setRows({}); }, [user, date]);
    const data = (t: string) => rows[t] || [];
    const dateRows = (t: string, col: string) => data(t).filter(x => x[col] && localDay(x[col]) === date);
    const meals = dateRows('meals', 'eaten_at'), mealIds = new Set(meals.map(x => x.id)), items = data('meal_items').filter(x => mealIds.has(x.meal_id));
    const total = (key: string) => items.reduce((a, x) => a + Number(x[key] || 0), 0);
    const water = dateRows('water_entries', 'consumed_at').reduce((a, x) => a + Number(x.amount_ml), 0);
    const workouts = dateRows('workout_sessions', 'started_at');
    const measurements = [...data('body_measurements')].filter(x => localDay(x.measured_at) <= date).sort((a, b) => b.measured_at.localeCompare(a.measured_at));
    const sleeps = [...data('sleep_entries')].filter(x => localDay(x.woke_at) <= date).sort((a, b) => b.woke_at.localeCompare(a.woke_at));
    const latestWeight = measurements.find(x => x.weight_kg != null);
    const latestWaist = measurements.find(x => x.waist_cm != null);
    async function action(fn: () => Promise<void>) { setBusy(true); setError(''); setNotice(''); try {
        await fn();
        await refresh();
        setNotice('Registro guardado correctamente');
    }
    catch (e: any) {
        setError(e.message || String(e));
    }
    finally {
        setBusy(false);
    } }
    async function auth(e: FormEvent) { e.preventDefault(); setBusy(true); setError(''); const r = signup ? await db.auth.signUp({ email, password }) : await db.auth.signInWithPassword({ email, password }); if (r.error)
        setError(r.error.message);
    else if (signup)
        setNotice('Cuenta creada. Confirma tu correo si Supabase lo solicita.'); setBusy(false); }
    async function insert(table: string, obj: Row) { const { error } = await db.from(table).insert({ ...obj, user_id: user!.id }); if (error)
        throw error; }
    async function remove(table: string, id: string) { await action(async () => { const { error } = await db.from(table).delete().eq('id', id).eq('user_id', user!.id); if (error)
        throw error; }); }
    async function addMeal(e: FormEvent) {
        e.preventDefault();
        await action(async () => {
            const grams = Number(food.grams);
            if (!food.food_name.trim() || grams <= 0 || !Number.isFinite(grams))
                throw Error('Introduce nombre y gramos válidos');
            const nutrients = portion(food, grams, basis);
            if (!isLocal) {
                const { error } = await db.rpc('actium_add_meal', { p_type: food.meal_type, p_name: food.food_name, p_grams: grams, p_kcal: nutrients.kcal, p_protein: nutrients.protein_g, p_carbs: nutrients.carbs_g, p_fat: nutrients.fat_g, p_eaten_at: stamp() });
                if (error)
                    throw error;
                setFood({ ...food, food_name: '', kcal: '', protein_g: '', carbs_g: '', fat_g: '' });
                return;
            }
            const { data: m, error: me } = await db.from('meals').insert({ user_id: user!.id, meal_type: food.meal_type, eaten_at: new Date(date + 'T' + new Date().toTimeString().slice(0, 8)).toISOString() }).select('id').single();
            if (me)
                throw me;
            const { error: ie } = await db.from('meal_items').insert({ user_id: user!.id, meal_id: m.id, food_name: food.food_name, grams, ...nutrients });
            if (ie) {
                await db.from('meals').delete().eq('id', m.id);
                throw ie;
            }
            setFood({ ...food, food_name: '', kcal: '', protein_g: '', carbs_g: '', fat_g: '' });
        });
    }
    async function addSet(e: FormEvent) {
        e.preventDefault();
        await action(async () => {
            if (!setForm.exercise_id)
                throw Error('Crea y selecciona un ejercicio');
            if (Number(setForm.reps) < 1 || !Number.isInteger(Number(setForm.reps)) || Number(setForm.rir) > 10)
                throw Error('Usa repeticiones enteras mayores a cero y RIR entre 0 y 10');
            let sid = sessionId;
            if (!sid) {
                const { data: s, error } = await db.from('workout_sessions').insert({ user_id: user!.id, started_at: stamp() }).select('id').single();
                if (error)
                    throw error;
                sid = s.id;
                setSessionId(sid);
            }
            const { count, error: ce } = await db.from('workout_sets').select('*', { count: 'exact', head: true }).eq('session_id', sid).eq('exercise_id', setForm.exercise_id);
            if (ce)
                throw ce;
            await insert('workout_sets', { session_id: sid, exercise_id: setForm.exercise_id, set_number: (count || 0) + 1, weight_kg: Number(setForm.weight_kg || 0), reps: Number(setForm.reps || 0), rir: Number(setForm.rir || 0) });
        });
    }
    async function upload(e: FormEvent) {
        e.preventDefault();
        if (!file)
            return;
        await action(async () => {
            const allowed = mediaBucket === 'exercise-media' ? ['image/jpeg', 'image/png', 'image/webp', 'video/mp4'] : ['image/jpeg', 'image/png', 'image/webp'];
            if (!allowed.includes(file.type))
                throw Error('Formato no permitido para este tipo de archivo');
            if (file.size > (mediaBucket === 'exercise-media' ? 50 : 10) * 1024 * 1024)
                throw Error('Archivo demasiado grande');
            const path = user!.id + '/' + crypto.randomUUID() + '.' + (file.name.split('.').pop() || 'bin').toLowerCase();
            const { error: up } = await db.storage.from(mediaBucket).upload(path, file, { contentType: file.type });
            if (up)
                throw up;
            const { data: asset, error: ae } = await db.from('media_assets').insert({ user_id: user!.id, bucket: mediaBucket, object_path: path, mime_type: file.type }).select('id').single();
            if (ae) {
                await db.storage.from(mediaBucket).remove([path]);
                throw ae;
            }
            try {
                if (mediaBucket === 'body-progress') {
                    const { error: pe } = await db.from('body_photos').insert({ user_id: user!.id, media_id: asset.id, view_type: viewType });
                    if (pe)
                        throw pe;
                }
                if (mediaBucket === 'meal-photos' && mealPhotoId) {
                    const { error: pe } = await db.from('meal_media').insert({ user_id: user!.id, media_id: asset.id, meal_id: mealPhotoId });
                    if (pe)
                        throw pe;
                }
            }
            catch (e) {
                await db.from('media_assets').delete().eq('id', asset.id);
                await db.storage.from(mediaBucket).remove([path]);
                throw e;
            }
            setFile(null);
        });
    }
    async function openMedia(asset: Row) { const { data, error } = await db.storage.from(asset.bucket).createSignedUrl(asset.object_path, 60); if (error)
        setError(error.message);
    else
        window.open(data.signedUrl, '_blank', 'noopener,noreferrer'); }
    const goals = [['Calorías', total('kcal'), goal.calories, 'kcal'], ['Proteína', total('protein_g'), goal.protein_g, 'g'], ['Carbohidratos', total('carbs_g'), goal.carbs_g, 'g'], ['Grasas', total('fat_g'), goal.fat_g, 'g']];
    const cards = (<div className="metricgrid">{goals.map(([label, value, target, unit]) => <div className="metric" key={String(label)}><small>{label}</small><strong>{fmt(Number(value))}<span> / {fmt(Number(target))} {unit}</span></strong><div className="bar"><i style={{ width: (Number(target) > 0 ? Math.min(100, Number(value) / Number(target) * 100) : 0) + '%' }}/></div><div className="remaining">{Number(value) <= Number(target) ? 'Restan ' + fmt(Number(target) - Number(value)) : 'Sobre la meta: ' + fmt(Number(value) - Number(target))} {unit}</div></div>)}</div>);
    if (!ready)
        return <main className="auth">Cargando...</main>;
    if (!user)
        return <main className="auth"><form className="authcard" onSubmit={auth}><div className="brand"><Activity /> ACTIUM</div><p>BUILD. TRACK. EVOLVE.</p><h2>{signup ? 'Crear cuenta' : 'Iniciar sesión'}</h2><label>Correo electrónico<input type="email" required value={email} onChange={e => setEmail(e.target.value)}/></label><label>Contraseña<input type="password" minLength={6} required value={password} onChange={e => setPassword(e.target.value)}/></label>{error && <div className="error">{error}</div>}{notice && <div className="notice">{notice}</div>}<button disabled={busy}>{signup ? 'Registrarme' : 'Entrar'}</button><button type="button" className="link" onClick={() => setSignup(!signup)}>{signup ? 'Ya tengo cuenta' : 'Crear una cuenta'}</button></form></main>;
    return <div className="shell"><aside className={'sidebar ' + (mobileNav ? 'show' : '')}><div className="brand"><Activity /> ACTIUM</div><div className="subbrand">DARK PERFORMANCE</div><nav>{nav.map(([name, Icon]) => <button key={name} className={page === name ? 'active' : ''} onClick={() => { setPage(name); setMobileNav(false); }}><Icon size={19}/>{name}</button>)}</nav><div className="sidebarbottom"><div className="modebadge"><i />{isLocal ? 'Datos en este dispositivo' : 'Cuenta sincronizada'}</div><small>Tu progreso. Tus datos.<br />Tu evolución.</small>{!isLocal && <button className="logout" onClick={() => { db.auth.signOut(); setRows({}); setCoachAnswer(''); setConsent(false); }}><LogOut size={18}/> Salir</button>}</div></aside>
 <main className="content"><header><div className="heading"><button aria-label="Abrir menú" aria-expanded={mobileNav} className="menubtn" onClick={() => setMobileNav(!mobileNav)}><Menu /></button><div><div className="eyebrow">ACTIUM / {page.toUpperCase()}</div><h1>{page}</h1></div></div><input aria-label="Fecha seleccionada" type="date" max={dayISO()} required value={date} onChange={e => e.target.value && setDate(e.target.value)}/></header>
 {isLocal && <div className="localbanner"><span><b>Modo local</b> · Puedes empezar sin cuenta. Los datos se guardan en este navegador.</span><button className="link" onClick={() => setPage('Ajustes')}>Copia de seguridad →</button></div>}{error && <div className="error" role="alert">{error}</div>}{notice && <div className="notice" role="status">{notice}</div>}
 {page === 'Mi plan' && <Personal userId={user.id}/>}
 {page === 'Asistente' && <section className="panel narrow"><h2><Bot size={21}/> Asistente ACTIUM</h2><p className="muted">Analiza tus registros de los últimos 14 días. Solo envía datos necesarios a la función segura de Supabase y al proveedor de IA. No comparte fotografías.</p><form onSubmit={askCoach}><label>Tu pregunta<input required maxLength={700} value={coachQuestion} onChange={e => setCoachQuestion(e.target.value)}/></label><label className="checkline"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)}/>Autorizo enviar el resumen de mis registros al proveedor de IA para esta consulta.</label><button disabled={coachLoading || isLocal || !consent}>{coachLoading ? 'Analizando...' : 'Preguntar al asistente'}</button></form>{coachAnswer && <div className="coachanswer">{coachAnswer}</div>}<p className="hint">Requiere desplegar actium-coach y configurar OPENAI_API_KEY en Supabase. La API de OpenAI se factura por separado de ChatGPT.</p></section>}
 {page === 'Inicio' && <><div className="hero"><div><div className="eyebrow">TU CENTRO DE CONTROL</div><h2>Tu próxima versión<br />empieza hoy.</h2><p>Nutrición, fuerza y recuperación. Todo en un solo lugar.</p><button className="small" onClick={() => setPage('Comidas')}><Plus size={16}/> Registrar comida</button></div><div className="heromark"><Activity size={85}/><span>BUILD / TRACK / EVOLVE</span></div></div><div className="sectionheading"><h2>Balance del día</h2><span className="pill">Objetivos editables</span></div>{cards}<div className="metricgrid"><div className="metric"><small>Agua de hoy</small><strong>{fmt(water)} <span>/ {goal.water_ml} ml</span></strong><button className="small" disabled={busy} onClick={() => action(() => insert('water_entries', { amount_ml: 250, consumed_at: stamp() }))}>+ 250 ml</button></div><div className="metric"><small>Entrenamientos del día</small><strong>{workouts.length}</strong><button className="small" onClick={() => setPage('Entrenar')}>Ver sesiones</button></div><div className="metric"><small>Último peso</small><strong>{latestWeight ? fmt(latestWeight.weight_kg) + ' kg' : 'Sin datos'}</strong></div><div className="metric"><small>Última cintura</small><strong>{latestWaist ? fmt(latestWaist.waist_cm) + ' cm' : 'Sin datos'}</strong></div></div><Progress rows={rows} date={date}/><section className="panel"><div className="sectionheading"><h3>Recuperación y constancia</h3><button className="small" onClick={() => setPage('Mi plan')}>Ver mi plan →</button></div><p className="muted">{sleeps[0] ? 'Último descanso: ' + fmt((new Date(sleeps[0].woke_at).getTime() - new Date(sleeps[0].slept_at).getTime()) / 3600000) + ' horas' : 'Sin descanso registrado todavía'} · Meta de sueño: {fmt(goal.sleep_minutes / 60)} h</p><p className="hint">Configura tus referencias iniciales en Mi plan. El resumen muestra únicamente mediciones que registres.</p></section></>}
 {page === 'Comidas' && <><h2>Diario nutricional</h2>{cards}<div className="columns"><section className="panel"><h3><Plus size={18}/> Nueva comida</h3><form onSubmit={addMeal}><label>Momento<select value={food.meal_type} onChange={e => setFood({ ...food, meal_type: e.target.value })}>{[['breakfast', 'Desayuno'], ['lunch', 'Almuerzo'], ['snack', 'Merienda'], ['dinner', 'Cena'], ['other', 'Otro']].map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label><label>Valores nutricionales<select value={basis} onChange={e => setBasis(e.target.value)}><option value="portion">Para la porción completa</option><option value="100g">Por cada 100 g (etiqueta)</option></select></label><label>Nombre del alimento<input required value={food.food_name} onChange={e => setFood({ ...food, food_name: e.target.value })} placeholder="Ej. pollo con arroz"/></label><div className="fields">{(['grams', 'kcal', 'protein_g', 'carbs_g', 'fat_g'] as const).map(k => <label key={k}>{({ grams: 'Gramos', kcal: 'Calorías', protein_g: 'Proteínas (g)', carbs_g: 'Carbohidratos (g)', fat_g: 'Grasas (g)' })[k]}<input type="number" min="0" step="0.1" required value={food[k]} onChange={e => setFood({ ...food, [k]: e.target.value })}/></label>)}</div><p className="hint">{basis === '100g' ? 'La app multiplica los valores de la etiqueta por los gramos consumidos / 100.' : 'Introduce los nutrientes de toda la porción consumida.'}</p><button disabled={busy}>Guardar comida</button></form></section><section className="panel"><h3>Comidas del día</h3>{meals.length === 0 ? <p className="muted">Todavía no hay comidas.</p> : meals.map(m => <div className="entry" key={m.id}><div><b>{({ breakfast: 'Desayuno', lunch: 'Almuerzo', snack: 'Merienda', dinner: 'Cena', other: 'Otro' } as any)[m.meal_type]}</b>{data('meal_items').filter(i => i.meal_id === m.id).map(i => <div className="muted" key={i.id}>{i.food_name} · {fmt(i.grams)} g · {fmt(i.kcal)} kcal</div>)}</div><button disabled={busy} className="iconbutton" aria-label="Eliminar comida" onClick={() => remove('meals', m.id)}><Trash2 size={17}/></button></div>)}</section></div></>}
 {page === 'Agua' && <div className="columns"><section className="panel"><h2>Hidratación</h2><div className="big">{fmt(water)} <span>ml</span></div><div className="bar"><i style={{ width: Math.min(100, water / Math.max(1, goal.water_ml) * 100) + '%' }}/></div><p className="muted">Meta editable: {goal.water_ml} ml</p><div className="quick">{[150, 250, 500, 750].map(v => <button key={v} disabled={busy} onClick={() => action(() => insert('water_entries', { amount_ml: v, consumed_at: stamp() }))}>+{v} ml</button>)}</div></section><section className="panel"><h3>Registros</h3>{dateRows('water_entries', 'consumed_at').map(x => <div className="entry" key={x.id}><span>{x.amount_ml} ml · {new Date(x.consumed_at).toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit' })}</span><button className="iconbutton" aria-label="Eliminar registro" disabled={busy} onClick={() => remove('water_entries', x.id)}><Trash2 size={16}/></button></div>)}</section></div>}
 {page === 'Sueño' && <div className="columns"><section className="panel"><h2>Registrar descanso</h2><form onSubmit={e => { e.preventDefault(); action(async () => { if (new Date(sleep.woke_at).getTime() - new Date(sleep.slept_at).getTime() > 86400000)
        throw Error('El descanso no puede superar 24 horas'); if (new Date(sleep.woke_at) <= new Date(sleep.slept_at))
        throw Error('El despertar debe ser posterior a dormir'); await insert('sleep_entries', { slept_at: new Date(sleep.slept_at).toISOString(), woke_at: new Date(sleep.woke_at).toISOString(), quality: Number(sleep.quality) }); }); }}><label>Me dormí<input type="datetime-local" required value={sleep.slept_at} onChange={e => setSleep({ ...sleep, slept_at: e.target.value })}/></label><label>Desperté<input type="datetime-local" required value={sleep.woke_at} onChange={e => setSleep({ ...sleep, woke_at: e.target.value })}/></label><label>Calidad (1 a 5)<select value={sleep.quality} onChange={e => setSleep({ ...sleep, quality: e.target.value })}>{[1, 2, 3, 4, 5].map(n => <option key={n}>{n}</option>)}</select></label><button disabled={busy}>Guardar sueño</button></form></section><section className="panel"><h3>Últimos descansos</h3>{sleeps.slice(0, 20).map(s => <div className="entry" key={s.id}><div><b>{fmt((new Date(s.woke_at).getTime() - new Date(s.slept_at).getTime()) / 3600000)} h</b><div className="muted">{new Date(s.woke_at).toLocaleDateString('es-BO')} · Calidad {s.quality}/5</div></div><button className="iconbutton" aria-label="Eliminar registro" disabled={busy} onClick={() => remove('sleep_entries', s.id)}><Trash2 size={16}/></button></div>)}</section></div>}
 {page === 'Medidas' && <><Progress rows={rows} date={date}/><div className="columns"><section className="panel"><h2>Medidas corporales</h2><form onSubmit={e => { e.preventDefault(); action(() => insert('body_measurements', { measured_at: stamp(), ...Object.fromEntries(Object.entries(measurement).filter(([, v]) => v !== '').map(([k, v]) => [k, Number(v)])) })); }}><div className="fields">{(Object.keys(measurement) as (keyof typeof measurement)[]).map(k => <label key={k}>{({ weight_kg: 'Peso (kg)', waist_cm: 'Cintura (cm)', chest_cm: 'Pecho (cm)', arm_cm: 'Brazo (cm)', thigh_cm: 'Muslo (cm)' })[k]}<input type="number" min="1" step="0.1" value={measurement[k]} onChange={e => setMeasurement({ ...measurement, [k]: e.target.value })}/></label>)}</div><button disabled={busy || Object.values(measurement).every(x => !x)}>Guardar medición</button></form></section><section className="panel"><h3>Historial</h3>{measurements.map(m => <div className="entry" key={m.id}><div><b>{new Date(m.measured_at).toLocaleDateString('es-BO')}</b><div className="muted">{m.weight_kg && `${m.weight_kg} kg · `}{m.waist_cm && `Cintura ${m.waist_cm} cm`}</div></div><button className="iconbutton" aria-label="Eliminar registro" disabled={busy} onClick={() => remove('body_measurements', m.id)}><Trash2 size={16}/></button></div>)}</section></div></>}
 {page === 'Entrenar' && <><RestTimer /><div className="columns"><section className="panel"><h2>Biblioteca de ejercicios</h2><form onSubmit={e => { e.preventDefault(); action(async () => { await insert('exercises', exercise); setExercise({ ...exercise, name: '' }); }); }}><label>Nombre<input required value={exercise.name} onChange={e => setExercise({ ...exercise, name: e.target.value })}/></label><label>Grupo muscular<select value={exercise.muscle_group} onChange={e => setExercise({ ...exercise, muscle_group: e.target.value })}>{['Pecho', 'Espalda', 'Hombros', 'Bíceps', 'Tríceps', 'Cuádriceps', 'Femoral', 'Glúteos', 'Pantorrillas', 'Abdomen', 'Cardio'].map(x => <option key={x}>{x}</option>)}</select></label><button disabled={busy}>Crear ejercicio</button></form><h3>Ejercicios guardados</h3>{data('exercises').map(x => <div className="entry" key={x.id}><span>{x.name} <small>· {x.muscle_group}</small></span><button className="iconbutton" aria-label="Eliminar registro" disabled={busy} onClick={() => remove('exercises', x.id)}><Trash2 size={16}/></button></div>)}</section><section className="panel"><h2>Registrar entrenamiento</h2><p className="muted">{sessionId ? 'Sesión activa' : 'Se creará una sesión al registrar la primera serie'}</p><form onSubmit={addSet}><label>Ejercicio<select required value={setForm.exercise_id} onChange={e => setSetForm({ ...setForm, exercise_id: e.target.value })}><option value="">Selecciona...</option>{data('exercises').map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select></label><div className="fields">{(['weight_kg', 'reps', 'rir'] as const).map(k => <label key={k}>{({ weight_kg: 'Peso (kg)', reps: 'Repeticiones', rir: 'RIR' })[k]}<input type="number" min={k === 'reps' ? 1 : 0} max={k === 'rir' ? 10 : undefined} step={k === 'weight_kg' ? '0.5' : '1'} required value={setForm[k]} onChange={e => setSetForm({ ...setForm, [k]: e.target.value })}/></label>)}</div><button disabled={busy}>Guardar serie</button></form>{sessionId && <button className="secondary" onClick={() => action(async () => { const { error } = await db.from('workout_sessions').update({ ended_at: date === dayISO() ? new Date().toISOString() : new Date(date + 'T23:59:59').toISOString() }).eq('id', sessionId); if (error)
        throw error; setSessionId(null); })}>Finalizar sesión</button>}<h3>Sesiones recientes</h3>{[...data('workout_sessions')].sort((a, b) => b.started_at.localeCompare(a.started_at)).slice(0, 15).map(s => <div className="entry" key={s.id}><div><b>{new Date(s.started_at).toLocaleString('es-BO')}</b><div className="muted">{data('workout_sets').filter(x => x.session_id === s.id).length} series · {s.ended_at ? 'Finalizada' : 'En progreso'}</div></div><button className="iconbutton" aria-label="Eliminar registro" disabled={busy} onClick={() => remove('workout_sessions', s.id)}><Trash2 size={16}/></button></div>)}</section></div><div className="columns"><section className="panel"><h2>Planificador semanal</h2><form onSubmit={saveRoutine}><div className="fields"><label>Día<select value={routine.weekday} onChange={e => setRoutine({ ...routine, weekday: e.target.value })}>{['Lunes · Torso A', 'Martes · Piernas A', 'Miércoles · Torso B', 'Jueves · Piernas B', 'Viernes · Hombros y brazos', 'Sábado', 'Domingo'].map((d, i) => <option value={i + 1} key={d}>{d}</option>)}</select></label><label>Ejercicio<select required value={routine.exercise_id} onChange={e => setRoutine({ ...routine, exercise_id: e.target.value })}><option value="">Selecciona…</option>{data('exercises').map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select></label>{(['target_sets', 'min_reps', 'max_reps', 'rest_seconds'] as const).map(k => <label key={k}>{({ target_sets: 'Series', min_reps: 'Reps mínimas', max_reps: 'Reps máximas', rest_seconds: 'Descanso (seg)' })[k]}<input required type="number" min={k === 'rest_seconds' ? 0 : k === 'max_reps' ? Number(routine.min_reps) : 1} max={k === 'target_sets' ? 20 : k === 'rest_seconds' ? 1800 : 1000} value={routine[k]} onChange={e => setRoutine({ ...routine, [k]: e.target.value })}/></label>)}</div><button disabled={busy}>Añadir a mi rutina</button></form>{data('plan_exercises').filter(r => r.weekday === Number(routine.weekday)).sort((a, b) => a.position - b.position).map(r => <div className="entry" key={r.id}><div><b>{data('exercises').find(e => e.id === r.exercise_id)?.name}</b><div className="muted">{r.target_sets} × {r.min_reps}–{r.max_reps} reps · {r.rest_seconds} s de descanso</div></div><button className="iconbutton" aria-label="Quitar de rutina" onClick={() => remove('plan_exercises', r.id)}><Trash2 size={16}/></button></div>)}</section><section className="panel"><h2>Volumen y marcas</h2><p className="hint">Volumen = kg × repeticiones. Las marcas comparan el peso registrado por ejercicio.</p>{data('exercises').map(ex => { const sets = data('workout_sets').filter(r => r.exercise_id === ex.id); return sets.length ? <div className="entry" key={ex.id}><div><b>{ex.name}</b><div className="muted">{sets.length} series · {fmt(sets.reduce((n, r) => n + Number(r.weight_kg) * Number(r.reps), 0))} kg de volumen total</div></div><span className="pill">Máx. {fmt(Math.max(...sets.map(r => Number(r.weight_kg))))} kg</span></div> : null; })}<h3>Series de la sesión activa</h3>{data('workout_sets').filter(r => r.session_id === sessionId).map(r => <div className="entry" key={r.id}><span>{data('exercises').find(e => e.id === r.exercise_id)?.name} · {r.weight_kg} kg × {r.reps} · RIR {r.rir}</span><button className="iconbutton" aria-label="Eliminar serie" onClick={() => remove('workout_sets', r.id)}><Trash2 size={16}/></button></div>)}</section></div></>}
 {page === 'Multimedia' && <div className="columns"><section className="panel"><h2>Subir fotografía o video</h2>{isLocal && <p className="notice">Conecta Supabase para guardar fotografías y videos privados. El modo local guarda registros de texto y números.</p>}<form onSubmit={upload}><label>Categoría<select value={mediaBucket} onChange={e => setMediaBucket(e.target.value)}><option value="body-progress">Progreso corporal</option><option value="meal-photos">Fotografía de comida</option><option value="exercise-media">Ejercicio (foto/video)</option></select></label>{mediaBucket === 'body-progress' && <label>Vista<select value={viewType} onChange={e => setViewType(e.target.value)}>{[['front', 'Frente'], ['back', 'Espalda'], ['left', 'Perfil izquierdo'], ['right', 'Perfil derecho']].map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></label>}{mediaBucket === 'meal-photos' && <label>Asociar a comida (opcional)<select value={mealPhotoId} onChange={e => setMealPhotoId(e.target.value)}><option value="">Sin asociación</option>{meals.map(m => <option key={m.id} value={m.id}>{m.meal_type} · {new Date(m.eaten_at).toLocaleTimeString('es-BO')}</option>)}</select></label>}<label>Archivo<input type="file" required accept={mediaBucket === 'exercise-media' ? 'image/jpeg,image/png,image/webp,video/mp4' : 'image/jpeg,image/png,image/webp'} onChange={e => setFile(e.target.files?.[0] || null)}/></label><p className="hint">Fotos hasta 10 MB; videos MP4 hasta 50 MB. Los archivos son privados.</p><button disabled={busy || !file || isLocal}><Upload size={17}/> Subir archivo</button></form></section><section className="panel"><h3>Archivos privados</h3>{data('media_assets').map(a => <div className="entry" key={a.id}><div><b>{a.bucket}</b><div className="muted">{new Date(a.created_at).toLocaleString('es-BO')}</div></div><button className="small" onClick={() => openMedia(a)}>Abrir</button></div>)}</section></div>}
 {page === 'Ajustes' && <><section className="panel narrow"><h2>Metas personales</h2><p className="muted">Los objetivos se guardan con fecha de vigencia para conservar tu historial.</p><form onSubmit={e => { e.preventDefault(); action(async () => { const { error } = await db.from('daily_goals').upsert({ user_id: user!.id, effective_from: date, ...goal }, { onConflict: 'user_id,effective_from' }); if (error)
        throw error; }); }}><div className="fields">{(Object.keys(goal) as (keyof typeof goal)[]).map(k => <label key={k}>{({ calories: 'Calorías', protein_g: 'Proteína (g)', fat_g: 'Grasas (g)', carbs_g: 'Carbohidratos (g)', water_ml: 'Agua (ml)', sleep_minutes: 'Sueño (min)' })[k]}<input type="number" min={k === 'calories' ? 500 : 0} max={k === 'sleep_minutes' ? 1440 : k === 'calories' || k === 'water_ml' ? 10000 : 1000} required value={goal[k]} onChange={e => setGoal({ ...goal, [k]: Number(e.target.value) })}/></label>)}</div><button disabled={busy}>Guardar metas</button></form><p className="muted">Cuenta: {user.email}</p></section><section className="panel narrow backup"><h2>Tus datos, bajo tu control</h2><p className="muted">{isLocal ? 'Los registros locales no se sincronizan con Android. Descarga copias periódicas: borrar los datos del navegador elimina estos registros.' : 'Tus registros se guardan en tu cuenta Supabase. La exportación incluye los registros y rutas multimedia, pero no los archivos.'}</p><button disabled={busy} onClick={backup}>Descargar copia JSON</button>{isLocal && <><h3>Restaurar copia local</h3><p className="hint">Sustituye todos los registros locales. Descarga primero tu copia actual.</p><input aria-label="Copia de seguridad JSON" type="file" accept=".json,application/json" onChange={e => setRestore(e.target.files?.[0] || null)}/><label className="checkline"><input type="checkbox" checked={restoreConfirmed} onChange={e => setRestoreConfirmed(e.target.checked)}/>Entiendo que se sustituirán mis registros locales.</label><button disabled={busy || !restore || !restoreConfirmed} onClick={restoreBackup}>Restaurar copia</button><p className="hint">Para sincronizar con Android, configura las variables públicas de Supabase siguiendo INICIO_RAPIDO.md. Los datos locales no se transfieren automáticamente a la nube.</p></>}</section></>}
 <footer>ACTIUM · Registro personal · Los valores nutricionales deben verificarse; no son diagnósticos médicos.</footer></main></div>;
}
