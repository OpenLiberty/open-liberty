CREATE TABLE ${schemaname}.SEQUENCE (SEQ_NAME VARCHAR(50) NOT NULL, SEQ_COUNT DECIMAL(15), PRIMARY KEY (SEQ_NAME));

CREATE TABLE ${schemaname}.OPENJPA_SEQUENCES_TABLE (ID VARCHAR(255) NOT NULL, SEQUENCE_VALUE BIGINT, PRIMARY KEY (ID));
CREATE TABLE ${schemaname}.OPENJPA_SEQUENCE_TABLE (ID SMALLINT NOT NULL, SEQUENCE_VALUE BIGINT, PRIMARY KEY (ID));

CREATE TABLE ${schemaname}.TableIDGen4Table    (GEN_NAME VARCHAR(255) NOT NULL, GEN_VAL BIGINT, PRIMARY KEY (GEN_NAME));
CREATE TABLE ${schemaname}.TableIDGenTable     (GEN_NAME VARCHAR(255) NOT NULL, GEN_VAL BIGINT, PRIMARY KEY (GEN_NAME));
CREATE TABLE ${schemaname}.XMLTableIDGen4Table (GEN_NAME VARCHAR(255) NOT NULL, GEN_VAL BIGINT, PRIMARY KEY (GEN_NAME));
CREATE TABLE ${schemaname}.XMLTableIDGenTable  (GEN_NAME VARCHAR(255) NOT NULL, GEN_VAL BIGINT, PRIMARY KEY (GEN_NAME));


INSERT INTO  ${schemaname}.SEQUENCE (SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN_SEQUENCE', 0);
INSERT INTO  ${schemaname}.SEQUENCE (SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);
INSERT INTO  ${schemaname}.SEQUENCE (SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN_TABLE', 0);
INSERT INTO  ${schemaname}.SEQUENCE (SEQ_NAME, SEQ_COUNT) values ('TableType2Generator', 0);
INSERT INTO  ${schemaname}.SEQUENCE (SEQ_NAME, SEQ_COUNT) values ('XMLTableType2Generator', 0);

INSERT INTO  ${schemaname}.TableIDGenTable  (GEN_NAME, GEN_VAL) values ('TableType3Generator', 0);
INSERT INTO  ${schemaname}.TableIDGen4Table (GEN_NAME, GEN_VAL) values ('TableType4Generator', 0);

INSERT INTO  ${schemaname}.XMLTableIDGenTable  (GEN_NAME, GEN_VAL) values ('XMLTableType3Generator', 0);
INSERT INTO  ${schemaname}.XMLTableIDGen4Table (GEN_NAME, GEN_VAL) values ('XMLTableType4Generator', 0);


              
CREATE TABLE jpaschema.ACfgFldEn (id INTEGER NOT NULL, floatValColumnPrecision REAL, floatValColumnScale REAL, intValColName INTEGER, notNullable BLOB NOT NULL, stringValColumnLength VARCHAR(12), stringValEager VARCHAR(255), stringValLazy VARCHAR(255), stringValOptional VARCHAR(255), uniqueConstraintString VARCHAR(255) NOT NULL, uniqueString VARCHAR(255) NOT NULL, PRIMARY KEY (id), UNIQUE (uniqueString), UNIQUE (uniqueConstraintString));
CREATE TABLE jpaschema.AltColumnTable (AttrConfigFieldEntity_id INTEGER, id INTEGER, intValCol INTEGER);

CREATE TABLE jpaschema.XACfgFldE (id INTEGER NOT NULL, floatValColumnPrecision REAL, floatValColumnScale REAL, intValColName INTEGER, notNullable BLOB NOT NULL, stringValColumnLength VARCHAR(12), stringValEager VARCHAR(255), stringValLazy VARCHAR(255), stringValOptional VARCHAR(255), uniqueConstraintString VARCHAR(255) NOT NULL, uniqueString VARCHAR(255) NOT NULL, PRIMARY KEY (id), UNIQUE (uniqueString), UNIQUE (uniqueConstraintString));
CREATE TABLE jpaschema.XAltColumnTable (XMLAttrConfigFieldEntity_id INTEGER, id INTEGER, XMLIntValCol INTEGER);

CREATE TABLE ${schemaname}.AnnEmbedMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnnMSCMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnnMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.DatatypeSupPropTestEntity (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault CLOB, charAttrDefault CHAR(254), charWrapperArrayAttrDefault CLOB, characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.DatatypeSupTestEntity (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault CLOB, charAttrDefault CHAR(254), charWrapperArrayAttrDefault CLOB, characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.EmbeddableIdEntity (country VARCHAR(255) NOT NULL, id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (country, id));
CREATE TABLE ${schemaname}.EmbeddedObjectAOEntity (id INTEGER NOT NULL, localIntVal INTEGER, localStrVal VARCHAR(255), booleanVal SMALLINT, byteVal SMALLINT, charVal CHAR(254), doubleVal DOUBLE, floatVal REAL, intValCol INTEGER, longValCol BIGINT, shortVal SMALLINT, stringVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.EmbeddedObjectEntity (id INTEGER NOT NULL, localIntVal INTEGER, localStrVal VARCHAR(255), booleanVal SMALLINT, byteVal SMALLINT, charVal CHAR(254), doubleVal DOUBLE, floatVal REAL, intVal INTEGER, longVal BIGINT, shortVal SMALLINT, stringVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.IdClassEntity (country VARCHAR(255) NOT NULL, id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (country, id));
CREATE TABLE ${schemaname}.PKEntityByte (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityByteWrapper (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityChar (pkey CHAR(254) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityCharacterWrapper (pkey CHAR(254) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityInt (pkey INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityIntWrapper (pkey INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityJavaSqlDate (pkey DATE NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityJavaUtilDate (pkey DATE NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityLong (pkey BIGINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityLongWrapper (pkey BIGINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityShort (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityShortWrapper (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKEntityString (pkey VARCHAR(255) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.PKGenAutoEntity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenIdentityEntity (id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenSequenceType1Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenSequenceType2Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenTableType1Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenTableType2Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenTableType3Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGenTableType4Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.ReadOnlyEntity (id INTEGER NOT NULL, intVal INTEGER, noInsertIntVal INTEGER, noUpdatableIntVal INTEGER, readOnlyIntVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.SEC_TABLE1 (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));
CREATE TABLE ${schemaname}.SEC_TABLE2AMSC (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));
CREATE TABLE ${schemaname}.SEC_TABLEEMB (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));
CREATE TABLE ${schemaname}.SerialDatatypeSupPropTE (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault CLOB, charAttrDefault CHAR(254), charWrapperArrayAttrDefault CLOB, characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.SerialDatatypeSupTE (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault CLOB, charAttrDefault CHAR(254), charWrapperArrayAttrDefault CLOB, characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedIntEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedIntWrapperEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedLongEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedLongWrapperEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedShortEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedShortWrapperEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.VersionedSqlTimestampEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version TIMESTAMP, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLDatatypeSupPropTestEntity (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault VARCHAR(255), charAttrDefault CHAR(254), charWrapperArrayAttrDefault VARCHAR(255), characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLDatatypeSupTestEntity (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault VARCHAR(255), charAttrDefault CHAR(254), charWrapperArrayAttrDefault VARCHAR(255), characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLEmbeddableIdEntity (country VARCHAR(255) NOT NULL, id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (country, id));
CREATE TABLE ${schemaname}.XMLEmbeddedObjectAOEntity (id INTEGER NOT NULL, localIntVal INTEGER, localStrVal VARCHAR(255), booleanVal SMALLINT, byteVal SMALLINT, charVal CHAR(254), doubleVal DOUBLE, floatVal REAL, intValCol INTEGER, longValCol BIGINT, shortVal SMALLINT, stringVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLEmbeddedObjectEntity (id INTEGER NOT NULL, localIntVal INTEGER, localStrVal VARCHAR(255), booleanVal SMALLINT, byteVal SMALLINT, charVal CHAR(254), doubleVal DOUBLE, floatVal REAL, intVal INTEGER, longVal BIGINT, shortVal SMALLINT, stringVal VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLEmbedMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLIdClassEntity (country VARCHAR(255) NOT NULL, id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (country, id));
CREATE TABLE ${schemaname}.XMLMSCMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLMultiTableEnt (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKEntityByte (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityByteWrapper (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityChar (pkey CHAR(254) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityCharacterWrapper (pkey CHAR(254) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityInt (pkey INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityIntWrapper (pkey INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityJavaSqlDate (pkey DATE NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityJavaUtilDate (pkey DATE NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityLong (pkey BIGINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityLongWrapper (pkey BIGINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityShort (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityShortWrapper (pkey SMALLINT NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKEntityString (pkey VARCHAR(255) NOT NULL, intVal INTEGER, PRIMARY KEY (pkey));
CREATE TABLE ${schemaname}.XMLPKGenAutoEntity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenIdentityEntity (id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenSequenceType1Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenSequenceType2Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenTableType1Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenTableType2Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenTableType3Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLPKGenTableType4Entity (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLReadOnlyEntity (id INTEGER NOT NULL, intVal INTEGER, noInsertIntVal INTEGER, noUpdatableIntVal INTEGER, readOnlyIntVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSerialDatatypeSupPropTE (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault VARCHAR(255), charAttrDefault CHAR(254), charWrapperArrayAttrDefault VARCHAR(255), characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSerialDatatypeSupTE (id INTEGER NOT NULL, bigDecimalAttrDefault DECIMAL(12,6), bigIntegerAttrDefault BIGINT, booleanAttrDefault SMALLINT, booleanWrapperAttrDefault SMALLINT, byteArrayAttrDefault BLOB, byteAttrDefault SMALLINT, byteWrapperArrayAttrDefault BLOB, byteWrapperAttrDefault SMALLINT, charArrayAttrDefault VARCHAR(255), charAttrDefault CHAR(254), charWrapperArrayAttrDefault VARCHAR(255), characterWrapperAttrDefault CHAR(254), doubleAttrDefault DOUBLE, doubleWrapperAttrDefault DOUBLE, enumeration SMALLINT, floatAttrDefault REAL, floatWrapperAttrDefault REAL, intAttrDefault INTEGER, integerWrapperAttrDefault INTEGER, longAttrDefault BIGINT, longWrapperAttrDefault BIGINT, serializableClass BLOB, shortAttrDefault SMALLINT, shortWrapperAttrDefault SMALLINT, sqlDateAttrDefault DATE, sqlTimeAttrDefault TIME, sqlTimestampAttrDefault TIMESTAMP, stringAttrDefault VARCHAR(255), utilCalendarAttrDefault DATE, utilDateAttrDefault DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedIntEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedIntWrapperEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedLongEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedLongWrapperEnt (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedShortEntity (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedShortWrapperEnt (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLVersionedSqlTimestampEnt (id INTEGER NOT NULL, intVal INTEGER, stringVal VARCHAR(255), version TIMESTAMP, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XSEC_TABLE1 (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));

              
CREATE INDEX I_SC_TBL1_ID ON ${schemaname}.SEC_TABLE1 (id);
CREATE INDEX I_SC_TMSC_ID ON ${schemaname}.SEC_TABLE2AMSC (id);
CREATE INDEX I_SC_TLMB_ID ON ${schemaname}.SEC_TABLEEMB (id);
CREATE INDEX I_XSC_BL1_ID ON ${schemaname}.XSEC_TABLE1 (id);

CREATE TABLE ${schemaname}.XSEC_TABLE2AMSC (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));
CREATE INDEX I_XSC_MSC_ID ON ${schemaname}.XSEC_TABLE2AMSC (id);
CREATE TABLE ${schemaname}.XSEC_TABLEEMB (id INTEGER, city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255));
CREATE INDEX I_XSC_LMB_ID ON ${schemaname}.XSEC_TABLEEMB (id);
