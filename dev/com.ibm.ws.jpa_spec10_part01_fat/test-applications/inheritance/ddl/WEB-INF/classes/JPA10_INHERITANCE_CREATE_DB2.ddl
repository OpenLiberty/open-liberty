CREATE TABLE ${schemaname}.AnoConcreteLeaf1 (id INTEGER NOT NULL, name VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));  
CREATE TABLE ${schemaname}.AnoConcreteLeaf2 (id INTEGER NOT NULL, name VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoConcreteLeaf3 (id INTEGER NOT NULL, name VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));          
CREATE TABLE ${schemaname}.XMLConcreteLeaf1 (id INTEGER NOT NULL, name VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));  
CREATE TABLE ${schemaname}.XMLConcreteLeaf2 (id INTEGER NOT NULL, name VARCHAR(255), floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLConcreteLeaf3 (id INTEGER NOT NULL, name VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));          
                             
CREATE TABLE ${schemaname}.AnoJTCDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTCDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));            
CREATE TABLE ${schemaname}.XMLJTCDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTCDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
                        
CREATE INDEX I_JNCHRRT_DTYPE ON ${schemaname}.AnoJTCDRoot (DISC_COL);
CREATE INDEX I_XMLJRRT_DTYPE ON ${schemaname}.XMLJTCDRoot (DISC_COL);    
           
CREATE TABLE ${schemaname}.AnoJTIDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTIDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));            
CREATE TABLE ${schemaname}.XMLJTIDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTIDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
                        
CREATE INDEX I_JNINRRT_DTYPE ON ${schemaname}.AnoJTIDRoot (DISC_COL);
CREATE INDEX I_XMLINRT_DTYPE ON ${schemaname}.XMLJTIDRoot (DISC_COL);
           
CREATE TABLE ${schemaname}.AnoJTSDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoJTSDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));            
CREATE TABLE ${schemaname}.XMLJTSDRoot  (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf1 (id INTEGER NOT NULL, intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf2 (id INTEGER NOT NULL, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLJTSDLeaf3 (id INTEGER NOT NULL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), PRIMARY KEY (id));
                        
CREATE INDEX I_JNSTRRT_DTYPE ON ${schemaname}.AnoJTSDRoot (DISC_COL);
CREATE INDEX I_XMLSTRT_DTYPE ON ${schemaname}.XMLJTSDRoot (DISC_COL); 
                
CREATE TABLE ${schemaname}.AnoSTCDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoSTIDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL INTEGER, floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoSTSDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTCDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(255), stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, floatVal REAL, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTIDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL INTEGER, floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSTSDRoot (id INTEGER NOT NULL, name VARCHAR(255), DISC_COL VARCHAR(31), floatVal REAL, stringVal1 VARCHAR(255), stringVal2 VARCHAR(255), intVal INTEGER, PRIMARY KEY (id));
                 
CREATE INDEX I_SNGLTTY_DTYPE1 ON ${schemaname}.AnoSTCDRoot (DISC_COL);
CREATE INDEX I_SNGLTTY_DTYPE ON  ${schemaname}.AnoSTIDRoot (DISC_COL);
CREATE INDEX I_SNGLTTY_DTYPE2 ON ${schemaname}.AnoSTSDRoot (DISC_COL);
CREATE INDEX I_XMLSTTY_DTYPE1 ON ${schemaname}.XMLSTCDRoot (DISC_COL);
CREATE INDEX I_XMLSTTY_DTYPE ON  ${schemaname}.XMLSTIDRoot (DISC_COL);
CREATE INDEX I_XMLSTTY_DTYPE2 ON ${schemaname}.XMLSTSDRoot (DISC_COL);
                  
CREATE TABLE ${schemaname}.AnoAnoMSCEntity (id INTEGER NOT NULL, name VARCHAR(255), overridenNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.AnoXMLMSCEntity (id INTEGER NOT NULL, name VARCHAR(255), overridenNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLAnoMSCEntity (id INTEGER NOT NULL, name VARCHAR(255), originalNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLXMLMSCEntity (id INTEGER NOT NULL, name VARCHAR(255), originalNameAO VARCHAR(255), description VARCHAR(255), PRIMARY KEY (id));               
