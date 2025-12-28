alter table appointments
    add column if not exists doctor_notified boolean not null default false,
    add column if not exists doctor_notified_at timestamp null,
    add column if not exists timeout_decline_notified boolean not null default false,
    add column if not exists timeout_decline_notified_at timestamp null;