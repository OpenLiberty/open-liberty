CREATE TABLE ${schemaname}.CALLBACKENTITY (id NUMBER NOT NULL, name VARCHAR2(255), protectionType VARCHAR2(31), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_CLLBTTY_DTYPE ON ${schemaname}.CALLBACKENTITY (protectionType);
CREATE TABLE ${schemaname}.OOIRootEntity (id NUMBER NOT NULL, name VARCHAR2(255), leafType VARCHAR2(31), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_OOIRTTY_DTYPE ON ${schemaname}.OOIRootEntity (leafType);
CREATE TABLE ${schemaname}.CallbkEntNSptDefCbk (id NUMBER NOT NULL, name VARCHAR2(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CallbkEntSptDefCbk (id NUMBER NOT NULL, name VARCHAR2(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntNSptDefCbk (id NUMBER NOT NULL, name VARCHAR2(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntSptDefCbk (id NUMBER NOT NULL, name VARCHAR2(255), PRIMARY KEY (id));