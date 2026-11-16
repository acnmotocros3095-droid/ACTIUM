// Local-only data store. Cloud mode always uses the real Supabase client.
export type Row = Record<string, any>;
export const TABLES = ['daily_goals', 'meals', 'meal_items', 'foods', 'exercises', 'workout_plans', 'plan_exercises', 'workout_sessions', 'workout_sets', 'water_entries', 'sleep_entries', 'body_measurements', 'media_assets', 'body_photos'] as const;
const KEY = 'actium.local.v2';
export const LOCAL_USER = '00000000-0000-4000-8000-000000000001';
export function validateRow(table: string, r: Row) {
    if (r.user_id !== LOCAL_USER)
        throw Error('Propietario local no válido');
    const number = (k: string, min: number, max = 1e7, required = true, integer = false) => {
        if (!required && r[k] == null)
            return;
        if (typeof r[k] !== 'number' || !Number.isFinite(r[k]) || r[k] < min || r[k] > max || (integer && !Number.isInteger(r[k])))
            throw Error('Valor no válido: ' + k);
    };
    const date = (k: string) => { if (typeof r[k] !== 'string' || !Number.isFinite(Date.parse(r[k])))
        throw Error('Fecha no válida: ' + k); };
    if (table === 'water_entries') {
        number('amount_ml', 1, 5000, true, true);
        date('consumed_at');
    }
    if (table === 'daily_goals') {
        number('calories', 500, 10000, true, true);
        for (const k of ['protein_g', 'carbs_g', 'fat_g'])
            number(k, 0);
        number('water_ml', 0, 10000, true, true);
        number('sleep_minutes', 0, 1440, true, true);
        date('effective_from');
    }
    if (table === 'meals') {
        date('eaten_at');
        if (!['breakfast', 'lunch', 'snack', 'dinner', 'other'].includes(r.meal_type))
            throw Error('Momento no válido');
    }
    if (table === 'meal_items') {
        if (typeof r.food_name !== 'string' || !r.food_name.trim())
            throw Error('Alimento requerido');
        number('grams', .01);
        for (const k of ['kcal', 'protein_g', 'carbs_g', 'fat_g'])
            number(k, 0);
    }
    if (table === 'exercises' && (!r.name?.trim() || !r.muscle_group?.trim()))
        throw Error('Nombre y grupo muscular requeridos');
    if (table === 'workout_sessions') {
        date('started_at');
        if (r.ended_at) {
            date('ended_at');
            if (Date.parse(r.ended_at) < Date.parse(r.started_at))
                throw Error('Fin anterior al inicio');
        }
    }
    if (table === 'workout_sets') {
        number('set_number', 1, 10000, true, true);
        number('weight_kg', 0);
        number('reps', 1, 10000, true, true);
        number('rir', 0, 10);
    }
    if (table === 'plan_exercises') {
        number('weekday', 1, 7, true, true);
        number('position', 0, 10000, true, true);
        number('target_sets', 1, 20, true, true);
        number('min_reps', 1, 1000, true, true);
        number('max_reps', r.min_reps, 1000, true, true);
        number('rest_seconds', 0, 1800, true, true);
    }
    if (table === 'sleep_entries') {
        date('slept_at');
        date('woke_at');
        const duration = Date.parse(r.woke_at) - Date.parse(r.slept_at);
        if (duration <= 0 || duration > 86400000)
            throw Error('El descanso debe durar entre 0 y 24 horas');
        number('quality', 1, 5, true, true);
    }
    if (table === 'body_measurements') {
        date('measured_at');
        const ranges: Record<string, [
            number,
            number
        ]> = { weight_kg: [20, 500], waist_cm: [20, 300], chest_cm: [20, 300], arm_cm: [10, 150], thigh_cm: [10, 200] };
        if (Object.keys(ranges).every(k => r[k] == null))
            throw Error('Introduce al menos una medida');
        for (const [k, [min, max]] of Object.entries(ranges))
            number(k, min, max, false);
    }
}
function read(): Record<string, Row[]> {
    const raw = localStorage.getItem(KEY);
    if (!raw)
        return {};
    try {
        return JSON.parse(raw);
    }
    catch {
        throw Error('Los datos locales no se pueden leer. Conserva el almacenamiento antes de restaurar una copia.');
    }
}
function save(data: Record<string, Row[]>) {
    try {
        localStorage.setItem(KEY, JSON.stringify(data));
    }
    catch {
        throw Error('No se pudo guardar en este navegador. Revisa el espacio y exporta una copia.');
    }
}
export function exportLocal() { return JSON.stringify({ app: 'ACTIUM', version: 2, exported_at: new Date().toISOString(), tables: read() }, null, 2); }
export function importLocal(raw: string) {
    const backup = JSON.parse(raw);
    if (backup.app !== 'ACTIUM' || backup.version !== 2 || !backup.tables || typeof backup.tables !== 'object' || Array.isArray(backup.tables))
        throw Error('Copia ACTIUM v2 no válida');
    const clean: Record<string, Row[]> = {};
    for (const [table, rows] of Object.entries(backup.tables)) {
        if (!(TABLES as readonly string[]).includes(table) || !Array.isArray(rows) || rows.length > 50000)
            throw Error('Tabla de copia no válida');
        const ids = new Set();
        for (const r of rows) {
            if (!r || typeof r !== 'object' || Array.isArray(r) || typeof r.id !== 'string' || ids.has(r.id) || r.user_id !== LOCAL_USER)
                throw Error('Registros de copia no válidos');
            ids.add(r.id);
            validateRow(table, r);
        }
        clean[table] = rows;
    }
    for (const [child, parent, key] of [['meal_items', 'meals', 'meal_id'], ['workout_sets', 'workout_sessions', 'session_id'], ['workout_sets', 'exercises', 'exercise_id'], ['plan_exercises', 'workout_plans', 'plan_id'], ['plan_exercises', 'exercises', 'exercise_id']]) {
        if ((clean[child] || []).some(r => !(clean[parent] || []).some(p => p.id === r[key])))
            throw Error('La copia tiene relaciones incompletas');
    }
    save(clean);
}
class Query implements PromiseLike<any> {
    private filters: [
        string,
        any
    ][] = [];
    private op = 'select';
    private payload: Row[] = [];
    private one = false;
    private head = false;
    constructor(private table: string) { }
    select(_columns = '*', options?: {
        head?: boolean;
        count?: string;
    }) { this.head = !!options?.head; return this; }
    eq(key: string, value: any) { this.filters.push([key, value]); return this; }
    limit(_n: number) { return this; } // Local histories are complete, never silently truncated.
    order(_column: string) { return this; }
    range(_from: number, _to: number) { return this; }
    insert(value: Row | Row[]) { this.op = 'insert'; this.payload = Array.isArray(value) ? value : [value]; return this; }
    upsert(value: Row, _options?: unknown) { this.op = 'upsert'; this.payload = [value]; return this; }
    update(value: Row) { this.op = 'update'; this.payload = [value]; return this; }
    delete() { this.op = 'delete'; return this; }
    single() { this.one = true; return this; }
    then<T = any, U = never>(resolve?: ((value: any) => T | PromiseLike<T>) | null, reject?: ((reason: any) => U | PromiseLike<U>) | null): Promise<T | U> {
        return Promise.resolve().then(() => this.run()).then(resolve, reject);
    }
    private run() {
        try {
            if (!(TABLES as readonly string[]).includes(this.table))
                throw Error('Esta función necesita Supabase');
            const all = read(), rows = all[this.table] || [], matches = (r: Row) => this.filters.every(([k, v]) => r[k] === v);
            let result = rows.filter(matches);
            if (this.op === 'insert' || this.op === 'upsert') {
                result = this.payload.map(p => {
                    const old = this.op === 'upsert' ? rows.find(r => r.user_id === p.user_id && r.effective_from === p.effective_from) : null;
                    const now = new Date().toISOString();
                    const r = { id: crypto.randomUUID(), created_at: now, eaten_at: now, consumed_at: now, measured_at: now, started_at: now, completed_at: now, ...old, ...p };
                    if (old)
                        Object.assign(old, r);
                    else
                        rows.push(r);
                    return r;
                });
                all[this.table] = rows;
            }
            else if (this.op === 'update')
                result.forEach(r => Object.assign(r, this.payload[0]));
            else if (this.op === 'delete') {
                const ids = new Set(result.map(r => r.id));
                if (this.table === 'exercises' && [...(all.workout_sets || []), ...(all.plan_exercises || [])].some(r => ids.has(r.exercise_id)))
                    throw Error('Este ejercicio tiene series o rutinas y no se puede eliminar.');
                all[this.table] = rows.filter(r => !matches(r));
                const child = this.table === 'meals' ? 'meal_items' : this.table === 'workout_sessions' ? 'workout_sets' : null;
                if (child)
                    all[child] = (all[child] || []).filter(r => !ids.has(r[child === 'meal_items' ? 'meal_id' : 'session_id']));
            }
            if (this.op !== 'select') {
                if (this.op === 'update')
                    all[this.table] = rows;
                for (const r of all[this.table] || [])
                    validateRow(this.table, r);
                save(all);
            }
            return { data: this.head ? null : this.one ? result[0] : result, error: null, count: result.length };
        }
        catch (e) {
            return { data: null, error: e, count: 0 };
        }
    }
}
const unavailable = async () => ({ data: null, error: new Error('Disponible al conectar Supabase. Tus registros locales permanecen en este navegador.') });
export const localDb = {
    from: (table: string) => new Query(table),
    auth: {
        getUser: async () => ({ data: { user: { id: LOCAL_USER, email: 'Modo local · este dispositivo' } } }),
        onAuthStateChange: (_cb: unknown) => ({ data: { subscription: { unsubscribe() { } } } }),
        signOut: async () => { }, signUp: unavailable, signInWithPassword: unavailable
    },
    functions: { invoke: unavailable },
    storage: { from: (_bucket: string) => ({ upload: unavailable, remove: unavailable, createSignedUrl: unavailable }) }
};
