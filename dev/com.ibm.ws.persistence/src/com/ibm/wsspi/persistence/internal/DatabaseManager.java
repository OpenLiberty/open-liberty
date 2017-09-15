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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.internal.databaseaccess.DatabasePlatform;
import org.eclipse.persistence.internal.helper.DBPlatformHelper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.sessions.SessionEventListener;
import org.eclipse.persistence.sessions.SessionEventManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.persistence.internal.eclipselink.PsSessionEventListener;
import com.ibm.wsspi.persistence.internal.eclipselink.TraceLog;

/**
 * A manager for all things database.
 */
public class DatabaseManager {
    private final static TraceComponent tc = Tr.register(DatabaseManager.class);

    // A set of database products that we know do not support passing unicode values
    private static Set<Pattern> _noUnicodeSupportPlatform;

    // A set of database products that we know support passing unicode values
    private static Set<Pattern> _unicodeSupportPlatform;

    static {
        _noUnicodeSupportPlatform = Collections.newSetFromMap(new ConcurrentHashMap<Pattern, Boolean>());
        _noUnicodeSupportPlatform.add(Pattern.compile("(?i)(.)*informix(.)*"));

        _unicodeSupportPlatform = Collections.newSetFromMap(new ConcurrentHashMap<Pattern, Boolean>());
        /**
         * It is quite possible that this isn't restrictive enough. Are there old versions of DB2
         * / Derby where we should be filtering?
         */
        _unicodeSupportPlatform.add(Pattern.compile("(?i)(.)*db2(.)*"));
        _unicodeSupportPlatform.add(Pattern.compile("(?i)(.)*derby(.)*"));
    }

    public DatabasePlatform getPlatform(EntityManagerFactory emf) {
        return emf.unwrap(DatabaseSessionImpl.class).getPlatform();
    }

    public boolean isOracle(EntityManagerFactory emf) {
        return getPlatform(emf).isOracle();
    }

    /**
     * Checks if the Database/driver backing the provided emf is able to handle unicode values. If
     * unicode support is detected, the {@link com.ibm.wsspi.persistence.internal.eclipselink.PsSessionEventListener} unicode filer
     * will be removed. Support for unicode checking comes in three flavors (in order of
     * precedence):
     * <p>
     * <li>
     * Consult {@link com.ibm.wsspi.persistence.PersistenceServiceUnitConfig#getAllowUnicode()}. If
     * this value is non-null, it wins.
     * <li>
     * Consult a static list of supported/unsupported platforms.
     * <li>Invoke {@link org.eclipse.persistence.internal.databaseaccess.DatabasePlatform#shouldUseGetSetNString()} to see if the Platform supports calling get/setNString
     */
    @FFDCIgnore(Exception.class)
    public void processUnicodeSettings(EntityManagerFactory emf, PersistenceServiceUnitConfig conf) {
        DataSource ds = conf.getJtaDataSource();
        // If the consumer configured unicode, or no datasource noop.
        if (conf.getAllowUnicode() != null || ds == null) {
            return;
        }

        DatabasePlatform db = null;
        AbstractSession session = null;
        try {
            // This call will attempt to login. If we don't have a valid datasource this call
            // will blow up. Need to think more about refactoring / adding another user facing
            // method to avoid doing this at all in cases of ddlGeneration
            db = emf.unwrap(DatabaseSessionImpl.class).getPlatform();
            session = emf.unwrap(AbstractSession.class);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "caught an exception trying to unwrap an emf. Possibly no connection?", new Object[] { e });
            }
            return;
        }
        Boolean supports = supportsUnicodeStaticCheck(ds);
        if (supports == null) {
            // No configuration so we're going to defer to the detected/configured platform.
            // Update config if the platform says that it supports calling get/setNString().
            // Don't do anything if not as filtering is on by default.
            if (db.shouldUseGetSetNString()) {
                supports = Boolean.TRUE;
            }
        }

        // If this platform supports unicode, we need to remove the filtering.
        if (Boolean.TRUE.equals(supports)) {
            // create copy to avoid ConcurrentModificationException
            SessionEventManager sem = session.getEventManager();
            for (SessionEventListener sel : new ArrayList<SessionEventListener>(sem.getListeners())) {
                if (sel instanceof PsSessionEventListener) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "removing unicode filter", new Object[] { sel });
                    }
                    ((PsSessionEventListener) sel).removeWrappingConnector(session);
                    sem.removeListener(sel);
                }
            }
        }
    }

    /**
     * Returns the EclipseLink database platform class name based on the properties passed in or by
     * detecting it through the connection, if one is available.
     */
    public String getDatabasePlatformClassName(PUInfoImpl pui) {
        SessionLog traceLogger = new TraceLog();

        Properties properties = pui.getProperties();
        String productName = properties.getProperty(PersistenceUnitProperties.SCHEMA_DATABASE_PRODUCT_NAME);

        String vendorNameAndVersion = null;
        // check persistent properties
        if (productName != null) {
            vendorNameAndVersion = productName;

            String majorVersion = properties.getProperty(PersistenceUnitProperties.SCHEMA_DATABASE_MAJOR_VERSION);
            if (majorVersion != null) {
                vendorNameAndVersion += majorVersion;
                String minorVersion = properties.getProperty(PersistenceUnitProperties.SCHEMA_DATABASE_MINOR_VERSION);
                if (minorVersion != null) {
                    vendorNameAndVersion += minorVersion;
                }
            }
        } else {
            vendorNameAndVersion = getVendorNameAndVersion(pui.getJtaDataSource());
            if (vendorNameAndVersion == null) {
                getVendorNameAndVersion(pui.getNonJtaDataSource());
            }
        }

        return DBPlatformHelper.getDBPlatform(vendorNameAndVersion, traceLogger);
    }

    /**
     * Consults the static collections contained within this class and attempts to determine
     * whether the provided DataSource supports unicode.
     * 
     * @return True if ds supports unicode. False if ds doesn't support unicode. null if unknown.
     */
    @FFDCIgnore(SQLException.class)
    private Boolean supportsUnicodeStaticCheck(DataSource ds) {
        Boolean res = null;
        try {
            Connection conn = ds.getConnection();
            String product = null;
            try {
                DatabaseMetaData dmd = conn.getMetaData();
                product = dmd.getDatabaseProductName();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "supportsUnicodeStaticCheck : getDatabaseProductName = " + product);
                }
            } finally {
                conn.close();
            }
            for (Pattern supportedPattern : _unicodeSupportPlatform) {
                if (supportedPattern.matcher(product).matches()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "matched _unicodeSupportPlatform=" + supportedPattern.pattern());
                    }
                    return Boolean.TRUE;
                }
            }
            for (Pattern unsupported : _noUnicodeSupportPlatform) {
                if (unsupported.matcher(product).matches()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "matched _noUnicodeSupportPlatform=" + unsupported.pattern());
                    }
                    return Boolean.FALSE;
                }
            }
        } catch (SQLException e) {
            // Unexpected
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Something went wrong in supportsUnicodeStaticCheck() -- ", e);
            }
        }
        return res;
    }

    private String getVendorNameAndVersion(DataSource ds) {
        if (ds == null) {
            return null;
        }
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            return dmd.getDatabaseProductName() + dmd.getDatabaseMajorVersion();
        } catch (SQLException sqle) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to retrieve connection in getDatabasePlatformClassName", sqle);
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqlee) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to retrieve connection in getDatabasePlatformClassName", sqlee);
                }
            }
        }
        return null;
    }
}
