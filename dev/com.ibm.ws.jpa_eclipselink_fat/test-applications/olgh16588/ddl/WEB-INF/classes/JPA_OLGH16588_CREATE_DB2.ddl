-- EclipseLink
CREATE TABLE ${schemaname}.PREV_ADDRESSES (SpecEmployeeOLGH16588_ID INTEGER, city VARCHAR(254), state VARCHAR(254), street VARCHAR(254), zipcode VARCHAR(254));
CREATE TABLE ${schemaname}.SpecEmployeeOLGH16588 (id INTEGER NOT NULL, city VARCHAR(254), state VARCHAR(254), street VARCHAR(254), zipcode VARCHAR(254), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (SpecEmployeeOLGH16588_ID INTEGER, phones_ID INTEGER);
CREATE TABLE ${schemaname}.SpecPhoneOLGH16588 (id INTEGER NOT NULL, vendor VARCHAR(254), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_PRV_SSS_SpecEmployeeOLGH16588_ID ON ${schemaname}.PREV_ADDRESSES (SpecEmployeeOLGH16588_ID);
CREATE INDEX ${schemaname}.I_SPCM588_ELEMENT ON ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (phones_ID);
CREATE INDEX ${schemaname}.I_SPCM588_SpecEmployeeOLGH16588_ID ON ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (SpecEmployeeOLGH16588_ID);