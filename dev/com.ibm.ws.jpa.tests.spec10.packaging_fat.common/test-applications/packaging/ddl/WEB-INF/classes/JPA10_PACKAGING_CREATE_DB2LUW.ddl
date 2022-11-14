CREATE TABLE XMLSchName.XMLTableName (id INTEGER NOT NULL, XMLName VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.PKGAO_GAOE (id INTEGER NOT NULL, annotatedEagerName VARCHAR(255), annotatedLazyName VARCHAR(255), annotatedNonUniqueName VARCHAR(255) NOT NULL, annotatedUniqueName VARCHAR(255), lengthBoundString VARCHAR(20), name VARCHAR(255), PRIMARY KEY (id), UNIQUE (annotatedNonUniqueName));

CREATE TABLE DefSchmea.EntListTestEntity (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDEntity1 (id INTEGER NOT NULL, city VARCHAR(255), name VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDEntity2 (id INTEGER NOT NULL, city VARCHAR(255), name VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDFQEmbedEnt (id INTEGER NOT NULL, name VARCHAR(255), city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDMSC1Ent (id INTEGER NOT NULL, name VARCHAR(255), city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDMSC2Ent (id INTEGER NOT NULL, name VARCHAR(255), city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDNFQEmbedEnt (id INTEGER NOT NULL, name VARCHAR(255), city VARCHAR(255), state VARCHAR(255), street VARCHAR(255), zip VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDRelationalEntA (id INTEGER NOT NULL, name VARCHAR(255), manyXoneEntityB_id INTEGER, oneXoneEntityB_id INTEGER, PRIMARY KEY (id));
CREATE TABLE DefSchmea.PKGMFD_REA_MMEBC_TBL (MFDRelationalEntA INTEGER, MFDRelationalMMB INTEGER);
CREATE TABLE DefSchmea.PKGMFD_REA_OMEBC_TBL (MFDRelationalEntA INTEGER, MFDRelationalOMB INTEGER);
CREATE TABLE DefSchmea.MFDRelationalEntA_MFDRelationalMMB (MFDRelationalEntA_id INTEGER, manyXmanyEntityBCollection_id INTEGER);
CREATE TABLE DefSchmea.MFDRelationalEntA_MFDRelationalOMB (MFDRelationalEntA_id INTEGER, oneXmanyEntityBCollection_id INTEGER);
CREATE TABLE DefSchmea.MFDRelationalMMB (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDRelationalMOB (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDRelationalOMB (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE DefSchmea.MFDRelationalOOB (id INTEGER NOT NULL, name VARCHAR(255), PRIMARY KEY (id));

CREATE INDEX DefSchmea.I_MFDRLNT_MANYXONE ON DefSchmea.MFDRelationalEntA (manyXoneEntityB_id);
CREATE INDEX DefSchmea.I_MFDRLNT_ONEXONEE ON DefSchmea.MFDRelationalEntA (oneXoneEntityB_id);
CREATE INDEX DefSchmea.I_PKGMFD_REA003 ON DefSchmea.PKGMFD_REA_MMEBC_TBL (MFDRelationalEntA);
CREATE INDEX DefSchmea.I_PKGMFD_REA004 ON DefSchmea.PKGMFD_REA_MMEBC_TBL (MFDRelationalMMB);
CREATE INDEX DefSchmea.I_PKGMFD_REA005 ON DefSchmea.PKGMFD_REA_OMEBC_TBL (MFDRelationalEntA);
CREATE INDEX DefSchmea.I_PKGMFD_REA006 ON DefSchmea.PKGMFD_REA_OMEBC_TBL (MFDRelationalOMB);
CREATE INDEX DefSchmea.I_MFDRMMB_ELEMENT ON DefSchmea.MFDRelationalEntA_MFDRelationalMMB (manyXmanyEntityBCollection_id);
CREATE INDEX DefSchmea.I_MFDRMMB_MFDRELAT ON DefSchmea.MFDRelationalEntA_MFDRelationalMMB (MFDRelationalEntA_id);
CREATE INDEX DefSchmea.I_MFDRLMB_ELEMENT ON DefSchmea.MFDRelationalEntA_MFDRelationalOMB (oneXmanyEntityBCollection_id);
CREATE INDEX DefSchmea.I_MFDRLMB_MFDRELAT ON DefSchmea.MFDRelationalEntA_MFDRelationalOMB (MFDRelationalEntA_id);

CREATE TABLE ${schemaname}.MDCEmbedEntity (id INTEGER NOT NULL, name VARCHAR(30), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.MDCEntity (id INTEGER NOT NULL, name VARCHAR(30), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.MDCMSCEntity (id INTEGER NOT NULL, name VARCHAR(30), PRIMARY KEY (id));

CREATE TABLE ${schemaname}.AnnotationOnlyEntity (id INTEGER NOT NULL, name VARCHAR(12), PRIMARY KEY (id));
CREATE TABLE ${schemaname}.XMLCompleteTestEntity (id INTEGER NOT NULL, nonOptionalName VARCHAR(255), PRIMARY KEY (id));

