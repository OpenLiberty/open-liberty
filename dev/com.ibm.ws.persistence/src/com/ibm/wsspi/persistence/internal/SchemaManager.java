/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.transaction.TransactionManager;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.platform.database.DB2Platform;
import org.eclipse.persistence.platform.database.DerbyPlatform;
import org.eclipse.persistence.platform.database.Informix11Platform;
import org.eclipse.persistence.platform.database.JavaDBPlatform;
import org.eclipse.persistence.platform.database.OraclePlatform;
import org.eclipse.persistence.platform.database.SQLServerPlatform;
import org.eclipse.persistence.sessions.DatabaseSession;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.internal.eclipselink.PsPersistenceProvider;

/**
 * A manager for all things schema generation.
 */
class SchemaManager {

    private static final Map<String, String> platformTerminationToken = new ConcurrentHashMap<String, String>();
    static {
        platformTerminationToken.put(DB2Platform.class.getCanonicalName(), ";");
        platformTerminationToken.put(Informix11Platform.class.getCanonicalName(), ";");
        // Gross-ness of having a space rather than empty string. Could/should fix in Eclipselink
        // JavaDBPlaform==Derby
        platformTerminationToken.put(JavaDBPlatform.class.getCanonicalName(), " ");
        platformTerminationToken.put(DerbyPlatform.class.getCanonicalName(), " ");
        platformTerminationToken.put(OraclePlatform.class.getCanonicalName(), ";");
        // See org.eclipse.persistence.sessions.DatasourceLogin.setPlatformClassName(..)
        platformTerminationToken.put("org.eclipse.persistence.platform.database.oracle.OraclePlatform", ";");
        // Even though these platform classes aren't used at runtime, they get detected as the plaform class
        // from VendorNameToPlatformMapping.properties. Map ALL oracle regexs to ";"
        platformTerminationToken.put("org.eclipse.persistence.platform.database.oracle.Oracle9Platform", ";");
        platformTerminationToken.put("org.eclipse.persistence.platform.database.oracle.Oracle10Platform", ";");
        platformTerminationToken.put("org.eclipse.persistence.platform.database.oracle.Oracle11Platform", ";");
        platformTerminationToken.put("org.eclipse.persistence.platform.database.oracle.Oracle12Platform", ";");
        // Don't set Sybase into this map as the value in SybasePlatform is correct. This is working around a bug
        // where we aren't able to set whitespace characters into a Platform via PropertiesUtils.
        // platformTerminationToken.put(SybasePlatform.class.getCanonicalName(), "\ngo");
        platformTerminationToken.put(SQLServerPlatform.class.getCanonicalName(), ";");
        platformTerminationToken.put("org.eclipse.persistence.platform.database.MySQLPlatform", ";");
    }

    // Care needs to be taken to not modify this map as it is shared with the
    // PersistenceServiceUnitImpl.
    private final Map<String, String> _serviceProperties;
    private final PUInfoImpl _pui;
    private final PsPersistenceProvider _provider;
    private final DatabaseManager _dbMgr;

    SchemaManager(Map<String, String> props, PUInfoImpl pui, PsPersistenceProvider provider, DatabaseManager dbMgr) {
        _pui = pui;
        _serviceProperties = props;
        _provider = provider;
        _dbMgr = dbMgr;
    }

    void generateSchema(TransactionManager tranMgr, Object... schemaGenkeyValuePair) {
        Map<Object, Object> props = new HashMap<Object, Object>();
        for (int i = 0; i < schemaGenkeyValuePair.length; i += 2) {
            props.put(schemaGenkeyValuePair[i], schemaGenkeyValuePair[i + 1]);
        }
        props.putAll(_serviceProperties);

        Writer writer = (Writer) props.get(PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET);
        if (writer != null) {
            // replace the provided writer with our own implementation that ignores close()
            // calls. This allows us to add some extra bits onto the end of the writer prior to
            // really closing
            writer = new DelegatingWriter(writer, false);
            props.put(PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET, writer);
        }

        // TODO(151909) -- Need to investigate via an EclipseLink bug.
        // props.put(SchemaGeneration.CONNECTION, _schemaDataSource);

        // Pass a copy of the PUI to the provider. The copy will have a new name. The provider
        // seems to ignore repeated requests with the same name.
        PersistenceUnitInfo puiCopy = _pui.createCopyWithNewName();

        // If the database platform being used needs a special termination token at the end of SQL
        // statements in the DDL files, override it using the target database properties.
        overrideDatabaseTerminationToken(props);

        EntityManagerFactory emf = null;
        try {
            // Don't use PersistenceProvider.generateSchema method here. We need a reference to a
            // emf so we can determine the database that we're generating for and write script
            // terminators if necessary.
            emf = _provider.createContainerEMF(puiCopy, props, false);
            if (writer != null) {
                postProcess(emf, writer);
            } else if (tranMgr != null) {
                // Begin a transaction to effect table generator row population
                // Technique described in https://bugs.eclipse.org/bugs/show_bug.cgi?id=356256
                EntityManager em = null;
                try {
                    tranMgr.begin();
                    em = emf.createEntityManager();
                    DatabaseSession ss = em.unwrap(DatabaseSession.class);
                    (new org.eclipse.persistence.tools.schemaframework.SchemaManager(ss)).createSequences();
                } catch (Exception e) {
                    throw new PersistenceException(e);
                } finally {
                    if (em != null) {
                        try {
                            em.close();
                        } catch (Throwable t) {
                            // Swallow
                        }
                    }

                    try {
                        tranMgr.commit();
                    } catch (Exception e) {
                        throw new PersistenceException(e);
                    }
                }
            }
        } finally {
            if (emf != null) {
                emf.close();
            }
        }

        if (writer != null) {
            try {
                ((DelegatingWriter) writer).closeInternal();
            } catch (IOException e) {
                throw new PersistenceException(ValidationException.fileError(e));
            }
        }
    }

    /**
     * Helper method that will override the termination token if the database detected is in our
     * platformTerminationToken list.
     * 
     */
    private void overrideDatabaseTerminationToken(Map<Object, Object> props) {
        String overrideTermToken = null;
        String platformClassName = _dbMgr.getDatabasePlatformClassName(_pui);

        if (platformClassName != null) {
            overrideTermToken = platformTerminationToken.get(platformClassName);
        }

        if (overrideTermToken != null) {
            String existing = (String) props.get(PersistenceUnitProperties.TARGET_DATABASE_PROPERTIES);
            if (existing != null) {
                existing = existing + ",";
            } else {
                existing = "";
            }
            existing = (existing + "StoredProcedureTerminationToken=" + overrideTermToken);

            props.put(PersistenceUnitProperties.TARGET_DATABASE_PROPERTIES, existing);
        }
    }

    private void postProcess(EntityManagerFactory emf, Writer writer) {
        if (_dbMgr.isOracle(emf)) {
            try {
                writer.write("\nEXIT;");
            } catch (IOException e) {
                throw new PersistenceException(ValidationException.fileError(e));
            }
        }
    }

    @Trivial
    private static class DelegatingWriter extends Writer {
        final Writer _del;
        final boolean _ignoreClose;

        public DelegatingWriter(Writer del, boolean ignoreClose) {
            _del = del;
            _ignoreClose = ignoreClose;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            _del.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            _del.flush();
        }

        @Override
        public void close() throws IOException {
            if (_ignoreClose) {
                _del.close();
            }
        }

        public void closeInternal() throws IOException {
            _del.close();
        }
    }
}
