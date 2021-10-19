CREATE TABLE ${schemaname}.CMN10_Department (id NUMBER NOT NULL, departmentName VARCHAR2(255), description VARCHAR2(255), version NUMBER, DEPARTMENTMANAGER_ID NUMBER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_Employee (id NUMBER NOT NULL, birthdate DATE, dateOfHire DATE, firstName VARCHAR2(255), lastName VARCHAR2(255), position VARCHAR2(255), EMP_TYPE VARCHAR2(20), version NUMBER, DEPARTMENT_ID NUMBER, MANAGER_ID NUMBER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleEnt (id NUMBER NOT NULL, byteArrData BLOB, charData CHAR(1 CHAR), doubleData NUMBER, floatData REAL, intData NUMBER, longData NUMBER, strData VARCHAR2(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleVEnt (id NUMBER NOT NULL, byteArrData BLOB, charData CHAR(1 CHAR), doubleData NUMBER, floatData REAL, intData NUMBER, longData NUMBER, strData VARCHAR2(255), version NUMBER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleEntity10 (id NUMBER NOT NULL, byteArrData BLOB, charData CHAR(1 CHAR), doubleData NUMBER, floatData REAL, intData NUMBER, longData NUMBER, strData VARCHAR2(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleVersionedEntity10 (id NUMBER NOT NULL, byteArrData BLOB, charData CHAR(1 CHAR), doubleData NUMBER, floatData REAL, intData NUMBER, longData NUMBER, strData VARCHAR2(255), version NUMBER, PRIMARY KEY (id));

CREATE INDEX ${schemaname}.I_CMN1MNT_DEPARTMENTMANAGER ON ${schemaname}.CMN10_Department (DEPARTMENTMANAGER_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DEPARTMENT ON ${schemaname}.CMN10_Employee (DEPARTMENT_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DTYPE ON ${schemaname}.CMN10_Employee (EMP_TYPE);
CREATE INDEX ${schemaname}.I_CMN1PLY_MANAGER ON ${schemaname}.CMN10_Employee (MANAGER_ID);     
              