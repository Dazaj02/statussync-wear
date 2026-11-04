-- D1 · Índices (idempotente)
create index if not exists idx_groups_owner          on public.groups (owner_id);
create index if not exists idx_group_members_user    on public.group_members (user_id);
-- expire_statuses(): solo estados con temporizador activo
create index if not exists idx_statuses_until        on public.statuses (until) where state <> 'free';
create index if not exists idx_schedule_user_start   on public.schedule_blocks (user_id, starts_at);
create index if not exists idx_motion_user_window    on public.motion_features (user_id, window_start desc);
-- purga de retención (14 días)
create index if not exists idx_motion_window         on public.motion_features (window_start);
