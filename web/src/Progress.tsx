import type { Row } from './local';
import { localDay, weeklyWeight } from './metrics';
export { localDay, goalsAt } from './metrics';
export default function Progress({ rows, date }: {
    rows: Record<string, Row[]>;
    date: string;
}) {
    const week = weeklyWeight(rows.body_measurements || [], date);
    const measures = (rows.body_measurements || []).filter(r => localDay(r.measured_at) <= date && Number(r.weight_kg) > 0).sort((a, b) => a.measured_at.localeCompare(b.measured_at)).slice(-20);
    const weights = measures.map(r => Number(r.weight_kg));
    const low = Math.min(...weights) - .5, high = Math.max(...weights) + .5;
    const points = weights.map((w, i) => `${30 + i * 540 / Math.max(weights.length - 1, 1)},${145 - (w - low) / (high - low) * 110}`).join(' ');
    const days = Array.from({ length: 7 }, (_, i) => { const d = new Date(date + 'T12:00:00'); d.setDate(d.getDate() - 6 + i); const key = localDay(d.toISOString()); const ids = new Set((rows.meals || []).filter(r => localDay(r.eaten_at) === key).map(r => r.id)); const items = (rows.meal_items || []).filter(r => ids.has(r.meal_id)); return { key, label: d.toLocaleDateString('es', { weekday: 'short' }), kcal: items.reduce((s, r) => s + Number(r.kcal || 0), 0), count: items.length }; });
    const max = Math.max(1, ...days.map(d => d.kcal));
    return <div className="columns"><section className="panel"><div className="sectionheading"><h3>Evolución del peso</h3><span className="pill">Últimas 20 mediciones</span></div>{weights.length > 1 ? <><div className="trendvalue">{weights.at(-1)} <small>kg</small><span>{(weights.at(-1)! - weights[0]).toFixed(1)} kg desde el primer punto</span></div><svg className="weightchart" viewBox="0 0 600 180" role="img" aria-label={`Peso de ${weights[0]} a ${weights.at(-1)} kilogramos`}><line x1="30" y1="145" x2="570" y2="145" stroke="#343b35"/><polyline points={points} fill="none" stroke="#c7ff45" strokeWidth="3" strokeLinejoin="round"/>{weights.map((w, i) => <circle key={i} cx={30 + i * 540 / Math.max(weights.length - 1, 1)} cy={145 - (w - low) / (high - low) * 110} r="4" fill="#c7ff45"><title>{localDay(measures[i].measured_at)}: {w} kg</title></circle>)}</svg><p className="hint">Promedio de 7 días: {week.average === null ? 'sin registros' : week.average.toFixed(1) + ' kg'} · {week.days} días con peso registrado. Cada día tiene el mismo peso en el promedio.</p><div className="chartlabels"><span>{localDay(measures[0].measured_at)}</span><span>{localDay(measures.at(-1)!.measured_at)}</span></div></> : <div className="empty">Tu evolución, sin suposiciones.<p>Registra al menos dos pesos para ver tu gráfica.</p></div>}</section><section className="panel"><div className="sectionheading"><h3>Tu semana en registros</h3><span className="pill">Calorías registradas</span></div><div className="weekchart">{days.map(d => <div key={d.key}><span>{d.count ? Math.round(d.kcal) : '—'}</span><div className="track"><i style={{ height: d.count ? Math.max(3, d.kcal / max * 100) + '%' : '0%' }}/></div><small>{d.label}</small></div>)}</div><p className="hint">— indica que no hay alimentos registrados. No equivale a cero consumo.</p></section></div>;
}
