-- EclipseLink
CREATE TABLE ${schemaname}.PREV_ADDRESSES (SpecEmployeeOLGH16588_ID INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zipcode VARCHAR(255)) ENGINE = innodb;
CREATE TABLE ${schemaname}.SpecEmployeeOLGH16588 (id INTEGER NOT NULL, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zipcode VARCHAR(255), PRIMARY KEY (id)) ENGINE = innodb;
CREATE TABLE ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (SpecEmployeeOLGH16588_ID INTEGER, phones_ID INTEGER) ENGINE = innodb;
CREATE TABLE ${schemaname}.SpecPhoneOLGH16588 (id INTEGER NOT NULL, vendor VARCHAR(255), PRIMARY KEY (id)) ENGINE = innodb;
CREATE INDEX ${schemaname}.I_PRV_SSS_SpecEmployeeOLGH16588_ID ON ${schemaname}.PREV_ADDRESSES (SpecEmployeeOLGH16588_ID);
CREATE INDEX ${schemaname}.I_SPCM588_ELEMENT ON ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (phones_ID);
CREATE INDEX ${schemaname}.I_SPCM588_SpecEmployeeOLGH16588_ID ON ${schemaname}.SpecEmployeeOLGH16588_SpecPhoneOLGH16588 (SpecEmployeeOLGH16588_ID);