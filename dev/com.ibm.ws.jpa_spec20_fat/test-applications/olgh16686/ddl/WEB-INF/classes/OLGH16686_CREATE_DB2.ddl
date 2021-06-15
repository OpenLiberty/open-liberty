CREATE TABLE ${schemaname}.ElementCollectionEntityOLGH16686 (id INTEGER NOT NULL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.EntMapDateTemporal (ELEME_ID INTEGER, mykey DATE NOT NULL, temporalValue DATE);
CREATE INDEX ${schemaname}.I_NTMPPRL_ELEME_ID ON ${schemaname}.EntMapDateTemporal (ELEME_ID);