import { useState } from 'react';
const initial = { name: '', age: 0, height: 0, weight: 0, waist: 0, days: 5, duration: 90, gym: '19:00', breakfast: '09:30', lunch: '13:30', snack: '17:00', dinner: '21:00', objective: 'Recomposición corporal', started: '' };
export default function Personal({ userId }: {
    userId: string;
}) {
    const key = 'actium.profile.' + userId;
    const [profile, setProfile] = useState(() => { try {
        return { ...initial, ...JSON.parse(localStorage.getItem(key) || '{}') };
    }
    catch {
        return initial;
    } }), [message, setMessage] = useState('');
    return <><section className="panel"><div className="sectionheading"><div><div className="eyebrow">TU PUNTO DE PARTIDA</div><h2>Un plan con tu contexto</h2></div><span className="pill">12 semanas</span></div><p className="muted">{profile.objective}. Estas referencias iniciales no se añaden como mediciones reales.</p><form onSubmit={e => { e.preventDefault(); try {
        localStorage.setItem(key, JSON.stringify(profile));
        setMessage('Preferencias guardadas en este navegador');
    }
    catch {
        setMessage('No se pudieron guardar las preferencias');
    } }}><div className="fields">{(['name', 'age', 'height', 'weight', 'waist', 'days', 'duration', 'objective', 'started'] as const).map(k => <label key={k}>{({ name: 'Nombre', age: 'Edad inicial', height: 'Estatura (cm)', weight: 'Peso inicial (kg)', waist: 'Cintura inicial (cm)', days: 'Días de gimnasio por semana', duration: 'Minutos por sesión', objective: 'Objetivo', started: 'Inicio del seguimiento' })[k]}<input required={k !== 'started'} step={['weight', 'waist', 'height'].includes(k) ? '0.1' : '1'} type={k === 'started' ? 'date' : ['name', 'objective'].includes(k) ? 'text' : 'number'} min="1" max={k === 'days' ? 7 : undefined} value={profile[k] || ''} onChange={e => setProfile({ ...profile, [k]: ['name', 'objective', 'started'].includes(k) ? e.target.value : Number(e.target.value) })}/></label>)}</div><h3>Tus horarios</h3><div className="fields">{(['breakfast', 'lunch', 'snack', 'gym', 'dinner'] as const).map(k => <label key={k}>{({ breakfast: 'Desayuno', lunch: 'Almuerzo', snack: 'Merienda', gym: 'Gimnasio', dinner: 'Cena' })[k]}<input required type="time" value={profile[k]} onChange={e => setProfile({ ...profile, [k]: e.target.value })}/></label>)}</div><button>Guardar perfil y horarios</button>{message && <p role="status">{message}</p>}</form><p className="hint">Estas preferencias se guardan por cuenta en este navegador. Exporta una copia desde Ajustes para conservarlas; los registros en la nube se sincronizan por Supabase.</p></section><div className="columns"><section className="panel"><h3>Tu distribución semanal</h3>{['Lunes · Torso A', 'Martes · Piernas A', 'Miércoles · Torso B', 'Jueves · Piernas B', 'Viernes · Hombros, brazos y abdominales', 'Sábado y domingo · Descanso o actividad ligera'].map(t => <div className="entry" key={t}>{t}</div>)}<p className="hint">Estructura acordada. En Entrenar puedes crear ejercicios y asignarlos a cada día.</p></section><section className="panel"><h3>Seguimiento de 12 semanas</h3>{['Peso · 3–4 mañanas por semana; revisar el promedio.', 'Cintura · una medición por semana.', 'Fotos · frente, perfil y espalda cada cuatro semanas.', 'Rendimiento · registrar pesos, repeticiones y series.', 'Nutrición · registrar las porciones realmente consumidas.'].map(t => <div className="entry" key={t}>{t}</div>)}{profile.started && <p className="notice">Semana {Math.min(12, Math.max(1, Math.floor((Date.now() - new Date(profile.started + 'T00:00:00').getTime()) / 604800000) + 1))} de 12 · inicio {profile.started}</p>}</section></div></>;
}
