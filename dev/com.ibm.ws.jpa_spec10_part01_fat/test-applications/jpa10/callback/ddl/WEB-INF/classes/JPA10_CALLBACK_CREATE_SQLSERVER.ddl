CREATE TABLE ${schemaname}.CALLBACKENTITY (id INT NOT NULL, name VARCHAR(255), protectionType VARCHAR(31), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_CLLBTTY_DTYPE ON ${schemaname}.CALLBACKENTITY (protectionType);
CREATE TABLE ${schemaname}.CallbkEntNSptDefCbk (id INT NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CallbkEntSptDefCbk (id INT NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntNSptDefCbk (id INT NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCallbkEntSptDefCbk (id INT NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.OOIRootEntity (id INT NOT NULL, name VARCHAR(255), leafType VARCHAR(31), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_ROTNTTY_DTYPE ON ${schemaname}.OOIRootEntity (leafType);
