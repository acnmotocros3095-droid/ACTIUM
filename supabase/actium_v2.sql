-- Run AFTER actium_mvp.sql; safe to re-run. Atomic meal creation, under caller RLS.
create or replace function public.actium_add_meal(
 p_type text, p_name text, p_grams numeric, p_kcal numeric,
 p_protein numeric, p_carbs numeric, p_fat numeric, p_eaten_at timestamptz default now()
) returns uuid language plpgsql security invoker set search_path='' as $$
declare meal uuid; owner_id uuid := auth.uid();
begin
 if owner_id is null then raise exception 'Authentication required'; end if;
 if p_name is null or length(trim(p_name))=0 then raise exception 'Food name required'; end if;
 insert into public.meals(user_id,meal_type,eaten_at) values(owner_id,p_type,p_eaten_at) returning id into meal;
 insert into public.meal_items(user_id,meal_id,food_name,grams,kcal,protein_g,carbs_g,fat_g)
 values(owner_id,meal,trim(p_name),p_grams,p_kcal,p_protein,p_carbs,p_fat);
 return meal;
end $$;
revoke all on function public.actium_add_meal(text,text,numeric,numeric,numeric,numeric,numeric,timestamptz) from public,anon;
grant execute on function public.actium_add_meal(text,text,numeric,numeric,numeric,numeric,numeric,timestamptz) to authenticated;

create or replace function public.assert_owned_links() returns trigger language plpgsql set search_path='' as $$
begin
 if tg_table_name='meal_items' then
   if new.food_id is not null then
     if not exists(select 1 from public.foods where id=new.food_id and user_id=new.user_id) then raise exception 'Food does not belong to user'; end if;
   end if;
 elsif tg_table_name in ('meal_media','body_photos','exercise_media') then
   if not exists(select 1 from public.media_assets where id=new.media_id and user_id=new.user_id and bucket=case tg_table_name when 'meal_media' then 'meal-photos' when 'body_photos' then 'body-progress' else 'exercise-media' end) then raise exception 'Media does not belong to user or wrong bucket'; end if;
 end if;
 return new;
end $$;
