-- D1 · Funciones y triggers (idempotente)

-- updated_at automático en statuses
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end $$;

drop trigger if exists trg_statuses_updated_at on public.statuses;
create trigger trg_statuses_updated_at
  before insert or update on public.statuses
  for each row execute function public.set_updated_at();

-- Crea profile + status 'free' al registrarse un usuario (no está en el contrato;
-- lo exigen las FK). display_name: metadata -> parte local del email -> 'Usuario'.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_name text;
begin
  v_name := coalesce(
    nullif(new.raw_user_meta_data ->> 'display_name', ''),
    nullif(split_part(coalesce(new.email, ''), '@', 1), ''),
    'Usuario');
  insert into public.profiles (id, display_name) values (new.id, v_name)
    on conflict (id) do nothing;
  insert into public.statuses (user_id, state, source) values (new.id, 'free', 'manual')
    on conflict (user_id) do nothing;
  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Vence temporizadores: devuelve a 'free' (CONTRATO §1). Devuelve filas afectadas.
create or replace function public.expire_statuses()
returns integer language plpgsql security definer set search_path = public as $$
declare
  n integer;
begin
  update public.statuses
     set state = 'free', until = null, source = 'timer'
   where state <> 'free' and until is not null and until < now();
  get diagnostics n = row_count;
  return n;
end $$;

-- Retención de motion_features: 14 días
create or replace function public.purge_motion_features()
returns integer language plpgsql security definer set search_path = public as $$
declare
  n integer;
begin
  delete from public.motion_features where window_start < now() - interval '14 days';
  get diagnostics n = row_count;
  return n;
end $$;

-- Solo el backend / pg_cron pueden ejecutarlas
revoke all on function public.expire_statuses()       from public, anon, authenticated;
revoke all on function public.purge_motion_features() from public, anon, authenticated;
grant execute on function public.expire_statuses()       to service_role;
grant execute on function public.purge_motion_features() to service_role;
