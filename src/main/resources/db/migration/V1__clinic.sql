CREATE SEQUENCE chart_sequence START WITH 1;
CREATE TABLE patients (
    id VARCHAR(36) PRIMARY KEY,
    chart VARCHAR(12) NOT NULL UNIQUE,
    name_normalized VARCHAR(160) NOT NULL,
    birth_date DATE NOT NULL,
    status VARCHAR(12) NOT NULL,
    merged_into VARCHAR(36),
    version BIGINT NOT NULL,
    payload TEXT NOT NULL
);
CREATE TABLE identities (
    document_key VARCHAR(400) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL REFERENCES patients(id)
);
CREATE TABLE receipts (
    actor VARCHAR(100) NOT NULL,
    token VARCHAR(36) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    patient_id VARCHAR(36) NOT NULL REFERENCES patients(id),
    PRIMARY KEY(actor, token)
);
CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    patient_id VARCHAR(36),
    outcome VARCHAR(20) NOT NULL,
    fields VARCHAR(2000) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    refs VARCHAR(1000) NOT NULL
);
CREATE INDEX audit_by_patient ON audit_events(patient_id, occurred_at);
CREATE INDEX patient_search ON patients(name_normalized, chart);
