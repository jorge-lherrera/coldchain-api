CREATE TABLE inbox_event (
    event_id      uuid          NOT NULL,
    type          varchar(40)   NOT NULL,
    excursion_id  uuid          NOT NULL,
    occurred_at   timestamptz   NOT NULL,
    received_at   timestamptz   NOT NULL,
    status        varchar(20)   NOT NULL,
    processed_at  timestamptz,
    CONSTRAINT pk_inbox_event PRIMARY KEY (event_id),
    CONSTRAINT ck_inbox_event_type CHECK (type IN ('ExcursionOpened', 'ExcursionClosed')),
    CONSTRAINT ck_inbox_event_status CHECK (status IN ('RECEIVED', 'PROCESSED')),
    CONSTRAINT ck_inbox_event_processed CHECK ((status = 'PROCESSED') = (processed_at IS NOT NULL))
);

CREATE INDEX ix_inbox_event_excursion ON inbox_event (excursion_id);

CREATE TABLE excursion_alert (
    id                  uuid           NOT NULL,
    excursion_id        uuid           NOT NULL,
    shipment_id         uuid           NOT NULL,
    organization_id     uuid           NOT NULL,
    kind                varchar(20)    NOT NULL,
    status              varchar(20)    NOT NULL,
    opened_at           timestamptz,
    closed_at           timestamptz,
    peak_celsius        numeric(5, 2),
    duration_minutes    integer,
    probable_cause      varchar(40),
    severity            varchar(10),
    confidence          numeric(3, 2),
    summary_en          varchar(1000),
    summary_es          varchar(1000),
    summary_pt          varchar(1000),
    recommended_action  varchar(500),
    source              varchar(10),
    model               varchar(100),
    prompt_version      varchar(20),
    input_tokens        integer,
    output_tokens       integer,
    latency_ms          integer,
    context_available   boolean,
    triaged_at          timestamptz,
    version             integer        NOT NULL DEFAULT 0,
    created_at          timestamptz    NOT NULL,
    updated_at          timestamptz    NOT NULL,
    CONSTRAINT pk_excursion_alert PRIMARY KEY (id),
    CONSTRAINT uq_excursion_alert_excursion UNIQUE (excursion_id),
    CONSTRAINT ck_excursion_alert_kind CHECK (kind IN ('ABOVE_MAX', 'BELOW_MIN')),
    CONSTRAINT ck_excursion_alert_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_excursion_alert_cause CHECK (probable_cause IN
        ('HANDLING_DURING_HANDOFF', 'REFRIGERATION_FAILURE', 'DOOR_OPENING', 'SENSOR_FAULT', 'UNKNOWN')),
    CONSTRAINT ck_excursion_alert_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_excursion_alert_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_excursion_alert_source CHECK (source IN ('LLM', 'RULES')),
    CONSTRAINT ck_excursion_alert_triage_complete CHECK (
        (triaged_at IS NULL AND source IS NULL AND severity IS NULL AND probable_cause IS NULL)
        OR (triaged_at IS NOT NULL AND source IS NOT NULL AND severity IS NOT NULL AND probable_cause IS NOT NULL)),
    CONSTRAINT ck_excursion_alert_model CHECK ((source = 'LLM') = (model IS NOT NULL))
);

CREATE INDEX ix_excursion_alert_shipment ON excursion_alert (shipment_id, triaged_at);

CREATE TABLE event_publication (
    id                      uuid           NOT NULL,
    listener_id             varchar(512)   NOT NULL,
    event_type              varchar(512)   NOT NULL,
    serialized_event        text           NOT NULL,
    publication_date        timestamptz    NOT NULL,
    completion_date         timestamptz,
    status                  varchar(20),
    completion_attempts     integer        NOT NULL DEFAULT 0,
    last_resubmission_date  timestamptz,
    CONSTRAINT pk_event_publication PRIMARY KEY (id),
    CONSTRAINT ck_event_publication_status CHECK (status IN
        ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

CREATE INDEX ix_event_publication_serialized_event ON event_publication USING hash (serialized_event);

CREATE INDEX ix_event_publication_completion ON event_publication (completion_date);
