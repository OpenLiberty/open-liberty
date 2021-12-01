CREATE TABLE ${schemaname}.ELEMENTCOLLECTIONENTITYOLGH166 (id NUMBER NOT NULL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.EntMapDateTemporal (ELEME_ID NUMBER, mykey DATE NOT NULL, temporalValue DATE);
CREATE INDEX ${schemaname}.I_NTMPPRL_ELEME_ID ON ${schemaname}.EntMapDateTemporal (ELEME_ID);