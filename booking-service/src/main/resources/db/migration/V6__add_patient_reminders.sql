alter table appointments
    add column if not exists reminder_24_sent_at timestamp null,
    add column if not exists reminder_2h_sent_at timestamp null;