CREATE TABLE EmbedIDOOEntA (id INT NOT NULL, password VARCHAR(255) NULL, userName VARCHAR(255) NULL, identity_id INT NULL, identity_country VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE EmbedIDOOEntB (country VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(255) NULL, salary INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (country, id));
CREATE TABLE IDClassOOEntityA (id INT NOT NULL, password VARCHAR(255) NULL, userName VARCHAR(255) NULL, identity_id INT NULL, identity_country VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE IDClassOOEntityB (country VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(255) NULL, salary INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (country, id));
CREATE TABLE OOBiCardEntA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id), UNIQUE(B_ID));
CREATE TABLE OOBiCardEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntA (id INT NOT NULL, name VARCHAR(255) NULL, BIENT_B1CP INT NULL, BIENT_B1 INT NULL, B2_ID INT NULL, BIENT_B4 INT NULL, BIENT_B1CA INT NULL, BIENT_B1CM INT NULL, BIENT_B1RF INT NULL, BIENT_B1RM INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B1 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B2 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B4 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B5CA (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B5CM (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B5CP (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B5RF (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOBiEntB_B5RM (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOCardEntA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id), UNIQUE(B_ID));
CREATE TABLE OOCardEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OONoOptBiEntityA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OONoOptBiEntityB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OONoOptEntityA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OONoOptEntityB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOUniEntA (id INT NOT NULL, name VARCHAR(255) NULL, UNIENT_B1 INT NULL, B2_ID INT NULL, B4_ID INT NULL, B5CA_ID INT NULL, B5CM_ID INT NULL, B5CP_ID INT NULL, B5RF_ID INT NULL, B5RM_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE OOUniEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE PKJoinOOEntityA (id INT NOT NULL, strVal VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE PKJoinOOEntityB (id INT NOT NULL, intVal INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLEmbedIDOOEntA (id INT NOT NULL, password VARCHAR(255) NULL, userName VARCHAR(255) NULL, identity_id INT NULL, identity_country VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLEmbedIDOOEntB (country VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(255) NULL, salary INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (country, id));
CREATE TABLE XMLIDClassOOEntityA (id INT NOT NULL, password VARCHAR(255) NULL, userName VARCHAR(255) NULL, identity_id INT NULL, identity_country VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLIDClassOOEntityB (country VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(255) NULL, salary INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (country, id));
CREATE TABLE XMLOOBiCardEntA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id), UNIQUE(B_ID));
CREATE TABLE XMLOOBiCardEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntA (id INT NOT NULL, name VARCHAR(255) NULL, B5CM_ID INT NULL, XMLBIENT_B1 INT NULL, B2_ID INT NULL, B4_ID INT NULL, B5CA_ID INT NULL, B5CP_ID INT NULL, B5RF_ID INT NULL, B5RM_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B1 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B2 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B4 (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B5CA (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B5CM (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B5CP (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B5RF (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOBiEntB_B5RM (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOCardEntA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id), UNIQUE(B_ID));
CREATE TABLE XMLOOCardEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOONoOptBiEntityA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOONoOptBiEntityB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOONoOptEntityA (id INT NOT NULL, name VARCHAR(255) NULL, B_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOONoOptEntityB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOUniEntA (id INT NOT NULL, name VARCHAR(255) NULL, UNIENT_B1 INT NULL, B2_ID INT NULL, B4_ID INT NULL, B5CA_ID INT NULL, B5CM_ID INT NULL, B5CP_ID INT NULL, B5RF_ID INT NULL, B5RM_ID INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLOOUniEntB (id INT NOT NULL, name VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLPKJoinOOEnA (id INT NOT NULL, strVal VARCHAR(255) NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE TABLE XMLPKJoinOOEnB (id INT NOT NULL, intVal INT NULL, UNQ_INDEX NUMERIC IDENTITY UNIQUE, PRIMARY KEY (id));
CREATE INDEX I_MBDDONT_IDENTITY ON EmbedIDOOEntA (identity_id, identity_country);
CREATE INDEX I_DCLSTTY_IDENTITY ON IDClassOOEntityA (identity_id, identity_country);
CREATE INDEX I_OBCRDNT_B ON OOBiCardEntA (B_ID);
CREATE INDEX I_OOBIENT_B1 ON OOBiEntA (BIENT_B1);
CREATE INDEX I_OOBIENT_B2 ON OOBiEntA (B2_ID);
CREATE INDEX I_OOBIENT_B4 ON OOBiEntA (BIENT_B4);
CREATE INDEX I_OOBIENT_B5CA ON OOBiEntA (BIENT_B1CA);
CREATE INDEX I_OOBIENT_B5CM ON OOBiEntA (BIENT_B1CM);
CREATE INDEX I_OOBIENT_B5CP ON OOBiEntA (BIENT_B1CP);
CREATE INDEX I_OOBIENT_B5RF ON OOBiEntA (BIENT_B1RF);
CREATE INDEX I_OOBIENT_B5RM ON OOBiEntA (BIENT_B1RM);
CREATE INDEX I_OOCRDNT_B ON OOCardEntA (B_ID);
CREATE INDEX I_NPTBTTY_B ON OONoOptBiEntityA (B_ID);
CREATE INDEX I_NPTNTTY_B ON OONoOptEntityA (B_ID);
CREATE INDEX I_OOUNINT_B1 ON OOUniEntA (UNIENT_B1);
CREATE INDEX I_OOUNINT_B2 ON OOUniEntA (B2_ID);
CREATE INDEX I_OOUNINT_B4 ON OOUniEntA (B4_ID);
CREATE INDEX I_OOUNINT_B5CA ON OOUniEntA (B5CA_ID);
CREATE INDEX I_OOUNINT_B5CM ON OOUniEntA (B5CM_ID);
CREATE INDEX I_OOUNINT_B5CP ON OOUniEntA (B5CP_ID);
CREATE INDEX I_OOUNINT_B5RF ON OOUniEntA (B5RF_ID);
CREATE INDEX I_OOUNINT_B5RM ON OOUniEntA (B5RM_ID);
CREATE INDEX I_XMLMDNT_IDENTITY ON XMLEmbedIDOOEntA (identity_id, identity_country);
CREATE INDEX I_XMLDTTY_IDENTITY ON XMLIDClassOOEntityA (identity_id, identity_country);
CREATE INDEX I_XMLBDNT_B ON XMLOOBiCardEntA (B_ID);
CREATE INDEX I_XMLOBNT_B1 ON XMLOOBiEntA (XMLBIENT_B1);
CREATE INDEX I_XMLOBNT_B2 ON XMLOOBiEntA (B2_ID);
CREATE INDEX I_XMLOBNT_B4 ON XMLOOBiEntA (B4_ID);
CREATE INDEX I_XMLOBNT_B5CA ON XMLOOBiEntA (B5CA_ID);
CREATE INDEX I_XMLOBNT_B5CM ON XMLOOBiEntA (B5CM_ID);
CREATE INDEX I_XMLOBNT_B5CP ON XMLOOBiEntA (B5CP_ID);
CREATE INDEX I_XMLOBNT_B5RF ON XMLOOBiEntA (B5RF_ID);
CREATE INDEX I_XMLOBNT_B5RM ON XMLOOBiEntA (B5RM_ID);
CREATE INDEX I_XMLCDNT_B ON XMLOOCardEntA (B_ID);
CREATE INDEX I_XMLNTTY_B ON XMLOONoOptBiEntityA (B_ID);
CREATE INDEX I_XMLNTTY_B1 ON XMLOONoOptEntityA (B_ID);
CREATE INDEX I_XMLUNNT_B1 ON XMLOOUniEntA (UNIENT_B1);
CREATE INDEX I_XMLUNNT_B2 ON XMLOOUniEntA (B2_ID);
CREATE INDEX I_XMLUNNT_B4 ON XMLOOUniEntA (B4_ID);
CREATE INDEX I_XMLUNNT_B5CA ON XMLOOUniEntA (B5CA_ID);
CREATE INDEX I_XMLUNNT_B5CM ON XMLOOUniEntA (B5CM_ID);
CREATE INDEX I_XMLUNNT_B5CP ON XMLOOUniEntA (B5CP_ID);
CREATE INDEX I_XMLUNNT_B5RF ON XMLOOUniEntA (B5RF_ID);
CREATE INDEX I_XMLUNNT_B5RM ON XMLOOUniEntA (B5RM_ID);