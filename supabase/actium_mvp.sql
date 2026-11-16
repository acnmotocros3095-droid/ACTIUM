-- ACTIUM MVP | Supabase PostgreSQL | Execute in SQL Editor
create extension if not exists pgcrypto;

create table public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 display_name text,
 height_cm numeric(5,2) check(height_cm between 50 and 250),
 birth_date date,
 timezone text not null default 'America/La_Paz',
 created_at timestamptz not null default now()
);
create table public.daily_goals (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 effective_from date not null,
 calories integer not null check(calories between 500 and 10000),
 protein_g numeric(7,2) not null check(protein_g >= 0),
 fat_g numeric(7,2) not null check(fat_g >= 0),
 carbs_g numeric(7,2) not null check(carbs_g >= 0),
 water_ml integer check(water_ml between 0 and 10000),
 sleep_minutes integer check(sleep_minutes between 0 and 1440),
 unique(user_id,effective_from)
);
create table public.foods (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 name text not null check(length(trim(name))>0),
 kcal_per_100g numeric(8,2) not null check(kcal_per_100g>=0),
 protein_per_100g numeric(8,2) not null check(protein_per_100g>=0),
 carbs_per_100g numeric(8,2) not null check(carbs_per_100g>=0),
 fat_per_100g numeric(8,2) not null check(fat_per_100g>=0),
 created_at timestamptz not null default now()
);
create table public.meals (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 eaten_at timestamptz not null default now(),
 meal_type text not null check(meal_type in ('breakfast','lunch','snack','dinner','other')),
 notes text,
 created_at timestamptz not null default now(),
 unique(id,user_id)
);
create table public.meal_items (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 meal_id uuid not null,
 food_id uuid,
 food_name text not null,
 grams numeric(9,2) not null check(grams>0),
 kcal numeric(9,2) not null check(kcal>=0),
 protein_g numeric(9,2) not null check(protein_g>=0),
 carbs_g numeric(9,2) not null check(carbs_g>=0),
 fat_g numeric(9,2) not null check(fat_g>=0),
 foreign key(meal_id,user_id) references public.meals(id,user_id) on delete cascade,
 foreign key(food_id) references public.foods(id) on delete set null
);
create table public.exercises (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 name text not null,
 muscle_group text not null,
 instructions text,
 created_at timestamptz not null default now(),
 unique(id,user_id)
);
create table public.workout_plans (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 name text not null,
 active boolean not null default true,
 created_at timestamptz not null default now(),
 unique(id,user_id)
);
create table public.plan_exercises (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 plan_id uuid not null,
 exercise_id uuid not null,
 weekday smallint not null check(weekday between 1 and 7),
 position integer not null check(position>=0),
 target_sets integer not null check(target_sets between 1 and 20),
 min_reps integer not null check(min_reps>=1),
 max_reps integer not null check(max_reps>=min_reps),
 rest_seconds integer check(rest_seconds between 0 and 1800),
 foreign key(plan_id,user_id) references public.workout_plans(id,user_id) on delete cascade,
 foreign key(exercise_id,user_id) references public.exercises(id,user_id),
 unique(plan_id,weekday,position)
);
create table public.workout_sessions (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 started_at timestamptz not null default now(),
 ended_at timestamptz,
 notes text,
 check(ended_at is null or ended_at>=started_at),
 unique(id,user_id)
);
create table public.workout_sets (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 session_id uuid not null,
 exercise_id uuid not null,
 set_number integer not null check(set_number>=1),
 weight_kg numeric(8,2) check(weight_kg>=0),
 reps integer check(reps>=0),
 rir numeric(3,1) check(rir between 0 and 10),
 rest_seconds integer check(rest_seconds between 0 and 1800),
 completed_at timestamptz not null default now(),
 foreign key(session_id,user_id) references public.workout_sessions(id,user_id) on delete cascade,
 foreign key(exercise_id,user_id) references public.exercises(id,user_id)
);
create table public.water_entries (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 consumed_at timestamptz not null default now(),
 amount_ml integer not null check(amount_ml between 1 and 5000)
);
create table public.sleep_entries (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 slept_at timestamptz not null,
 woke_at timestamptz not null,
 quality smallint check(quality between 1 and 5),
 notes text,
 check(woke_at>slept_at and woke_at<=slept_at+interval '24 hours')
);
create table public.body_measurements (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 measured_at timestamptz not null default now(),
 weight_kg numeric(6,2) check(weight_kg between 20 and 500),
 waist_cm numeric(6,2) check(waist_cm between 20 and 300),
 chest_cm numeric(6,2) check(chest_cm between 20 and 300),
 arm_cm numeric(6,2) check(arm_cm between 10 and 150),
 thigh_cm numeric(6,2) check(thigh_cm between 10 and 200),
 notes text,
 check(weight_kg is not null or waist_cm is not null or chest_cm is not null or arm_cm is not null or thigh_cm is not null)
);
create table public.media_assets (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 bucket text not null check(bucket in ('body-progress','meal-photos','exercise-media')),
 object_path text not null,
 mime_type text not null,
 created_at timestamptz not null default now(),
 unique(bucket,object_path),
 check(object_path like user_id::text || '/%')
);
create table public.meal_media (
 user_id uuid not null references auth.users(id) on delete cascade,
 meal_id uuid not null,
 media_id uuid not null,
 primary key(meal_id,media_id),
 foreign key(meal_id,user_id) references public.meals(id,user_id) on delete cascade
);
create table public.body_photos (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references auth.users(id) on delete cascade,
 media_id uuid not null,
 view_type text not null check(view_type in ('front','back','left','right')),
 taken_at timestamptz not null default now(),
 unique(media_id)
);
create table public.exercise_media (
 user_id uuid not null references auth.users(id) on delete cascade,
 exercise_id uuid not null,
 media_id uuid not null,
 primary key(exercise_id,media_id),
 foreign key(exercise_id,user_id) references public.exercises(id,user_id) on delete cascade
);
-- Enforce ownership of linked media and foods, even when foreign keys exist.
create or replace function public.assert_owned_links() returns trigger language plpgsql set search_path='' as $$
begin
 if tg_table_name='meal_items' and new.food_id is not null and not exists(select 1 from public.foods where id=new.food_id and user_id=new.user_id) then raise exception 'Food does not belong to user'; end if;
 if tg_table_name in ('meal_media','body_photos','exercise_media') and not exists(select 1 from public.media_assets where id=new.media_id and user_id=new.user_id and bucket=case tg_table_name when 'meal_media' then 'meal-photos' when 'body_photos' then 'body-progress' else 'exercise-media' end) then raise exception 'Media does not belong to user or wrong bucket'; end if;
 return new;
end $$;
create trigger own_food before insert or update on public.meal_items for each row execute function public.assert_owned_links();
create trigger own_meal_media before insert or update on public.meal_media for each row execute function public.assert_owned_links();
create trigger own_body_photo before insert or update on public.body_photos for each row execute function public.assert_owned_links();
create trigger own_exercise_media before insert or update on public.exercise_media for each row execute function public.assert_owned_links();
-- Prevent reassignment of ownership on updates.
create or replace function public.prevent_user_reassignment() returns trigger language plpgsql set search_path='' as $$
begin
 if new.user_id is distinct from old.user_id then raise exception 'Cannot change record owner'; end if;
 return new;
end $$;
do $$ declare t text; begin
 foreach t in array array['daily_goals','foods','meals','meal_items','exercises','workout_plans','plan_exercises','workout_sessions','workout_sets','water_entries','sleep_entries','body_measurements','media_assets','meal_media','body_photos','exercise_media'] loop
 execute format('create trigger prevent_owner_change before update on public.%I for each row execute function public.prevent_user_reassignment()',t);
 end loop;
end $$;
-- Generic RLS: all personal rows are owned by auth.uid().
do $$ declare t text; begin
 foreach t in array array['profiles','daily_goals','foods','meals','meal_items','exercises','workout_plans','plan_exercises','workout_sessions','workout_sets','water_entries','sleep_entries','body_measurements','media_assets','meal_media','body_photos','exercise_media'] loop
 execute format('alter table public.%I enable row level security',t);
 execute format('create policy "read_own" on public.%I for select to authenticated using (%I = (select auth.uid()))',t,case when t='profiles' then 'id' else 'user_id' end);
 execute format('create policy "insert_own" on public.%I for insert to authenticated with check (%I = (select auth.uid()))',t,case when t='profiles' then 'id' else 'user_id' end);
 execute format('create policy "update_own" on public.%I for update to authenticated using (%I = (select auth.uid())) with check (%I = (select auth.uid()))',t,case when t='profiles' then 'id' else 'user_id' end,case when t='profiles' then 'id' else 'user_id' end);
 execute format('create policy "delete_own" on public.%I for delete to authenticated using (%I = (select auth.uid()))',t,case when t='profiles' then 'id' else 'user_id' end);
 end loop;
end $$;
-- Storage: user UUID is the first path segment. Buckets remain private.
insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types) values
 ('body-progress','body-progress',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('meal-photos','meal-photos',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('exercise-media','exercise-media',false,52428800,array['image/jpeg','image/png','image/webp','video/mp4'])
on conflict (id) do nothing;
create policy "actium_storage_read" on storage.objects for select to authenticated using (bucket_id in ('body-progress','meal-photos','exercise-media') and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "actium_storage_insert" on storage.objects for insert to authenticated with check (bucket_id in ('body-progress','meal-photos','exercise-media') and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "actium_storage_update" on storage.objects for update to authenticated using (bucket_id in ('body-progress','meal-photos','exercise-media') and (storage.foldername(name))[1]=(select auth.uid())::text) with check (bucket_id in ('body-progress','meal-photos','exercise-media') and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "actium_storage_delete" on storage.objects for delete to authenticated using (bucket_id in ('body-progress','meal-photos','exercise-media') and (storage.foldername(name))[1]=(select auth.uid())::text);
create index meals_user_date_idx on public.meals(user_id,eaten_at desc);
create index water_user_date_idx on public.water_entries(user_id,consumed_at desc);
create index workout_user_date_idx on public.workout_sessions(user_id,started_at desc);
create index measurements_user_date_idx on public.body_measurements(user_id,measured_at desc);
create index sleep_user_date_idx on public.sleep_entries(user_id,slept_at desc);
-- Note: set up profile and initial goals from authenticated client after signup.
