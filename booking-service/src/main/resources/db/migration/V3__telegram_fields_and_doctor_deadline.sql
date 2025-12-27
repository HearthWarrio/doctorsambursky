alter table patients
    add column if not exists address varchar(500),
    add column if not exists telegram_username varchar(100),
    add column if not exists whatsapp_number varchar(50),
    add column if not exists telegram_chat_id bigint;

alter table patients
    alter column email drop not null;

create unique index if not exists ux_patients_telegram_chat_id
    on patients(telegram_chat_id)
    where telegram_chat_id is not null;

alter table appointments
    add column if not exists updated_at timestamp,
    add column if not exists decline_reason varchar(1000),
    add column if not exists reschedule_proposed_time timestamp,
    add column if not exists doctor_decision_deadline_at timestamp;

update appointments
set updated_at = coalesce(updated_at, created_at)
where updated_at is null;

update appointments
set doctor_decision_deadline_at = coalesce(doctor_decision_deadline_at, created_at + interval '2 hours')
where doctor_decision_deadline_at is null;

create index if not exists ix_appointments_status_deadline
    on appointments(status, doctor_decision_deadline_at);