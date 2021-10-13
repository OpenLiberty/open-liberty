CREATE TABLE Util1x1Lf (id INTEGER NOT NULL, firstName VARCHAR(255), version INTEGER, UNIRIGHT_ID INTEGER, UNIRIGHTLZY_ID INTEGER, PRIMARY KEY (id));
CREATE TABLE Util1x1Rt (id INTEGER NOT NULL, lastName VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE Util1xmLf (id INTEGER NOT NULL, firstName VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE Util1xmLf_Util1xmRt (UTIL1XMLF_ID INTEGER, UNIRIGHT_ID INTEGER, UNIRIGHTEGR_ID INTEGER);
CREATE TABLE Util1xmRt (id INTEGER NOT NULL, lastName VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE TABLE UtilEmbEntity (id INTEGER NOT NULL, name VARCHAR(255), version INTEGER, embName VARCHAR(255), embNotLoaded VARCHAR(255), emb1Name VARCHAR(255), emb1NotLoaded VARCHAR(255), embName2 VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE UtilEntity (id INTEGER NOT NULL, name VARCHAR(255), notLoaded VARCHAR(255), version INTEGER, PRIMARY KEY (id));
CREATE INDEX I_TL1X1LF_UNIRIGHT ON Util1x1Lf (UNIRIGHT_ID);
CREATE INDEX I_TL1X1LF_UNIRIGHTLZY ON Util1x1Lf (UNIRIGHTLZY_ID);
CREATE INDEX I_TL1XMRT_ELEMENT ON Util1xmLf_Util1xmRt (UNIRIGHT_ID);
CREATE INDEX I_TL1XMRT_ELEMENT1 ON Util1xmLf_Util1xmRt (UNIRIGHTEGR_ID);
CREATE INDEX I_TL1XMRT_UTIL1XMLF_ID ON Util1xmLf_Util1xmRt (UTIL1XMLF_ID);
