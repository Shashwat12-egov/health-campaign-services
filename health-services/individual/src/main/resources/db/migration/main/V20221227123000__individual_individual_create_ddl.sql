CREATE TABLE IF NOT EXISTS INDIVIDUAL
(
    id                character varying(64),
    userId            character varying(64),
    clientReferenceId character varying(64),
    tenantId          character varying(1000),
    givenName         character varying(200),
    familyName        character varying(200),
    otherNames        character varying(200),
    -- YYYYMMDD
    dateOfBirth       bigint,
    gender            character varying(10),
    bloodGroup        character varying(10),
    latitude          double precision,
    longitude         double precision,
    mobileNumber      character varying(20),
    altContactNumber  character varying(20),
    email             character varying(200),
    fatherName        character varying(100),
    husbandName       character varying(100),
    photo             text,
    -- Comma separated
    addressIds        text,
    additionalDetails jsonb,
    createdBy         character varying(64),
    lastModifiedBy    character varying(64),
    createdTime       bigint,
    lastModifiedTime  bigint,
    rowVersion        bigint,
    isDeleted         boolean,
    CONSTRAINT uk_individual_id PRIMARY KEY (id),
    CONSTRAINT uk_individual_client_reference_id UNIQUE (clientReferenceId)
);