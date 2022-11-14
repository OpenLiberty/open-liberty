CREATE TABLE ${schemaname}.CALLBACKENTITY (id INTEGER NOT NULL, name VARCHAR(255), protectionType VARCHAR(255), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_CLLBTTY_DTYPE ON ${schemaname}.CALLBACKENTITY (protectionType);
CREATE TABLE ${schemaname}.OOIRootEntity (id INTEGER NOT NULL, name VARCHAR(255), leafType VARCHAR(255), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_OOIRTTY_DTYPE ON ${schemaname}.OOIRootEntity (leafType);
CREATE TABLE ${schemaname}.CallbkEntNSptDefCbk (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CallbkEntSptDefCbk (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntNSptDefCbk (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntSptDefCbk (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));