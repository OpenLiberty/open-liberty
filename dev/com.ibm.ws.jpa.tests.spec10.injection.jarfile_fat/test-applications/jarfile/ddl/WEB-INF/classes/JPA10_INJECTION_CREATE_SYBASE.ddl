CREATE TABLE ${schemaname}.CMN10_Department (id INT NOT NULL, departmentName VARCHAR(255), description VARCHAR(255), version INT, DEPARTMENTMANAGER_ID INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_Employee (id INT NOT NULL, birthdate DATETIME, dateOfHire DATETIME, firstName VARCHAR(255), lastName VARCHAR(255), position VARCHAR(255), EMP_TYPE VARCHAR(20), version INT, DEPARTMENT_ID INT, MANAGER_ID INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleEnt (id INT NOT NULL, byteArrData IMAGE, charData CHAR(1), doubleData FLOAT(32), floatData REAL, intData INT, longData BIGINT, strData VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.CMN10_SimpleVEnt (id INT NOT NULL, byteArrData IMAGE, charData CHAR(1), doubleData FLOAT(32), floatData REAL, intData INT, longData BIGINT, strData VARCHAR(255), version INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleEntity10 (id INT NOT NULL, byteArrData IMAGE, charData CHAR(1), doubleData FLOAT(32), floatData REAL, intData INT, longData BIGINT, strData VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSimpleVersionedEntity10 (id INT NOT NULL, byteArrData IMAGE, charData CHAR(1), doubleData FLOAT(32), floatData REAL, intData INT, longData BIGINT, strData VARCHAR(255), version INT, PRIMARY KEY (id));

CREATE INDEX ${schemaname}.I_CMN1MNT_DEPARTMENTMANAGER ON ${schemaname}.CMN10_Department (DEPARTMENTMANAGER_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DEPARTMENT ON ${schemaname}.CMN10_Employee (DEPARTMENT_ID);
CREATE INDEX ${schemaname}.I_CMN1PLY_DTYPE ON ${schemaname}.CMN10_Employee (EMP_TYPE);
CREATE INDEX ${schemaname}.I_CMN1PLY_MANAGER ON ${schemaname}.CMN10_Employee (MANAGER_ID);  
              