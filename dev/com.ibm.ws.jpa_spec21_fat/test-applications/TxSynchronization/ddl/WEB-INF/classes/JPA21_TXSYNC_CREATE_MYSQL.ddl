CREATE TABLE ${schemaname}.CMN10_SimpleVEnt (id INTEGER NOT NULL, byteArrData BLOB, charData INTEGER, doubleData DOUBLE, floatData REAL, intData INTEGER, longData BIGINT, strData VARCHAR(255), version INTEGER, PRIMARY KEY (id)) ENGINE = innodb;