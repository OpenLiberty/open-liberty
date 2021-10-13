<sql>CREATE TABLE ${schemaname}.UtilEntity (id INTEGER NOT NULL, version INTEGER, name VARCHAR(25), notLoaded VARCHAR(25), PRIMARY KEY (id));
<sql>CREATE TABLE ${schemaname}.UtilEmbEntity (id INTEGER NOT NULL, name VARCHAR(25), version INTEGER, embName VARCHAR(25), embNotLoaded VARCHAR(25), emb1Name VARCHAR(25), emb1NotLoaded VARCHAR(25), embName2 VARCHAR(25), PRIMARY KEY (id));
<sql>CREATE TABLE ${schemaname}.Util1x1Rt (id INTEGER NOT NULL, lastName VARCHAR(25), version INTEGER, PRIMARY KEY (id));
<sql>CREATE TABLE ${schemaname}.Util1x1Lf (id INTEGER NOT NULL, firstName VARCHAR(25), version INTEGER, UNIRIGHT_ID INTEGER, UNIRIGHTLZY_ID INTEGER, PRIMARY KEY (id));

<sql>CREATE INDEX ${schemaname}.I_TL1X1LF_UNIRIGH1 ON Util1x1Lf (UNIRIGHTLZY_ID);
<sql>CREATE INDEX ${schemaname}.I_TL1X1LF_UNIRIGHT ON Util1x1Lf (UNIRIGHT_ID);

<sql>CREATE TABLE ${schemaname}.Util1xmRt (id INTEGER NOT NULL, lastName VARCHAR(25), version INTEGER, PRIMARY KEY (id));
<sql>CREATE TABLE ${schemaname}.Util1xmLf (id INTEGER NOT NULL, firstName VARCHAR(25), version INTEGER, PRIMARY KEY (id));
<sql>CREATE TABLE ${schemaname}.Util1xmLf_Util1xmRt (UTIL1XMLF_ID INTEGER, UNIRIGHT_ID INTEGER, UNIRIGHTEGR_ID INTEGER);

<sql>CREATE INDEX ${schemaname}.I_TL1XMRT_ELEMENT ON Util1xmLf_Util1xmRt (UNIRIGHT_ID);
<sql>CREATE INDEX ${schemaname}.I_TL1XMRT_ELEMENT1 ON Util1xmLf_Util1xmRt (UNIRIGHTEGR_ID);
<sql>CREATE INDEX ${schemaname}.I_TL1XMRT_UTIL1XML ON Util1xmLf_Util1xmRt (UTIL1XMLF_ID);

