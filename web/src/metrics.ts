import type { Row } from './local';
export function localDay(value: string) {
    const d = new Date(value);
    if (!Number.isFinite(d.getTime()))
        return '';
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
export const DEFAULT_GOALS = { calories: 2300, protein_g: 150, fat_g: 65, carbs_g: 279, water_ml: 2500, sleep_minutes: 480 };
export function goalsAt(rows: Row[], date: string) {
    const latest = rows.filter(x => x.effective_from <= date).sort((a, b) => b.effective_from.localeCompare(a.effective_from))[0];
    return latest ? Object.fromEntries(Object.entries(DEFAULT_GOALS).map(([k, v]) => [k, Number(latest[k] ?? v)])) as typeof DEFAULT_GOALS : { ...DEFAULT_GOALS };
}
export function portion(values: Record<string, string>, grams: number, basis: string) {
    if (!Number.isFinite(grams) || grams <= 0)
        throw Error('Gramos no válidos');
    return Object.fromEntries(['kcal', 'protein_g', 'carbs_g', 'fat_g'].map(k => { const n = Number(values[k]); if (!Number.isFinite(n) || n < 0)
        throw Error('Nutrientes no válidos'); return [k, n * (basis === '100g' ? grams / 100 : 1)]; }));
}
export function weeklyWeight(rows: Row[], date: string) {
    const lower = new Date(date + 'T12:00:00');
    lower.setDate(lower.getDate() - 6);
    const days = new Map<string, number[]>();
    for (const r of rows) {
        const day = localDay(r.measured_at);
        if (day >= localDay(lower.toISOString()) && day <= date && Number(r.weight_kg) > 0) {
            days.set(day, [...(days.get(day) || []), Number(r.weight_kg)]);
        }
    }
    const daily = [...days.values()].map(values => values.reduce((a, b) => a + b, 0) / values.length);
    return { days: daily.length, average: daily.length ? daily.reduce((a, b) => a + b, 0) / daily.length : null };
}
