import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';
import { localDb } from './local';
export const url = import.meta.env.VITE_SUPABASE_URL || '';
export const key = import.meta.env.VITE_SUPABASE_ANON_KEY || '';
export const configured = Boolean(url && key && url.startsWith('https://'));
export const isLocal = !configured;
export const db = (configured ? createClient(url, key, { auth: { persistSession: true, autoRefreshToken: true } }) : localDb) as SupabaseClient<any>;
