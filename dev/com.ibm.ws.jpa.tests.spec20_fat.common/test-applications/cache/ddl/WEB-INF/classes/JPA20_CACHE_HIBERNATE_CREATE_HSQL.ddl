CREATE TABLE ${schemaname}.CEmployee (id INTEGER NOT NULL, firstName VARCHAR(255), lastName VARCHAR(255), vacationDays INTEGER, version BIGINT, PRIMARY KEY (id));

CREATE TABLE ${schemaname}.CacheEntityCacheable (id INTEGER NOT NULL, intVal INTEGER, strVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CacheEntityCacheableNot (id INTEGER NOT NULL, intVal INTEGER, strVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CacheEntityCollection (id INTEGER NOT NULL, intVal INTEGER, strVal VARCHAR(255), ENTITYSIMPLE2_ID INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CacheEntitySimple1 (id INTEGER NOT NULL, intVal INTEGER, strVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CacheEntitySimple2 (id INTEGER NOT NULL, intVal INTEGER, strVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CECollection_CESimple1 (CACHEENTITYCOLLECTION_ID INTEGER, ENTITYSIMPLE1_ID INTEGER);

CREATE TABLE ${schemaname}.JPA20EMEntityA (id INTEGER NOT NULL, strData VARCHAR(255), version BIGINT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.JPA20EMEntityA_JPA20EMEntityB (ENTITYALIST_ID INTEGER, ENTITYBLIST_ID INTEGER);
CREATE TABLE ${schemaname}.JPA20EMEntityB (id INTEGER NOT NULL, strData VARCHAR(255), version BIGINT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.JPA20EMEntityC (id INTEGER NOT NULL, strData VARCHAR(255), version BIGINT, ENTITYA_ID INTEGER, ENTITYALAZY_ID INTEGER, PRIMARY KEY (id));

CREATE INDEX I_JP20CACHE_TYB_ELEMENT ON ${schemaname}.JPA20EMEntityA_JPA20EMEntityB (ENTITYBLIST_ID);
CREATE INDEX I_JP20CACHE_TYB_ENTITYALIST_ID ON ${schemaname}.JPA20EMEntityA_JPA20EMEntityB (ENTITYALIST_ID);
CREATE INDEX I_JP20CACHE_TYC_ENTITYA ON ${schemaname}.JPA20EMEntityC (ENTITYA_ID);
CREATE INDEX I_JP20CACHE_TYC_ENTITYALAZY ON ${schemaname}.JPA20EMEntityC (ENTITYALAZY_ID);


CREATE INDEX I_CCHNCTN_ENTITYSIMPLE2 ON ${schemaname}.CacheEntityCollection (ENTITYSIMPLE2_ID);
CREATE INDEX I_CCLLPL1_CACHEENTITYCOLLECTION_ID ON ${schemaname}.CECollection_CESimple1 (CACHEENTITYCOLLECTION_ID);
CREATE INDEX I_CCLLPL1_ELEMENT ON ${schemaname}.CECollection_CESimple1 (ENTITYSIMPLE1_ID);