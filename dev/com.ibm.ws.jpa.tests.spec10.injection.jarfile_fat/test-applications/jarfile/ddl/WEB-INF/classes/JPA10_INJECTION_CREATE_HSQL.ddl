CREATE TABLE ${schemaname}.CMN10_Department (id INTEGER NOT NULL, departmentName VARCHAR(255), description VARCHAR(255), version INTEGER, DEPARTMENTMANAGER_ID INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_Employee (id INTEGER NOT NULL, birthdate DATE, dateOfHire DATE, firstName VARCHAR(255), lastName VARCHAR(255), position VARCHAR(255), EMP_TYPE VARCHAR(20), version INTEGER, DEPARTMENT_ID INTEGER, MANAGER_ID INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleEnt (id INTEGER NOT NULL, byteArrData VARBINARY, charData INTEGER, doubleData NUMERIC, floatData REAL, intData INTEGER, longData BIGINT, strData VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleVEnt (id INTEGER NOT NULL, byteArrData VARBINARY, charData INTEGER, doubleData NUMERIC, floatData REAL, intData INTEGER, longData BIGINT, strData VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleEntity10 (id INTEGER NOT NULL, byteArrData VARBINARY, charData INTEGER, doubleData NUMERIC, floatData REAL, intData INTEGER, longData BIGINT, strData VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleVersionedEntity10 (id INTEGER NOT NULL, byteArrData VARBINARY, charData INTEGER, doubleData NUMERIC, floatData REAL, intData INTEGER, longData BIGINT, strData VARCHAR(255), version INTEGER, PRIMARY KEY (id));
              
CREATE INDEX ${schemaname}.I_CMN1MNT_DEPARTMENTMANAGER ON ${schemaname}.CMN10_Department (DEPARTMENTMANAGER_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DEPARTMENT ON ${schemaname}.CMN10_Employee (DEPARTMENT_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DTYPE ON ${schemaname}.CMN10_Employee (EMP_TYPE);
CREATE INDEX ${schemaname}.I_CMN1PLY_MANAGER ON ${schemaname}.CMN10_Employee (MANAGER_ID);
              
              