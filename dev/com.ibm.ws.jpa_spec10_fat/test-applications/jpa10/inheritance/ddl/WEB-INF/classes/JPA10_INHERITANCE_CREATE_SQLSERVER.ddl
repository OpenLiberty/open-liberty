CREATE TABLE ${schemaname}.AnoAnoMSCEntity (id INT NOT NULL, name VARCHAR(255), overridenNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoConcreteLeaf1 (id INT NOT NULL, name VARCHAR(255), intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoConcreteLeaf2 (id INT NOT NULL, name VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoConcreteLeaf3 (id INT NOT NULL, name VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL CHAR(1), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoSTCDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL CHAR(1), intVal INT, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoSTIDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL INT, intVal INT, floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoSTSDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), intVal INT, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoXMLMSCEntity (id INT NOT NULL, name VARCHAR(255), overridenNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLAnoMSCEntity (id INT NOT NULL, name VARCHAR(255), originalNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLConcreteLeaf1 (id INT NOT NULL, name VARCHAR(255), intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLConcreteLeaf2 (id INT NOT NULL, name VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLConcreteLeaf3 (id INT NOT NULL, name VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL CHAR(1), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf1 (id INT NOT NULL, intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf2 (id INT NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf3 (id INT NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTCDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL CHAR(1), floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INT, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTIDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL INT, intVal INT, floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTSDRoot (id INT NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), floatVal REAL, intVal INT, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLXMLMSCEntity (id INT NOT NULL, name VARCHAR(255), originalNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE INDEX ${schemaname}.I_NJTCDRT_DTYPE ON ${schemaname}.AnoJTCDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_NJTDROT_DTYPE ON ${schemaname}.AnoJTIDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_NJTSDRT_DTYPE ON ${schemaname}.AnoJTSDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_NSTCDRT_DTYPE ON ${schemaname}.AnoSTCDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_NSTDROT_DTYPE ON ${schemaname}.AnoSTIDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_NSTSDRT_DTYPE ON ${schemaname}.AnoSTSDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLJDRT_DTYPE2 ON ${schemaname}.XMLJTCDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLJDRT_DTYPE1 ON ${schemaname}.XMLJTIDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLJDRT_DTYPE ON ${schemaname}.XMLJTSDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLSDRT_DTYPE2 ON ${schemaname}.XMLSTCDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLSDRT_DTYPE1 ON ${schemaname}.XMLSTIDRoot (DISC_COL);
CREATE INDEX ${schemaname}.I_XMLSDRT_DTYPE ON ${schemaname}.XMLSTSDRoot (DISC_COL);
     