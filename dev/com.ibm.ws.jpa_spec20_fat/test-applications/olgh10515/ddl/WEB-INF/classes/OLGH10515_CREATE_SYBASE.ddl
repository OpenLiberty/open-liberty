CREATE TABLE ${schemaname}.criteria_car_origin (CAR_ID VARCHAR(255) NULL, CAR_VER INT NULL, component VARCHAR(255) NOT NULL, origin VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE);
CREATE TABLE ${schemaname}.SimpleEntityOLGH10515 (id NUMERIC(38) NOT NULL, content VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE INDEX I_CRTRRGN_CAR_ID ON ${schemaname}.criteria_car_origin (CAR_ID, CAR_VER);