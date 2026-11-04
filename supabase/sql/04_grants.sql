-- D1 · Grants. anon no accede a nada; authenticated lo mínimo; RLS filtra filas (D2).
revoke all on all tables in schema public from anon, authenticated;

-- Defensa: RLS activa ya aquí (sin políticas = denegar todo hasta D2). Idempotente.
alter table public.profiles        enable row level security;
alter table public.groups          enable row level security;
alter table public.group_members   enable row level security;
alter table public.statuses        enable row level security;
alter table public.schedule_blocks enable row level security;
alter table public.motion_features enable row level security;

grant select, update                 on public.profiles        to authenticated;
grant select, insert, update, delete on public.groups          to authenticated;
grant select, insert, delete         on public.group_members   to authenticated;
grant select, insert, update         on public.statuses        to authenticated;
grant select, insert, update, delete on public.schedule_blocks to authenticated;
grant select, insert                 on public.motion_features to authenticated;

grant all on all tables in schema public to service_role;
