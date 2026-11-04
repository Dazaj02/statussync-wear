-- D0 · Tablas (CONTRATO.md §3). Idempotente.

create table if not exists public.profiles (
  id           uuid primary key references auth.users(id) on delete cascade,
  display_name text not null,
  created_at   timestamptz not null default now()
);

create table if not exists public.groups (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  owner_id    uuid not null references public.profiles(id) on delete cascade,
  invite_code text not null unique
);

create table if not exists public.group_members (
  group_id uuid not null references public.groups(id) on delete cascade,
  user_id  uuid not null references public.profiles(id) on delete cascade,
  primary key (group_id, user_id)
);

create table if not exists public.statuses (
  user_id    uuid primary key references public.profiles(id) on delete cascade,
  state      text not null default 'free'
             check (state in ('free','focus','class','call')),
  until      timestamptz,
  source     text not null default 'manual'
             check (source in ('manual','timer','auto_calendar','auto_motion')),
  updated_at timestamptz not null default now(),
  constraint statuses_until_required check (state = 'free' or until is not null)
);

create table if not exists public.schedule_blocks (
  id        uuid primary key default gen_random_uuid(),
  user_id   uuid not null references public.profiles(id) on delete cascade,
  state     text not null check (state in ('free','focus','class','call')),
  starts_at timestamptz not null,
  ends_at   timestamptz not null,
  constraint schedule_blocks_range check (ends_at > starts_at)
);

-- Solo agregados, nunca datos crudos.
create table if not exists public.motion_features (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references public.profiles(id) on delete cascade,
  window_start    timestamptz not null,
  steps           integer not null check (steps >= 0),
  stillness_ratio numeric(4,3) not null check (stillness_ratio between 0 and 1),
  state_hint      text check (state_hint in ('free','focus','class','call'))
);
