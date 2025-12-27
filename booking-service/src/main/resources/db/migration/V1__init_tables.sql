CREATE TABLE patients (
  id              SERIAL PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  phone           VARCHAR(20)  NOT NULL UNIQUE,
  email           VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE appointments (
  id                SERIAL PRIMARY KEY,
  patient_id        INT   NOT NULL REFERENCES patients(id),
  appointment_time  TIMESTAMP NOT NULL,
  status            VARCHAR(20) NOT NULL,
  created_at        TIMESTAMP NOT NULL,
  payment_id        VARCHAR(100),
  paid_amount       INT
);
