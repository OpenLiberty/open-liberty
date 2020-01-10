--EclipseLink
CREATE TABLE ${schemaname}.CDM_Entity0001 (ENTITY0001_ID NUMBER(3) NOT NULL, ENTITY0001_STRING01 VARCHAR2(255) NULL, ENTITY0001_STRING02 VARCHAR2(255) NULL, ENTITY0001_STRING03 VARCHAR2(255) NULL, ENTITY0001_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0001_ID));
CREATE TABLE ${schemaname}.CDM_Entity0002 (ENTITY0002_ID NUMBER(3) NOT NULL, ENTITY0002_STRING01 VARCHAR2(255) NULL, ENTITY0002_STRING02 VARCHAR2(255) NULL, ENTITY0002_STRING03 VARCHAR2(255) NULL, ENTITY0002_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0002_ID));
CREATE TABLE ${schemaname}.CDM_Entity0003 (ENTITY0003_ID CHAR(1) NOT NULL, ENTITY0003_STRING01 VARCHAR2(255) NULL, ENTITY0003_STRING02 VARCHAR2(255) NULL, ENTITY0003_STRING03 VARCHAR2(255) NULL, ENTITY0003_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0003_ID));
CREATE TABLE ${schemaname}.CDM_Entity0004 (ENTITY0004_ID CHAR(1) NOT NULL, ENTITY0004_STRING01 VARCHAR2(255) NULL, ENTITY0004_STRING02 VARCHAR2(255) NULL, ENTITY0004_STRING03 VARCHAR2(255) NULL, ENTITY0004_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0004_ID));
CREATE TABLE ${schemaname}.CDM_Entity0005 (ENTITY0005_ID VARCHAR2(255) NOT NULL, ENTITY0005_STRING01 VARCHAR2(255) NULL, ENTITY0005_STRING02 VARCHAR2(255) NULL, ENTITY0005_STRING03 VARCHAR2(255) NULL, ENTITY0005_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0005_ID));
CREATE TABLE ${schemaname}.CDM_Entity0006 (ENTITY0006_ID NUMBER(19,4) NOT NULL, ENTITY0006_STRING01 VARCHAR2(255) NULL, ENTITY0006_STRING02 VARCHAR2(255) NULL, ENTITY0006_STRING03 VARCHAR2(255) NULL, ENTITY0006_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0006_ID));
CREATE TABLE ${schemaname}.CDM_Entity0007 (ENTITY0007_ID NUMBER(19,4) NOT NULL, ENTITY0007_STRING01 VARCHAR2(255) NULL, ENTITY0007_STRING02 VARCHAR2(255) NULL, ENTITY0007_STRING03 VARCHAR2(255) NULL, ENTITY0007_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0007_ID));
CREATE TABLE ${schemaname}.CDM_Entity0008 (ENTITY0008_ID NUMBER(19,4) NOT NULL, ENTITY0008_STRING01 VARCHAR2(255) NULL, ENTITY0008_STRING02 VARCHAR2(255) NULL, ENTITY0008_STRING03 VARCHAR2(255) NULL, ENTITY0008_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0008_ID));
CREATE TABLE ${schemaname}.CDM_Entity0009 (ENTITY0009_ID NUMBER(19,4) NOT NULL, ENTITY0009_STRING01 VARCHAR2(255) NULL, ENTITY0009_STRING02 VARCHAR2(255) NULL, ENTITY0009_STRING03 VARCHAR2(255) NULL, ENTITY0009_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0009_ID));
CREATE TABLE ${schemaname}.CDM_Entity0010 (ENTITY0010_ID NUMBER(10) NOT NULL, ENTITY0010_STRING01 VARCHAR2(255) NULL, ENTITY0010_STRING02 VARCHAR2(255) NULL, ENTITY0010_STRING03 VARCHAR2(255) NULL, ENTITY0010_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0010_ID));
CREATE TABLE ${schemaname}.CDM_Entity0011 (ENTITY0011_ID NUMBER(10) NOT NULL, ENTITY0011_STRING01 VARCHAR2(255) NULL, ENTITY0011_STRING02 VARCHAR2(255) NULL, ENTITY0011_STRING03 VARCHAR2(255) NULL, ENTITY0011_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0011_ID));
CREATE TABLE ${schemaname}.CDM_Entity0012 (ENTITY0012_ID NUMBER(19) NOT NULL, ENTITY0012_STRING01 VARCHAR2(255) NULL, ENTITY0012_STRING02 VARCHAR2(255) NULL, ENTITY0012_STRING03 VARCHAR2(255) NULL, ENTITY0012_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0012_ID));
CREATE TABLE ${schemaname}.CDM_Entity0013 (ENTITY0013_ID NUMBER(19) NOT NULL, ENTITY0013_STRING01 VARCHAR2(255) NULL, ENTITY0013_STRING02 VARCHAR2(255) NULL, ENTITY0013_STRING03 VARCHAR2(255) NULL, ENTITY0013_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0013_ID));
CREATE TABLE ${schemaname}.CDM_Entity0014 (ENTITY0014_ID NUMBER(5) NOT NULL, ENTITY0014_STRING01 VARCHAR2(255) NULL, ENTITY0014_STRING02 VARCHAR2(255) NULL, ENTITY0014_STRING03 VARCHAR2(255) NULL, ENTITY0014_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0014_ID));
CREATE TABLE ${schemaname}.CDM_Entity0015 (ENTITY0015_ID NUMBER(5) NOT NULL, ENTITY0015_STRING01 VARCHAR2(255) NULL, ENTITY0015_STRING02 VARCHAR2(255) NULL, ENTITY0015_STRING03 VARCHAR2(255) NULL, ENTITY0015_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0015_ID));
CREATE TABLE ${schemaname}.CDM_Entity0016 (ENTITY0016_ID NUMBER(38) NOT NULL, ENTITY0016_STRING01 VARCHAR2(255) NULL, ENTITY0016_STRING02 VARCHAR2(255) NULL, ENTITY0016_STRING03 VARCHAR2(255) NULL, ENTITY0016_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0016_ID));
CREATE TABLE ${schemaname}.CDM_Entity0017 (ENTITY0017_ID NUMBER(38) NOT NULL, ENTITY0017_STRING01 VARCHAR2(255) NULL, ENTITY0017_STRING02 VARCHAR2(255) NULL, ENTITY0017_STRING03 VARCHAR2(255) NULL, ENTITY0017_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0017_ID));
CREATE TABLE ${schemaname}.CDM_Entity0018 (ENTITY0018_ID DATE NOT NULL, ENTITY0018_STRING01 VARCHAR2(255) NULL, ENTITY0018_STRING02 VARCHAR2(255) NULL, ENTITY0018_STRING03 VARCHAR2(255) NULL, ENTITY0018_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0018_ID));
CREATE TABLE ${schemaname}.CDM_Entity0019 (ENTITY0019_ID DATE NOT NULL, ENTITY0019_STRING01 VARCHAR2(255) NULL, ENTITY0019_STRING02 VARCHAR2(255) NULL, ENTITY0019_STRING03 VARCHAR2(255) NULL, ENTITY0019_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0019_ID));
CREATE TABLE ${schemaname}.CDM_Entity0101 (ENTITY0101_ID1 NUMBER(3) NOT NULL, ENTITY0101_ID2 NUMBER(3) NOT NULL, ENTITY0101_STRING01 VARCHAR2(255) NULL, ENTITY0101_STRING02 VARCHAR2(255) NULL, ENTITY0101_STRING03 VARCHAR2(255) NULL, ENTITY0101_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0101_ID1, ENTITY0101_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0102 (ENTITY0102_ID1 NUMBER(3) NOT NULL, ENTITY0102_ID2 NUMBER(3) NOT NULL, ENTITY0102_STRING01 VARCHAR2(255) NULL, ENTITY0102_STRING02 VARCHAR2(255) NULL, ENTITY0102_STRING03 VARCHAR2(255) NULL, ENTITY0102_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0102_ID1, ENTITY0102_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0103 (ENTITY0103_ID2 CHAR(1) NOT NULL, ENTITY0103_ID1 CHAR(1) NOT NULL, ENTITY0103_STRING01 VARCHAR2(255) NULL, ENTITY0103_STRING02 VARCHAR2(255) NULL, ENTITY0103_STRING03 VARCHAR2(255) NULL, ENTITY0103_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0103_ID2, ENTITY0103_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0104 (ENTITY0104_ID2 CHAR(1) NOT NULL, ENTITY0104_ID1 CHAR(1) NOT NULL, ENTITY0104_STRING01 VARCHAR2(255) NULL, ENTITY0104_STRING02 VARCHAR2(255) NULL, ENTITY0104_STRING03 VARCHAR2(255) NULL, ENTITY0104_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0104_ID2, ENTITY0104_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0105 (ENTITY0105_ID2 VARCHAR2(255) NOT NULL, ENTITY0105_ID1 VARCHAR2(255) NOT NULL, ENTITY0105_STRING01 VARCHAR2(255) NULL, ENTITY0105_STRING02 VARCHAR2(255) NULL, ENTITY0105_STRING03 VARCHAR2(255) NULL, ENTITY0105_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0105_ID2, ENTITY0105_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0106 (ENTITY0106_ID1 NUMBER(19,4) NOT NULL, ENTITY0106_ID2 NUMBER(19,4) NOT NULL, ENTITY0106_STRING01 VARCHAR2(255) NULL, ENTITY0106_STRING02 VARCHAR2(255) NULL, ENTITY0106_STRING03 VARCHAR2(255) NULL, ENTITY0106_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0106_ID1, ENTITY0106_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0107 (ENTITY0107_ID2 NUMBER(19,4) NOT NULL, ENTITY0107_ID1 NUMBER(19,4) NOT NULL, ENTITY0107_STRING01 VARCHAR2(255) NULL, ENTITY0107_STRING02 VARCHAR2(255) NULL, ENTITY0107_STRING03 VARCHAR2(255) NULL, ENTITY0107_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0107_ID2, ENTITY0107_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0108 (ENTITY0108_ID2 NUMBER(19,4) NOT NULL, ENTITY0108_ID1 NUMBER(19,4) NOT NULL, ENTITY0108_STRING01 VARCHAR2(255) NULL, ENTITY0108_STRING02 VARCHAR2(255) NULL, ENTITY0108_STRING03 VARCHAR2(255) NULL, ENTITY0108_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0108_ID2, ENTITY0108_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0109 (ENTITY0109_ID2 NUMBER(19,4) NOT NULL, ENTITY0109_ID1 NUMBER(19,4) NOT NULL, ENTITY0109_STRING01 VARCHAR2(255) NULL, ENTITY0109_STRING02 VARCHAR2(255) NULL, ENTITY0109_STRING03 VARCHAR2(255) NULL, ENTITY0109_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0109_ID2, ENTITY0109_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0110 (ENTITY0110_ID2 NUMBER(10) NOT NULL, ENTITY0110_ID1 NUMBER(10) NOT NULL, ENTITY0110_STRING01 VARCHAR2(255) NULL, ENTITY0110_STRING02 VARCHAR2(255) NULL, ENTITY0110_STRING03 VARCHAR2(255) NULL, ENTITY0110_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0110_ID2, ENTITY0110_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0111 (ENTITY0111_ID2 NUMBER(10) NOT NULL, ENTITY0111_ID1 NUMBER(10) NOT NULL, ENTITY0111_STRING01 VARCHAR2(255) NULL, ENTITY0111_STRING02 VARCHAR2(255) NULL, ENTITY0111_STRING03 VARCHAR2(255) NULL, ENTITY0111_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0111_ID2, ENTITY0111_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0112 (ENTITY0112_ID2 NUMBER(19) NOT NULL, ENTITY0112_ID1 NUMBER(19) NOT NULL, ENTITY0112_STRING01 VARCHAR2(255) NULL, ENTITY0112_STRING02 VARCHAR2(255) NULL, ENTITY0112_STRING03 VARCHAR2(255) NULL, ENTITY0112_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0112_ID2, ENTITY0112_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0113 (ENTITY0113_ID2 NUMBER(19) NOT NULL, ENTITY0113_ID1 NUMBER(19) NOT NULL, ENTITY0113_STRING01 VARCHAR2(255) NULL, ENTITY0113_STRING02 VARCHAR2(255) NULL, ENTITY0113_STRING03 VARCHAR2(255) NULL, ENTITY0113_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0113_ID2, ENTITY0113_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0114 (ENTITY0114_ID1 NUMBER(5) NOT NULL, ENTITY0114_ID2 NUMBER(5) NOT NULL, ENTITY0114_STRING01 VARCHAR2(255) NULL, ENTITY0114_STRING02 VARCHAR2(255) NULL, ENTITY0114_STRING03 VARCHAR2(255) NULL, ENTITY0114_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0114_ID1, ENTITY0114_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0115 (ENTITY0115_ID1 NUMBER(5) NOT NULL, ENTITY0115_ID2 NUMBER(5) NOT NULL, ENTITY0115_STRING01 VARCHAR2(255) NULL, ENTITY0115_STRING02 VARCHAR2(255) NULL, ENTITY0115_STRING03 VARCHAR2(255) NULL, ENTITY0115_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0115_ID1, ENTITY0115_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0116 (ENTITY0116_ID1 NUMBER(38) NOT NULL, ENTITY0116_ID2 NUMBER(38) NOT NULL, ENTITY0116_STRING01 VARCHAR2(255) NULL, ENTITY0116_STRING02 VARCHAR2(255) NULL, ENTITY0116_STRING03 VARCHAR2(255) NULL, ENTITY0116_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0116_ID1, ENTITY0116_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0117 (ENTITY0117_ID1 NUMBER(38) NOT NULL, ENTITY0117_ID2 NUMBER(38) NOT NULL, ENTITY0117_STRING01 VARCHAR2(255) NULL, ENTITY0117_STRING02 VARCHAR2(255) NULL, ENTITY0117_STRING03 VARCHAR2(255) NULL, ENTITY0117_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0117_ID1, ENTITY0117_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0118 (ENTITY0118_ID1 DATE NOT NULL, ENTITY0118_ID2 DATE NOT NULL, ENTITY0118_STRING01 VARCHAR2(255) NULL, ENTITY0118_STRING02 VARCHAR2(255) NULL, ENTITY0118_STRING03 VARCHAR2(255) NULL, ENTITY0118_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0118_ID1, ENTITY0118_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0119 (ENTITY0119_ID1 DATE NOT NULL, ENTITY0119_ID2 DATE NOT NULL, ENTITY0119_STRING01 VARCHAR2(255) NULL, ENTITY0119_STRING02 VARCHAR2(255) NULL, ENTITY0119_STRING03 VARCHAR2(255) NULL, ENTITY0119_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0119_ID1, ENTITY0119_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0201 (ENTITY0201_ID1 NUMBER(3) NOT NULL, ENTITY0201_ID3 NUMBER(3) NOT NULL, ENTITY0201_ID2 NUMBER(3) NOT NULL, ENTITY0201_STRING01 VARCHAR2(255) NULL, ENTITY0201_STRING02 VARCHAR2(255) NULL, ENTITY0201_STRING03 VARCHAR2(255) NULL, ENTITY0201_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0201_ID1, ENTITY0201_ID3, ENTITY0201_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0202 (ENTITY0202_ID2 NUMBER(3) NOT NULL, ENTITY0202_ID1 NUMBER(3) NOT NULL, ENTITY0202_ID3 NUMBER(3) NOT NULL, ENTITY0202_STRING01 VARCHAR2(255) NULL, ENTITY0202_STRING02 VARCHAR2(255) NULL, ENTITY0202_STRING03 VARCHAR2(255) NULL, ENTITY0202_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0202_ID2, ENTITY0202_ID1, ENTITY0202_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0203 (ENTITY0203_ID1 CHAR(1) NOT NULL, ENTITY0203_ID3 CHAR(1) NOT NULL, ENTITY0203_ID2 CHAR(1) NOT NULL, ENTITY0203_STRING01 VARCHAR2(255) NULL, ENTITY0203_STRING02 VARCHAR2(255) NULL, ENTITY0203_STRING03 VARCHAR2(255) NULL, ENTITY0203_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0203_ID1, ENTITY0203_ID3, ENTITY0203_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0204 (ENTITY0204_ID3 CHAR(1) NOT NULL, ENTITY0204_ID2 CHAR(1) NOT NULL, ENTITY0204_ID1 CHAR(1) NOT NULL, ENTITY0204_STRING01 VARCHAR2(255) NULL, ENTITY0204_STRING02 VARCHAR2(255) NULL, ENTITY0204_STRING03 VARCHAR2(255) NULL, ENTITY0204_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0204_ID3, ENTITY0204_ID2, ENTITY0204_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0205 (ENTITY0205_ID1 VARCHAR2(255) NOT NULL, ENTITY0205_ID3 VARCHAR2(255) NOT NULL, ENTITY0205_ID2 VARCHAR2(255) NOT NULL, ENTITY0205_STRING01 VARCHAR2(255) NULL, ENTITY0205_STRING02 VARCHAR2(255) NULL, ENTITY0205_STRING03 VARCHAR2(255) NULL, ENTITY0205_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0205_ID1, ENTITY0205_ID3, ENTITY0205_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0206 (ENTITY0206_ID3 NUMBER(19,4) NOT NULL, ENTITY0206_ID2 NUMBER(19,4) NOT NULL, ENTITY0206_ID1 NUMBER(19,4) NOT NULL, ENTITY0206_STRING01 VARCHAR2(255) NULL, ENTITY0206_STRING02 VARCHAR2(255) NULL, ENTITY0206_STRING03 VARCHAR2(255) NULL, ENTITY0206_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0206_ID3, ENTITY0206_ID2, ENTITY0206_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0207 (ENTITY0207_ID3 NUMBER(19,4) NOT NULL, ENTITY0207_ID2 NUMBER(19,4) NOT NULL, ENTITY0207_ID1 NUMBER(19,4) NOT NULL, ENTITY0207_STRING01 VARCHAR2(255) NULL, ENTITY0207_STRING02 VARCHAR2(255) NULL, ENTITY0207_STRING03 VARCHAR2(255) NULL, ENTITY0207_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0207_ID3, ENTITY0207_ID2, ENTITY0207_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0208 (ENTITY0208_ID3 NUMBER(19,4) NOT NULL, ENTITY0208_ID2 NUMBER(19,4) NOT NULL, ENTITY0208_ID1 NUMBER(19,4) NOT NULL, ENTITY0208_STRING01 VARCHAR2(255) NULL, ENTITY0208_STRING02 VARCHAR2(255) NULL, ENTITY0208_STRING03 VARCHAR2(255) NULL, ENTITY0208_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0208_ID3, ENTITY0208_ID2, ENTITY0208_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0209 (ENTITY0209_ID1 NUMBER(19,4) NOT NULL, ENTITY0209_ID3 NUMBER(19,4) NOT NULL, ENTITY0209_ID2 NUMBER(19,4) NOT NULL, ENTITY0209_STRING01 VARCHAR2(255) NULL, ENTITY0209_STRING02 VARCHAR2(255) NULL, ENTITY0209_STRING03 VARCHAR2(255) NULL, ENTITY0209_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0209_ID1, ENTITY0209_ID3, ENTITY0209_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0210 (ENTITY0210_ID3 NUMBER(10) NOT NULL, ENTITY0210_ID2 NUMBER(10) NOT NULL, ENTITY0210_ID1 NUMBER(10) NOT NULL, ENTITY0210_STRING01 VARCHAR2(255) NULL, ENTITY0210_STRING02 VARCHAR2(255) NULL, ENTITY0210_STRING03 VARCHAR2(255) NULL, ENTITY0210_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0210_ID3, ENTITY0210_ID2, ENTITY0210_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0211 (ENTITY0211_ID3 NUMBER(10) NOT NULL, ENTITY0211_ID1 NUMBER(10) NOT NULL, ENTITY0211_ID2 NUMBER(10) NOT NULL, ENTITY0211_STRING01 VARCHAR2(255) NULL, ENTITY0211_STRING02 VARCHAR2(255) NULL, ENTITY0211_STRING03 VARCHAR2(255) NULL, ENTITY0211_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0211_ID3, ENTITY0211_ID1, ENTITY0211_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0212 (ENTITY0212_ID1 NUMBER(19) NOT NULL, ENTITY0212_ID2 NUMBER(19) NOT NULL, ENTITY0212_ID3 NUMBER(19) NOT NULL, ENTITY0212_STRING01 VARCHAR2(255) NULL, ENTITY0212_STRING02 VARCHAR2(255) NULL, ENTITY0212_STRING03 VARCHAR2(255) NULL, ENTITY0212_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0212_ID1, ENTITY0212_ID2, ENTITY0212_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0213 (ENTITY0213_ID3 NUMBER(19) NOT NULL, ENTITY0213_ID1 NUMBER(19) NOT NULL, ENTITY0213_ID2 NUMBER(19) NOT NULL, ENTITY0213_STRING01 VARCHAR2(255) NULL, ENTITY0213_STRING02 VARCHAR2(255) NULL, ENTITY0213_STRING03 VARCHAR2(255) NULL, ENTITY0213_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0213_ID3, ENTITY0213_ID1, ENTITY0213_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0214 (ENTITY0214_ID2 NUMBER(5) NOT NULL, ENTITY0214_ID3 NUMBER(5) NOT NULL, ENTITY0214_ID1 NUMBER(5) NOT NULL, ENTITY0214_STRING01 VARCHAR2(255) NULL, ENTITY0214_STRING02 VARCHAR2(255) NULL, ENTITY0214_STRING03 VARCHAR2(255) NULL, ENTITY0214_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0214_ID2, ENTITY0214_ID3, ENTITY0214_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0215 (ENTITY0215_ID3 NUMBER(5) NOT NULL, ENTITY0215_ID1 NUMBER(5) NOT NULL, ENTITY0215_ID2 NUMBER(5) NOT NULL, ENTITY0215_STRING01 VARCHAR2(255) NULL, ENTITY0215_STRING02 VARCHAR2(255) NULL, ENTITY0215_STRING03 VARCHAR2(255) NULL, ENTITY0215_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0215_ID3, ENTITY0215_ID1, ENTITY0215_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0216 (ENTITY0216_ID1 NUMBER(38) NOT NULL, ENTITY0216_ID2 NUMBER(38) NOT NULL, ENTITY0216_ID3 NUMBER(38) NOT NULL, ENTITY0216_STRING01 VARCHAR2(255) NULL, ENTITY0216_STRING02 VARCHAR2(255) NULL, ENTITY0216_STRING03 VARCHAR2(255) NULL, ENTITY0216_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0216_ID1, ENTITY0216_ID2, ENTITY0216_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0217 (ENTITY0217_ID1 NUMBER(38) NOT NULL, ENTITY0217_ID2 NUMBER(38) NOT NULL, ENTITY0217_ID3 NUMBER(38) NOT NULL, ENTITY0217_STRING01 VARCHAR2(255) NULL, ENTITY0217_STRING02 VARCHAR2(255) NULL, ENTITY0217_STRING03 VARCHAR2(255) NULL, ENTITY0217_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0217_ID1, ENTITY0217_ID2, ENTITY0217_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0218 (ENTITY0218_ID1 DATE NOT NULL, ENTITY0218_ID2 DATE NOT NULL, ENTITY0218_ID3 DATE NOT NULL, ENTITY0218_STRING01 VARCHAR2(255) NULL, ENTITY0218_STRING02 VARCHAR2(255) NULL, ENTITY0218_STRING03 VARCHAR2(255) NULL, ENTITY0218_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0218_ID1, ENTITY0218_ID2, ENTITY0218_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0219 (ENTITY0219_ID3 DATE NOT NULL, ENTITY0219_ID1 DATE NOT NULL, ENTITY0219_ID2 DATE NOT NULL, ENTITY0219_STRING01 VARCHAR2(255) NULL, ENTITY0219_STRING02 VARCHAR2(255) NULL, ENTITY0219_STRING03 VARCHAR2(255) NULL, ENTITY0219_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0219_ID3, ENTITY0219_ID1, ENTITY0219_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0301 (ENTITY0301_ID3 CHAR(1) NOT NULL, ENTITY0301_ID2 NUMBER(3) NOT NULL, ENTITY0301_ID1 NUMBER(3) NOT NULL, ENTITY0301_STRING01 VARCHAR2(255) NULL, ENTITY0301_STRING02 VARCHAR2(255) NULL, ENTITY0301_STRING03 VARCHAR2(255) NULL, ENTITY0301_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0301_ID3, ENTITY0301_ID2, ENTITY0301_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0302 (ENTITY0302_ID3 CHAR(1) NOT NULL, ENTITY0302_ID2 CHAR(1) NOT NULL, ENTITY0302_ID1 NUMBER(3) NOT NULL, ENTITY0302_STRING01 VARCHAR2(255) NULL, ENTITY0302_STRING02 VARCHAR2(255) NULL, ENTITY0302_STRING03 VARCHAR2(255) NULL, ENTITY0302_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0302_ID3, ENTITY0302_ID2, ENTITY0302_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0303 (ENTITY0303_ID3 VARCHAR2(255) NOT NULL, ENTITY0303_ID2 CHAR(1) NOT NULL, ENTITY0303_ID1 CHAR(1) NOT NULL, ENTITY0303_STRING01 VARCHAR2(255) NULL, ENTITY0303_STRING02 VARCHAR2(255) NULL, ENTITY0303_STRING03 VARCHAR2(255) NULL, ENTITY0303_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0303_ID3, ENTITY0303_ID2, ENTITY0303_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0304 (ENTITY0304_ID1 CHAR(1) NOT NULL, ENTITY0304_ID3 NUMBER(19,4) NOT NULL, ENTITY0304_ID2 VARCHAR2(255) NOT NULL, ENTITY0304_STRING01 VARCHAR2(255) NULL, ENTITY0304_STRING02 VARCHAR2(255) NULL, ENTITY0304_STRING03 VARCHAR2(255) NULL, ENTITY0304_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0304_ID1, ENTITY0304_ID3, ENTITY0304_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0305 (ENTITY0305_ID3 NUMBER(19,4) NOT NULL, ENTITY0305_ID2 NUMBER(19,4) NOT NULL, ENTITY0305_ID1 VARCHAR2(255) NOT NULL, ENTITY0305_STRING01 VARCHAR2(255) NULL, ENTITY0305_STRING02 VARCHAR2(255) NULL, ENTITY0305_STRING03 VARCHAR2(255) NULL, ENTITY0305_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0305_ID3, ENTITY0305_ID2, ENTITY0305_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0306 (ENTITY0306_ID3 NUMBER(19,4) NOT NULL, ENTITY0306_ID2 NUMBER(19,4) NOT NULL, ENTITY0306_ID1 NUMBER(19,4) NOT NULL, ENTITY0306_STRING01 VARCHAR2(255) NULL, ENTITY0306_STRING02 VARCHAR2(255) NULL, ENTITY0306_STRING03 VARCHAR2(255) NULL, ENTITY0306_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0306_ID3, ENTITY0306_ID2, ENTITY0306_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0307 (ENTITY0307_ID3 NUMBER(19,4) NOT NULL, ENTITY0307_ID2 NUMBER(19,4) NOT NULL, ENTITY0307_ID1 NUMBER(19,4) NOT NULL, ENTITY0307_STRING01 VARCHAR2(255) NULL, ENTITY0307_STRING02 VARCHAR2(255) NULL, ENTITY0307_STRING03 VARCHAR2(255) NULL, ENTITY0307_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0307_ID3, ENTITY0307_ID2, ENTITY0307_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0308 (ENTITY0308_ID2 NUMBER(19,4) NOT NULL, ENTITY0308_ID3 NUMBER(10) NOT NULL, ENTITY0308_ID1 NUMBER(19,4) NOT NULL, ENTITY0308_STRING01 VARCHAR2(255) NULL, ENTITY0308_STRING02 VARCHAR2(255) NULL, ENTITY0308_STRING03 VARCHAR2(255) NULL, ENTITY0308_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0308_ID2, ENTITY0308_ID3, ENTITY0308_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0309 (ENTITY0309_ID3 NUMBER(10) NOT NULL, ENTITY0309_ID1 NUMBER(19,4) NOT NULL, ENTITY0309_ID2 NUMBER(10) NOT NULL, ENTITY0309_STRING01 VARCHAR2(255) NULL, ENTITY0309_STRING02 VARCHAR2(255) NULL, ENTITY0309_STRING03 VARCHAR2(255) NULL, ENTITY0309_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0309_ID3, ENTITY0309_ID1, ENTITY0309_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0310 (ENTITY0310_ID3 NUMBER(19) NOT NULL, ENTITY0310_ID1 NUMBER(10) NOT NULL, ENTITY0310_ID2 NUMBER(10) NOT NULL, ENTITY0310_STRING01 VARCHAR2(255) NULL, ENTITY0310_STRING02 VARCHAR2(255) NULL, ENTITY0310_STRING03 VARCHAR2(255) NULL, ENTITY0310_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0310_ID3, ENTITY0310_ID1, ENTITY0310_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0311 (ENTITY0311_ID1 NUMBER(10) NOT NULL, ENTITY0311_ID2 NUMBER(19) NOT NULL, ENTITY0311_ID3 NUMBER(19) NOT NULL, ENTITY0311_STRING01 VARCHAR2(255) NULL, ENTITY0311_STRING02 VARCHAR2(255) NULL, ENTITY0311_STRING03 VARCHAR2(255) NULL, ENTITY0311_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0311_ID1, ENTITY0311_ID2, ENTITY0311_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0312 (ENTITY0312_ID1 NUMBER(19) NOT NULL, ENTITY0312_ID2 NUMBER(19) NOT NULL, ENTITY0312_ID3 NUMBER(5) NOT NULL, ENTITY0312_STRING01 VARCHAR2(255) NULL, ENTITY0312_STRING02 VARCHAR2(255) NULL, ENTITY0312_STRING03 VARCHAR2(255) NULL, ENTITY0312_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0312_ID1, ENTITY0312_ID2, ENTITY0312_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0313 (ENTITY0313_ID1 NUMBER(19) NOT NULL, ENTITY0313_ID2 NUMBER(5) NOT NULL, ENTITY0313_ID3 NUMBER(5) NOT NULL, ENTITY0313_STRING01 VARCHAR2(255) NULL, ENTITY0313_STRING02 VARCHAR2(255) NULL, ENTITY0313_STRING03 VARCHAR2(255) NULL, ENTITY0313_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0313_ID1, ENTITY0313_ID2, ENTITY0313_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0314 (ENTITY0314_ID3 NUMBER(38) NOT NULL, ENTITY0314_ID1 NUMBER(5) NOT NULL, ENTITY0314_ID2 NUMBER(5) NOT NULL, ENTITY0314_STRING01 VARCHAR2(255) NULL, ENTITY0314_STRING02 VARCHAR2(255) NULL, ENTITY0314_STRING03 VARCHAR2(255) NULL, ENTITY0314_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0314_ID3, ENTITY0314_ID1, ENTITY0314_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0315 (ENTITY0315_ID1 NUMBER(5) NOT NULL, ENTITY0315_ID2 NUMBER(38) NOT NULL, ENTITY0315_ID3 NUMBER(38) NOT NULL, ENTITY0315_STRING01 VARCHAR2(255) NULL, ENTITY0315_STRING02 VARCHAR2(255) NULL, ENTITY0315_STRING03 VARCHAR2(255) NULL, ENTITY0315_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0315_ID1, ENTITY0315_ID2, ENTITY0315_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0316 (ENTITY0316_ID1 NUMBER(38) NOT NULL, ENTITY0316_ID2 NUMBER(38) NOT NULL, ENTITY0316_ID3 DATE NOT NULL, ENTITY0316_STRING01 VARCHAR2(255) NULL, ENTITY0316_STRING02 VARCHAR2(255) NULL, ENTITY0316_STRING03 VARCHAR2(255) NULL, ENTITY0316_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0316_ID1, ENTITY0316_ID2, ENTITY0316_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0317 (ENTITY0317_ID1 NUMBER(38) NOT NULL, ENTITY0317_ID2 DATE NOT NULL, ENTITY0317_ID3 DATE NOT NULL, ENTITY0317_STRING01 VARCHAR2(255) NULL, ENTITY0317_STRING02 VARCHAR2(255) NULL, ENTITY0317_STRING03 VARCHAR2(255) NULL, ENTITY0317_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0317_ID1, ENTITY0317_ID2, ENTITY0317_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0318 (ENTITY0318_ID3 DATE NOT NULL, ENTITY0318_ID1 DATE NOT NULL, ENTITY0318_ID2 DATE NOT NULL, ENTITY0318_STRING01 VARCHAR2(255) NULL, ENTITY0318_STRING02 VARCHAR2(255) NULL, ENTITY0318_STRING03 VARCHAR2(255) NULL, ENTITY0318_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0318_ID3, ENTITY0318_ID1, ENTITY0318_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0319 (ENTITY0319_ID1 DATE NOT NULL, ENTITY0319_ID3 DATE NOT NULL, ENTITY0319_ID2 DATE NOT NULL, ENTITY0319_STRING01 VARCHAR2(255) NULL, ENTITY0319_STRING02 VARCHAR2(255) NULL, ENTITY0319_STRING03 VARCHAR2(255) NULL, ENTITY0319_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0319_ID1, ENTITY0319_ID3, ENTITY0319_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0320 (ENTITY0320_ID1 NUMBER(3) NOT NULL, ENTITY0320_ID3 VARCHAR2(255) NOT NULL, ENTITY0320_ID2 CHAR(1) NOT NULL, ENTITY0320_STRING01 VARCHAR2(255) NULL, ENTITY0320_STRING02 VARCHAR2(255) NULL, ENTITY0320_STRING03 VARCHAR2(255) NULL, ENTITY0320_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0320_ID1, ENTITY0320_ID3, ENTITY0320_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0321 (ENTITY0321_ID2 CHAR(1) NOT NULL, ENTITY0321_ID1 NUMBER(3) NOT NULL, ENTITY0321_ID3 NUMBER(19,4) NOT NULL, ENTITY0321_STRING01 VARCHAR2(255) NULL, ENTITY0321_STRING02 VARCHAR2(255) NULL, ENTITY0321_STRING03 VARCHAR2(255) NULL, ENTITY0321_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0321_ID2, ENTITY0321_ID1, ENTITY0321_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0322 (ENTITY0322_ID3 NUMBER(19,4) NOT NULL, ENTITY0322_ID2 VARCHAR2(255) NOT NULL, ENTITY0322_ID1 CHAR(1) NOT NULL, ENTITY0322_STRING01 VARCHAR2(255) NULL, ENTITY0322_STRING02 VARCHAR2(255) NULL, ENTITY0322_STRING03 VARCHAR2(255) NULL, ENTITY0322_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0322_ID3, ENTITY0322_ID2, ENTITY0322_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0323 (ENTITY0323_ID2 NUMBER(19,4) NOT NULL, ENTITY0323_ID1 CHAR(1) NOT NULL, ENTITY0323_ID3 NUMBER(19,4) NOT NULL, ENTITY0323_STRING01 VARCHAR2(255) NULL, ENTITY0323_STRING02 VARCHAR2(255) NULL, ENTITY0323_STRING03 VARCHAR2(255) NULL, ENTITY0323_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0323_ID2, ENTITY0323_ID1, ENTITY0323_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0324 (ENTITY0324_ID1 VARCHAR2(255) NOT NULL, ENTITY0324_ID3 NUMBER(19,4) NOT NULL, ENTITY0324_ID2 NUMBER(19,4) NOT NULL, ENTITY0324_STRING01 VARCHAR2(255) NULL, ENTITY0324_STRING02 VARCHAR2(255) NULL, ENTITY0324_STRING03 VARCHAR2(255) NULL, ENTITY0324_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0324_ID1, ENTITY0324_ID3, ENTITY0324_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0325 (ENTITY0325_ID2 NUMBER(19,4) NOT NULL, ENTITY0325_ID1 NUMBER(19,4) NOT NULL, ENTITY0325_ID3 NUMBER(10) NOT NULL, ENTITY0325_STRING01 VARCHAR2(255) NULL, ENTITY0325_STRING02 VARCHAR2(255) NULL, ENTITY0325_STRING03 VARCHAR2(255) NULL, ENTITY0325_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0325_ID2, ENTITY0325_ID1, ENTITY0325_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0326 (ENTITY0326_ID3 NUMBER(10) NOT NULL, ENTITY0326_ID2 NUMBER(19,4) NOT NULL, ENTITY0326_ID1 NUMBER(19,4) NOT NULL, ENTITY0326_STRING01 VARCHAR2(255) NULL, ENTITY0326_STRING02 VARCHAR2(255) NULL, ENTITY0326_STRING03 VARCHAR2(255) NULL, ENTITY0326_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0326_ID3, ENTITY0326_ID2, ENTITY0326_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0327 (ENTITY0327_ID3 NUMBER(19) NOT NULL, ENTITY0327_ID2 NUMBER(10) NOT NULL, ENTITY0327_ID1 NUMBER(19,4) NOT NULL, ENTITY0327_STRING01 VARCHAR2(255) NULL, ENTITY0327_STRING02 VARCHAR2(255) NULL, ENTITY0327_STRING03 VARCHAR2(255) NULL, ENTITY0327_VERSION TIMESTAMP NULL, PRIMARY KEY (ENTITY0327_ID3, ENTITY0327_ID2, ENTITY0327_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0328 (ENTITY0328_ID1 NUMBER(19,4) NOT NULL, ENTITY0328_ID3 NUMBER(19) NOT NULL, ENTITY0328_ID2 NUMBER(10) NOT NULL, ENTITY0328_STRING01 VARCHAR2(255) NULL, ENTITY0328_STRING02 VARCHAR2(255) NULL, ENTITY0328_STRING03 VARCHAR2(255) NULL, ENTITY0328_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0328_ID1, ENTITY0328_ID3, ENTITY0328_ID2));
CREATE TABLE ${schemaname}.CDM_Entity0329 (ENTITY0329_ID2 NUMBER(19) NOT NULL, ENTITY0329_ID1 NUMBER(10) NOT NULL, ENTITY0329_ID3 NUMBER(5) NOT NULL, ENTITY0329_STRING01 VARCHAR2(255) NULL, ENTITY0329_STRING02 VARCHAR2(255) NULL, ENTITY0329_STRING03 VARCHAR2(255) NULL, ENTITY0329_VERSION NUMBER(10) NULL, PRIMARY KEY (ENTITY0329_ID2, ENTITY0329_ID1, ENTITY0329_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0330 (ENTITY0330_ID3 NUMBER(5) NOT NULL, ENTITY0330_ID2 NUMBER(19) NOT NULL, ENTITY0330_ID1 NUMBER(10) NOT NULL, ENTITY0330_STRING01 VARCHAR2(255) NULL, ENTITY0330_STRING02 VARCHAR2(255) NULL, ENTITY0330_STRING03 VARCHAR2(255) NULL, ENTITY0330_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0330_ID3, ENTITY0330_ID2, ENTITY0330_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0331 (ENTITY0331_ID3 NUMBER(38) NOT NULL, ENTITY0331_ID2 NUMBER(5) NOT NULL, ENTITY0331_ID1 NUMBER(19) NOT NULL, ENTITY0331_STRING01 VARCHAR2(255) NULL, ENTITY0331_STRING02 VARCHAR2(255) NULL, ENTITY0331_STRING03 VARCHAR2(255) NULL, ENTITY0331_VERSION NUMBER(19) NULL, PRIMARY KEY (ENTITY0331_ID3, ENTITY0331_ID2, ENTITY0331_ID1));
CREATE TABLE ${schemaname}.CDM_Entity0332 (ENTITY0332_ID1 NUMBER(19) NOT NULL, ENTITY0332_ID2 NUMBER(5) NOT NULL, ENTITY0332_ID3 NUMBER(38) NOT NULL, ENTITY0332_STRING01 VARCHAR2(255) NULL, ENTITY0332_STRING02 VARCHAR2(255) NULL, ENTITY0332_STRING03 VARCHAR2(255) NULL, ENTITY0332_VERSION NUMBER(5) NULL, PRIMARY KEY (ENTITY0332_ID1, ENTITY0332_ID2, ENTITY0332_ID3));
CREATE TABLE ${schemaname}.CDM_Entity0401 (ENTITY0401_STRING01 VARCHAR2(255) NULL, ENTITY0401_STRING02 VARCHAR2(255) NULL, ENTITY0401_STRING03 VARCHAR2(255) NULL, ENTITY0401_VERSION NUMBER(19) NULL, ENTITY0401_ID_ENTITY0001_ID NUMBER(3) NOT NULL, PRIMARY KEY (ENTITY0401_ID_ENTITY0001_ID));
CREATE TABLE ${schemaname}.CDM_Entity0402 (ENTITY0402_STRING01 VARCHAR2(255) NULL, ENTITY0402_STRING02 VARCHAR2(255) NULL, ENTITY0402_STRING03 VARCHAR2(255) NULL, ENTITY0402_VERSION NUMBER(5) NULL, ENTITY0402_ID_ENTITY0002_ID NUMBER(3) NOT NULL, PRIMARY KEY (ENTITY0402_ID_ENTITY0002_ID));
CREATE TABLE ${schemaname}.CDM_Entity0403 (ENTITY0403_STRING01 VARCHAR2(255) NULL, ENTITY0403_STRING02 VARCHAR2(255) NULL, ENTITY0403_STRING03 VARCHAR2(255) NULL, ENTITY0403_VERSION NUMBER(5) NULL, ENTITY0403_ID_ENTITY0003_ID CHAR(1) NOT NULL, PRIMARY KEY (ENTITY0403_ID_ENTITY0003_ID));
CREATE TABLE ${schemaname}.CDM_Entity0404 (ENTITY0404_STRING01 VARCHAR2(255) NULL, ENTITY0404_STRING02 VARCHAR2(255) NULL, ENTITY0404_STRING03 VARCHAR2(255) NULL, ENTITY0404_VERSION TIMESTAMP NULL, ENTITY0404_ID_ENTITY0004_ID CHAR(1) NOT NULL, PRIMARY KEY (ENTITY0404_ID_ENTITY0004_ID));
CREATE TABLE ${schemaname}.CDM_Entity0405 (ENTITY0405_STRING01 VARCHAR2(255) NULL, ENTITY0405_STRING02 VARCHAR2(255) NULL, ENTITY0405_STRING03 VARCHAR2(255) NULL, ENTITY0405_VERSION NUMBER(10) NULL, ENTITY0405_ID_ENTITY0005_ID VARCHAR2(255) NOT NULL, PRIMARY KEY (ENTITY0405_ID_ENTITY0005_ID));
CREATE TABLE ${schemaname}.CDM_Entity0406 (ENTITY0406_STRING01 VARCHAR2(255) NULL, ENTITY0406_STRING02 VARCHAR2(255) NULL, ENTITY0406_STRING03 VARCHAR2(255) NULL, ENTITY0406_VERSION NUMBER(10) NULL, ENTITY0406_ID_ENTITY0006_ID NUMBER(19,4) NOT NULL, PRIMARY KEY (ENTITY0406_ID_ENTITY0006_ID));
CREATE TABLE ${schemaname}.CDM_Entity0407 (ENTITY0407_STRING01 VARCHAR2(255) NULL, ENTITY0407_STRING02 VARCHAR2(255) NULL, ENTITY0407_STRING03 VARCHAR2(255) NULL, ENTITY0407_VERSION NUMBER(19) NULL, ENTITY0407_ID_ENTITY0007_ID NUMBER(19,4) NOT NULL, PRIMARY KEY (ENTITY0407_ID_ENTITY0007_ID));
CREATE TABLE ${schemaname}.CDM_Entity0408 (ENTITY0408_STRING01 VARCHAR2(255) NULL, ENTITY0408_STRING02 VARCHAR2(255) NULL, ENTITY0408_STRING03 VARCHAR2(255) NULL, ENTITY0408_VERSION NUMBER(19) NULL, ENTITY0408_ID_ENTITY0008_ID NUMBER(19,4) NOT NULL, PRIMARY KEY (ENTITY0408_ID_ENTITY0008_ID));
CREATE TABLE ${schemaname}.CDM_Entity0409 (ENTITY0409_STRING01 VARCHAR2(255) NULL, ENTITY0409_STRING02 VARCHAR2(255) NULL, ENTITY0409_STRING03 VARCHAR2(255) NULL, ENTITY0409_VERSION NUMBER(5) NULL, ENTITY0409_ID_ENTITY0009_ID NUMBER(19,4) NOT NULL, PRIMARY KEY (ENTITY0409_ID_ENTITY0009_ID));
CREATE TABLE ${schemaname}.CDM_Entity0410 (ENTITY0410_STRING01 VARCHAR2(255) NULL, ENTITY0410_STRING02 VARCHAR2(255) NULL, ENTITY0410_STRING03 VARCHAR2(255) NULL, ENTITY0410_VERSION NUMBER(5) NULL, ENTITY0410_ID_ENTITY0010_ID NUMBER(10) NOT NULL, PRIMARY KEY (ENTITY0410_ID_ENTITY0010_ID));
CREATE TABLE ${schemaname}.CDM_Entity0411 (ENTITY0411_STRING01 VARCHAR2(255) NULL, ENTITY0411_STRING02 VARCHAR2(255) NULL, ENTITY0411_STRING03 VARCHAR2(255) NULL, ENTITY0411_VERSION TIMESTAMP NULL, ENTITY0411_ID_ENTITY0011_ID NUMBER(10) NOT NULL, PRIMARY KEY (ENTITY0411_ID_ENTITY0011_ID));
CREATE TABLE ${schemaname}.CDM_Entity0412 (ENTITY0412_STRING01 VARCHAR2(255) NULL, ENTITY0412_STRING02 VARCHAR2(255) NULL, ENTITY0412_STRING03 VARCHAR2(255) NULL, ENTITY0412_VERSION NUMBER(10) NULL, ENTITY0412_ID_ENTITY0012_ID NUMBER(19) NOT NULL, PRIMARY KEY (ENTITY0412_ID_ENTITY0012_ID));
CREATE TABLE ${schemaname}.CDM_Entity0413 (ENTITY0413_STRING01 VARCHAR2(255) NULL, ENTITY0413_STRING02 VARCHAR2(255) NULL, ENTITY0413_STRING03 VARCHAR2(255) NULL, ENTITY0413_VERSION NUMBER(10) NULL, ENTITY0413_ID_ENTITY0013_ID NUMBER(19) NOT NULL, PRIMARY KEY (ENTITY0413_ID_ENTITY0013_ID));
CREATE TABLE ${schemaname}.CDM_Entity0414 (ENTITY0414_STRING01 VARCHAR2(255) NULL, ENTITY0414_STRING02 VARCHAR2(255) NULL, ENTITY0414_STRING03 VARCHAR2(255) NULL, ENTITY0414_VERSION NUMBER(19) NULL, ENTITY0414_ID_ENTITY0014_ID NUMBER(5) NOT NULL, PRIMARY KEY (ENTITY0414_ID_ENTITY0014_ID));
CREATE TABLE ${schemaname}.CDM_Entity0415 (ENTITY0415_STRING01 VARCHAR2(255) NULL, ENTITY0415_STRING02 VARCHAR2(255) NULL, ENTITY0415_STRING03 VARCHAR2(255) NULL, ENTITY0415_VERSION NUMBER(19) NULL, ENTITY0415_ID_ENTITY0015_ID NUMBER(5) NOT NULL, PRIMARY KEY (ENTITY0415_ID_ENTITY0015_ID));
CREATE TABLE ${schemaname}.CDM_Entity0416 (ENTITY0416_STRING01 VARCHAR2(255) NULL, ENTITY0416_STRING02 VARCHAR2(255) NULL, ENTITY0416_STRING03 VARCHAR2(255) NULL, ENTITY0416_VERSION NUMBER(5) NULL, ENTITY0416_ID_ENTITY0016_ID NUMBER(38) NOT NULL, PRIMARY KEY (ENTITY0416_ID_ENTITY0016_ID));
CREATE TABLE ${schemaname}.CDM_Entity0417 (ENTITY0417_STRING01 VARCHAR2(255) NULL, ENTITY0417_STRING02 VARCHAR2(255) NULL, ENTITY0417_STRING03 VARCHAR2(255) NULL, ENTITY0417_VERSION NUMBER(5) NULL, ENTITY0417_ID_ENTITY0017_ID NUMBER(38) NOT NULL, PRIMARY KEY (ENTITY0417_ID_ENTITY0017_ID));
CREATE TABLE ${schemaname}.CDM_Entity0418 (ENTITY0418_STRING01 VARCHAR2(255) NULL, ENTITY0418_STRING02 VARCHAR2(255) NULL, ENTITY0418_STRING03 VARCHAR2(255) NULL, ENTITY0418_VERSION TIMESTAMP NULL, ENTITY0418_ID_ENTITY0018_ID DATE NOT NULL, PRIMARY KEY (ENTITY0418_ID_ENTITY0018_ID));
CREATE TABLE ${schemaname}.CDM_Entity0419 (ENTITY0419_STRING01 VARCHAR2(255) NULL, ENTITY0419_STRING02 VARCHAR2(255) NULL, ENTITY0419_STRING03 VARCHAR2(255) NULL, ENTITY0419_VERSION NUMBER(10) NULL, ENTITY0419_ID_ENTITY0019_ID DATE NOT NULL, PRIMARY KEY (ENTITY0419_ID_ENTITY0019_ID));
ALTER TABLE ${schemaname}.CDM_Entity0401 ADD CONSTRAINT CDMntty0401NTTY0401DNTTY0001ID FOREIGN KEY (ENTITY0401_ID_ENTITY0001_ID) REFERENCES CDM_Entity0001 (ENTITY0001_ID);
ALTER TABLE ${schemaname}.CDM_Entity0402 ADD CONSTRAINT CDMntty0402NTTY0402DNTTY0002ID FOREIGN KEY (ENTITY0402_ID_ENTITY0002_ID) REFERENCES CDM_Entity0002 (ENTITY0002_ID);
ALTER TABLE ${schemaname}.CDM_Entity0403 ADD CONSTRAINT CDMntty0403NTTY0403DNTTY0003ID FOREIGN KEY (ENTITY0403_ID_ENTITY0003_ID) REFERENCES CDM_Entity0003 (ENTITY0003_ID);
ALTER TABLE ${schemaname}.CDM_Entity0404 ADD CONSTRAINT CDMntty0404NTTY0404DNTTY0004ID FOREIGN KEY (ENTITY0404_ID_ENTITY0004_ID) REFERENCES CDM_Entity0004 (ENTITY0004_ID);
ALTER TABLE ${schemaname}.CDM_Entity0405 ADD CONSTRAINT CDMntty0405NTTY0405DNTTY0005ID FOREIGN KEY (ENTITY0405_ID_ENTITY0005_ID) REFERENCES CDM_Entity0005 (ENTITY0005_ID);
ALTER TABLE ${schemaname}.CDM_Entity0406 ADD CONSTRAINT CDMntty0406NTTY0406DNTTY0006ID FOREIGN KEY (ENTITY0406_ID_ENTITY0006_ID) REFERENCES CDM_Entity0006 (ENTITY0006_ID);
ALTER TABLE ${schemaname}.CDM_Entity0407 ADD CONSTRAINT CDMntty0407NTTY0407DNTTY0007ID FOREIGN KEY (ENTITY0407_ID_ENTITY0007_ID) REFERENCES CDM_Entity0007 (ENTITY0007_ID);
ALTER TABLE ${schemaname}.CDM_Entity0408 ADD CONSTRAINT CDMntty0408NTTY0408DNTTY0008ID FOREIGN KEY (ENTITY0408_ID_ENTITY0008_ID) REFERENCES CDM_Entity0008 (ENTITY0008_ID);
ALTER TABLE ${schemaname}.CDM_Entity0409 ADD CONSTRAINT CDMntty0409NTTY0409DNTTY0009ID FOREIGN KEY (ENTITY0409_ID_ENTITY0009_ID) REFERENCES CDM_Entity0009 (ENTITY0009_ID);
ALTER TABLE ${schemaname}.CDM_Entity0410 ADD CONSTRAINT CDMntty0410NTTY0410DNTTY0010ID FOREIGN KEY (ENTITY0410_ID_ENTITY0010_ID) REFERENCES CDM_Entity0010 (ENTITY0010_ID);
ALTER TABLE ${schemaname}.CDM_Entity0411 ADD CONSTRAINT CDMntty0411NTTY0411DNTTY0011ID FOREIGN KEY (ENTITY0411_ID_ENTITY0011_ID) REFERENCES CDM_Entity0011 (ENTITY0011_ID);
ALTER TABLE ${schemaname}.CDM_Entity0412 ADD CONSTRAINT CDMntty0412NTTY0412DNTTY0012ID FOREIGN KEY (ENTITY0412_ID_ENTITY0012_ID) REFERENCES CDM_Entity0012 (ENTITY0012_ID);
ALTER TABLE ${schemaname}.CDM_Entity0413 ADD CONSTRAINT CDMntty0413NTTY0413DNTTY0013ID FOREIGN KEY (ENTITY0413_ID_ENTITY0013_ID) REFERENCES CDM_Entity0013 (ENTITY0013_ID);
ALTER TABLE ${schemaname}.CDM_Entity0414 ADD CONSTRAINT CDMntty0414NTTY0414DNTTY0014ID FOREIGN KEY (ENTITY0414_ID_ENTITY0014_ID) REFERENCES CDM_Entity0014 (ENTITY0014_ID);
ALTER TABLE ${schemaname}.CDM_Entity0415 ADD CONSTRAINT CDMntty0415NTTY0415DNTTY0015ID FOREIGN KEY (ENTITY0415_ID_ENTITY0015_ID) REFERENCES CDM_Entity0015 (ENTITY0015_ID);
ALTER TABLE ${schemaname}.CDM_Entity0416 ADD CONSTRAINT CDMntty0416NTTY0416DNTTY0016ID FOREIGN KEY (ENTITY0416_ID_ENTITY0016_ID) REFERENCES CDM_Entity0016 (ENTITY0016_ID);
ALTER TABLE ${schemaname}.CDM_Entity0417 ADD CONSTRAINT CDMntty0417NTTY0417DNTTY0017ID FOREIGN KEY (ENTITY0417_ID_ENTITY0017_ID) REFERENCES CDM_Entity0017 (ENTITY0017_ID);
ALTER TABLE ${schemaname}.CDM_Entity0418 ADD CONSTRAINT CDMntty0418NTTY0418DNTTY0018ID FOREIGN KEY (ENTITY0418_ID_ENTITY0018_ID) REFERENCES CDM_Entity0018 (ENTITY0018_ID);
ALTER TABLE ${schemaname}.CDM_Entity0419 ADD CONSTRAINT CDMntty0419NTTY0419DNTTY0019ID FOREIGN KEY (ENTITY0419_ID_ENTITY0019_ID) REFERENCES CDM_Entity0019 (ENTITY0019_ID);