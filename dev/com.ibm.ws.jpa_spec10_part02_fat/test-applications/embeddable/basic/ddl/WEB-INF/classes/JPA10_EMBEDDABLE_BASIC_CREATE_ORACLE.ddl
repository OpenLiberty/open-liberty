CREATE TABLE ${schemaname}.AnnRootEmBT (id NUMBER NOT NULL, enumeratedOrdinalValueFA NUMBER, enumeratedStringValueFA VARCHAR2(20), enumeratedOrdinalValuePA NUMBER, enumeratedStringValuePA VARCHAR2(20), integerValueAttributeOverride NUMBER, integerValue NUMBER, integerValueFAColumn NUMBER, integerValuePAColumn NUMBER, clobValueFA CLOB, clobValuePA CLOB, temporalValueFA DATE, temporalValuePA DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.ColDate (parent_id NUMBER, value TIMESTAMP);
CREATE TABLE ${schemaname}.ColEnum (parent_id NUMBER, value VARCHAR2(255));
CREATE TABLE ${schemaname}.ColInt (parent_id NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.ColLob (parent_id NUMBER, value VARCHAR2(255));
CREATE TABLE ${schemaname}.EntColLobPAE (parent_id NUMBER, clobValuePA CLOB);
CREATE TABLE ${schemaname}.EntListEnumPAE (parent_id NUMBER, enumeratedOrdinalValuePA NUMBER, enumeratedStringValuePA VARCHAR2(20), valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.EntListIntegerAOE (parent_id NUMBER, integerValueAttributeOverride NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.EntListIntegerE (parent_id NUMBER, integerValue NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.EntListIntegerPAE (parent_id NUMBER, integerValuePAColumn NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.EntListTemporalPAE (parent_id NUMBER, temporalValuePA DATE, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.EntMapDateTemporalPAE (parent_id NUMBER, mykey DATE NOT NULL, temporalValuePA DATE);
CREATE TABLE ${schemaname}.EntMapEnumPAEEnumPAE (parent_id NUMBER, mykey VARCHAR2(255) NOT NULL, valueOrdinal NUMBER, valueString VARCHAR2(255));
CREATE TABLE ${schemaname}.EntMapIntegerEIntegerE (parent_id NUMBER, mykey NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.EntMapIntegerIntegerPAE (parent_id NUMBER, mykey NUMBER NOT NULL, value NUMBER);
CREATE TABLE ${schemaname}.EntMapIntegerTemporalPAE (parent_id NUMBER, mykey NUMBER NOT NULL, temporalValuePA DATE);
CREATE TABLE ${schemaname}.EntMapLobPAELobPAE (parent_id NUMBER, mykey CLOB, value CLOB);
CREATE TABLE ${schemaname}.EntSetIntegerPAE (parent_id NUMBER, integerValuePAColumn NUMBER);
CREATE TABLE ${schemaname}.ListDate (parent_id NUMBER, value TIMESTAMP, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.ListEnum (parent_id NUMBER, value VARCHAR2(255), valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.ListInt (parent_id NUMBER, value NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.ListLob (parent_id NUMBER, value VARCHAR2(255), valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.MapDateDate (parent_id NUMBER, mykey DATE NOT NULL, value TIMESTAMP);
CREATE TABLE ${schemaname}.MapEnumEnum (parent_id NUMBER, mykey VARCHAR2(255) NOT NULL, value VARCHAR2(255));
CREATE TABLE ${schemaname}.MapEnumLob (parent_id NUMBER, mykey VARCHAR2(255) NOT NULL, value VARCHAR2(255));
CREATE TABLE ${schemaname}.MapIntDate (parent_id NUMBER, mykey NUMBER NOT NULL, value TIMESTAMP);
CREATE TABLE ${schemaname}.MapIntInt (parent_id NUMBER, mykey NUMBER NOT NULL, value NUMBER);
CREATE TABLE ${schemaname}.SetDate (parent_id NUMBER, value TIMESTAMP);
CREATE TABLE ${schemaname}.SetEnum (parent_id NUMBER, value VARCHAR2(255));
CREATE TABLE ${schemaname}.SetInt (parent_id NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.SetLob (parent_id NUMBER, value VARCHAR2(255));
CREATE INDEX I_COLDATE_PARENT_ID ON ${schemaname}.ColDate (parent_id);
CREATE INDEX I_COLENUM_PARENT_ID ON ${schemaname}.ColEnum (parent_id);
CREATE INDEX I_COLINT_PARENT_ID ON ${schemaname}.ColInt (parent_id);
CREATE INDEX I_COLLOB_PARENT_ID ON ${schemaname}.ColLob (parent_id);
CREATE INDEX I_NTCLLBP_PARENT_ID ON ${schemaname}.EntColLobPAE (parent_id);
CREATE INDEX I_NTLSNMP_PARENT_ID ON ${schemaname}.EntListEnumPAE (parent_id);
CREATE INDEX I_NTLSTGR_PARENT_ID ON ${schemaname}.EntListIntegerAOE (parent_id);
CREATE INDEX I_NTLSTGR_PARENT_ID1 ON ${schemaname}.EntListIntegerE (parent_id);
CREATE INDEX I_NTLSGRP_PARENT_ID ON ${schemaname}.EntListIntegerPAE (parent_id);
CREATE INDEX I_NTLSRLP_PARENT_ID ON ${schemaname}.EntListTemporalPAE (parent_id);
CREATE INDEX I_NTMPRLP_PARENT_ID1 ON ${schemaname}.EntMapDateTemporalPAE (parent_id);
CREATE INDEX I_NTMPNMP_PARENT_ID ON ${schemaname}.EntMapEnumPAEEnumPAE (parent_id);
CREATE INDEX I_NTMPTGR_PARENT_ID ON ${schemaname}.EntMapIntegerEIntegerE (parent_id);
CREATE INDEX I_NTMPGRP_PARENT_ID ON ${schemaname}.EntMapIntegerIntegerPAE (parent_id);
CREATE INDEX I_NTMPRLP_PARENT_ID ON ${schemaname}.EntMapIntegerTemporalPAE (parent_id);
CREATE INDEX I_NTMPLBP_PARENT_ID ON ${schemaname}.EntMapLobPAELobPAE (parent_id);
CREATE INDEX I_NTSTGRP_PARENT_ID ON ${schemaname}.EntSetIntegerPAE (parent_id);
CREATE INDEX I_LISTDTE_PARENT_ID ON ${schemaname}.ListDate (parent_id);
CREATE INDEX I_LISTNUM_PARENT_ID ON ${schemaname}.ListEnum (parent_id);
CREATE INDEX I_LISTINT_PARENT_ID ON ${schemaname}.ListInt (parent_id);
CREATE INDEX I_LISTLOB_PARENT_ID ON ${schemaname}.ListLob (parent_id);
CREATE INDEX I_MPDTDTE_PARENT_ID ON ${schemaname}.MapDateDate (parent_id);
CREATE INDEX I_MPNMNUM_PARENT_ID ON ${schemaname}.MapEnumEnum (parent_id);
CREATE INDEX I_MPNUMLB_PARENT_ID ON ${schemaname}.MapEnumLob (parent_id);
CREATE INDEX I_MPINTDT_PARENT_ID ON ${schemaname}.MapIntDate (parent_id);
CREATE INDEX I_MPNTINT_PARENT_ID ON ${schemaname}.MapIntInt (parent_id);
CREATE INDEX I_SETDATE_PARENT_ID ON ${schemaname}.SetDate (parent_id);
CREATE INDEX I_SETENUM_PARENT_ID ON ${schemaname}.SetEnum (parent_id);
CREATE INDEX I_SETINT_PARENT_ID ON ${schemaname}.SetInt (parent_id);
CREATE INDEX I_SETLOB_PARENT_ID ON ${schemaname}.SetLob (parent_id);
CREATE TABLE ${schemaname}.XMLColDate (parent_id NUMBER, value DATE);
CREATE TABLE ${schemaname}.XMLColEnum (parent_id NUMBER, value VARCHAR2(255));
CREATE TABLE ${schemaname}.XMLColInt (parent_id NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.XMLColLob (parent_id NUMBER, value CLOB);
CREATE TABLE ${schemaname}.XMLEntColLobPAE (parent_id NUMBER, clobValuePA CLOB);
CREATE TABLE ${schemaname}.XMLEntListEnumPAE (parent_id NUMBER, enumeratedOrdinalValuePA NUMBER, enumeratedStringValuePA VARCHAR2(20), valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLEntListIntegerAOE (parent_id NUMBER, integerValueAttributeOverride NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLEntListIntegerE (parent_id NUMBER, integerValue NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLEntListIntegerPAE (parent_id NUMBER, integerValuePAColumn NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLListDate (parent_id NUMBER, value DATE, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLListEnum (parent_id NUMBER, value VARCHAR2(255), valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLListInt (parent_id NUMBER, value NUMBER, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLListLob (parent_id NUMBER, value CLOB, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLListTemporalPAE (parent_id NUMBER, temporalValuePA DATE, valueOrderColumn NUMBER);
CREATE TABLE ${schemaname}.XMLMapDateDate (parent_id NUMBER, mykey TIMESTAMP, value DATE);
CREATE TABLE ${schemaname}.XMLMapDateTemporalPAE (parent_id NUMBER, mykey TIMESTAMP, temporalValuePA DATE);
CREATE TABLE ${schemaname}.XMLMapEnumEnum (parent_id NUMBER, mykey VARCHAR2(255), value VARCHAR2(255));
CREATE TABLE ${schemaname}.XMLMapEnumLob (parent_id NUMBER, mykey VARCHAR2(255), value CLOB);
CREATE TABLE ${schemaname}.XMLMapIntDate (parent_id NUMBER, mykey NUMBER, value DATE);
CREATE TABLE ${schemaname}.XMLMapIntegerTemporalPAE (parent_id NUMBER, mykey NUMBER, temporalValuePA DATE);
CREATE TABLE ${schemaname}.XMLMapIntInt (parent_id NUMBER, mykey NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.XMLRootEmBT (id NUMBER NOT NULL, enumeratedOrdinalValueFA NUMBER, enumeratedStringValueFA VARCHAR2(20), enumeratedOrdinalValuePA NUMBER, enumeratedStringValuePA VARCHAR2(20), integerValueAttributeOverride NUMBER, integerValue NUMBER, integerValueFAColumn NUMBER, integerValuePAColumn NUMBER, transientValue NUMBER, clobValueFA CLOB, clobValuePA CLOB, temporalValueFA DATE, temporalValuePA DATE, PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLSetDate (parent_id NUMBER, value DATE);
CREATE TABLE ${schemaname}.XMLSetEnum (parent_id NUMBER, value VARCHAR2(255));
CREATE TABLE ${schemaname}.XMLSetInt (parent_id NUMBER, value NUMBER);
CREATE TABLE ${schemaname}.XMLSetIntegerPAE (parent_id NUMBER, integerValuePAColumn NUMBER);
CREATE TABLE ${schemaname}.XMLSetLob (parent_id NUMBER, value CLOB);
CREATE INDEX I_XMLCLDT_PARENT_ID ON ${schemaname}.XMLColDate (parent_id);
CREATE INDEX I_XMLCLNM_PARENT_ID ON ${schemaname}.XMLColEnum (parent_id);
CREATE INDEX I_XMLCLNT_PARENT_ID ON ${schemaname}.XMLColInt (parent_id);
CREATE INDEX I_XMLCLLB_PARENT_ID ON ${schemaname}.XMLColLob (parent_id);
CREATE INDEX I_XMLNLBP_PARENT_ID ON ${schemaname}.XMLEntColLobPAE (parent_id);
CREATE INDEX I_XMLNNMP_PARENT_ID ON ${schemaname}.XMLEntListEnumPAE (parent_id);
CREATE INDEX I_XMLNTGR_PARENT_ID ON ${schemaname}.XMLEntListIntegerAOE (parent_id);
CREATE INDEX I_XMLNTGR_PARENT_ID1 ON ${schemaname}.XMLEntListIntegerE (parent_id);
CREATE INDEX I_XMLNGRP_PARENT_ID ON ${schemaname}.XMLEntListIntegerPAE (parent_id);
CREATE INDEX I_XMLLTDT_PARENT_ID ON ${schemaname}.XMLListDate (parent_id);
CREATE INDEX I_XMLLTNM_PARENT_ID ON ${schemaname}.XMLListEnum (parent_id);
CREATE INDEX I_XMLLTNT_PARENT_ID ON ${schemaname}.XMLListInt (parent_id);
CREATE INDEX I_XMLLTLB_PARENT_ID ON ${schemaname}.XMLListLob (parent_id);
CREATE INDEX I_XMLLRLP_PARENT_ID ON ${schemaname}.XMLListTemporalPAE (parent_id);
CREATE INDEX I_XMLMTDT_PARENT_ID1 ON ${schemaname}.XMLMapDateDate (parent_id);
CREATE INDEX I_XMLMRLP_PARENT_ID1 ON ${schemaname}.XMLMapDateTemporalPAE (parent_id);
CREATE INDEX I_XMLMMNM_PARENT_ID ON ${schemaname}.XMLMapEnumEnum (parent_id);
CREATE INDEX I_XMLMMLB_PARENT_ID ON ${schemaname}.XMLMapEnumLob (parent_id);
CREATE INDEX I_XMLMTDT_PARENT_ID ON ${schemaname}.XMLMapIntDate (parent_id);
CREATE INDEX I_XMLMRLP_PARENT_ID ON ${schemaname}.XMLMapIntegerTemporalPAE (parent_id);
CREATE INDEX I_XMLMTNT_PARENT_ID ON ${schemaname}.XMLMapIntInt (parent_id);
CREATE INDEX I_XMLSTDT_PARENT_ID ON ${schemaname}.XMLSetDate (parent_id);
CREATE INDEX I_XMLSTNM_PARENT_ID ON ${schemaname}.XMLSetEnum (parent_id);
CREATE INDEX I_XMLSTNT_PARENT_ID ON ${schemaname}.XMLSetInt (parent_id);
CREATE INDEX I_XMLSGRP_PARENT_ID ON ${schemaname}.XMLSetIntegerPAE (parent_id);
CREATE INDEX I_XMLSTLB_PARENT_ID ON ${schemaname}.XMLSetLob (parent_id);