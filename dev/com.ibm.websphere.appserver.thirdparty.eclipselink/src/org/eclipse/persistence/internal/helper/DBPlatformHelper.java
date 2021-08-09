/*******************************************************************************
 * Copyright (c) 1998, 2019 Oracle and/or its affiliates, IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.helper;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.persistence.logging.SessionLog;


/**
 * @author Mitesh Meswani
 * This class is responsible to translate given database name to a DatabasePlatform.
 */
public class DBPlatformHelper {
    private static final String DEFAULTPLATFORM  = "org.eclipse.persistence.platform.database.DatabasePlatform"; // NOI18N

    private final static String PROPERTY_PATH = "org/eclipse/persistence/internal/helper/"; // NOI18N

    private final static String VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME =
        PROPERTY_PATH + "VendorNameToPlatformMapping.properties";  //NOI18N

    /**
     * Holds mapping between possible vendor names to internal platforms defined above.
     * vendor names are treated as regular expressions.
     */
    private static List<String[]> _nameToVendorPlatform = null;

    /** Get Database Platform from vendor name.
     * @param vendorName Input vendor name. Typically this is obtained by querying
     * <code>DatabaseMetaData</code>.
     * @param logger The logger.
     * @return Database platform that corresponds to <code>vendorName</code>.
     * If vendorName does not match any of predefined vendor names, <code>
     * DEFAULTPLATFORM </code> is returned.
     */
    public static String getDBPlatform(String vendorName, String minorVersion, String majorVersion, SessionLog logger) {

        initializeNameToVendorPlatform(logger);

        vendorName = (vendorName == null) ? "Vendor Not Found" : vendorName;
        minorVersion = (minorVersion == null) ? "0" : minorVersion;
        majorVersion = (majorVersion == null) ? "0" : majorVersion;

        String vendor = vendorName + "[" + minorVersion + ", " + majorVersion + "]";

        String detectedDbPlatform = matchVendorNameInProperties(vendor, _nameToVendorPlatform, logger);
        if (logger.shouldLog(SessionLog.FINE) ) {
            logger.log(SessionLog.FINE, SessionLog.CONNECTION, "dbPlatformHelper_detectedVendorPlatform", detectedDbPlatform ); // NOI18N
        }
        if (detectedDbPlatform == null) {
            if(logger.shouldLog(SessionLog.INFO)) {
                logger.log(SessionLog.INFO, SessionLog.CONNECTION, "dbPlatformHelper_defaultingPlatform",  vendor, DEFAULTPLATFORM); // NOI18N
            }
            detectedDbPlatform = DEFAULTPLATFORM;
        }
        return detectedDbPlatform;
    }

    //Placeholder method for compatibility
    /** Get Database Platform from vendor name.
     * @param vendorName Input vendor name. Typically this is obtained by querying
     * <code>DatabaseMetaData</code>.
     * @param logger The logger.
     * @return Database platform that corresponds to <code>vendorName</code>.
     * If vendorName does not match any of predefined vendor names, <code>
     * DEFAULTPLATFORM </code> is returned.
     */
    public static String getDBPlatform(String vendorName, SessionLog logger) {

        initializeNameToVendorPlatform(logger);

        String detectedDbPlatform = null;
        if(vendorName != null) {
            detectedDbPlatform = matchVendorNameInProperties(vendorName, _nameToVendorPlatform, logger);
        }
        if (logger.shouldLog(SessionLog.FINE) ) {
            logger.log(SessionLog.FINE, SessionLog.CONNECTION, "dbPlatformHelper_detectedVendorPlatform", detectedDbPlatform ); // NOI18N
        }
        if (detectedDbPlatform == null) {
            if(logger.shouldLog(SessionLog.INFO)) {
                logger.log(SessionLog.INFO, SessionLog.CONNECTION, "dbPlatformHelper_defaultingPlatform",  vendorName, DEFAULTPLATFORM); // NOI18N
            }
            detectedDbPlatform = DEFAULTPLATFORM;
        }
        return detectedDbPlatform;
    }

    /**
     * Allocate and initialize nameToVendorPlatform if not already done.
     */
    private static List<String[]> initializeNameToVendorPlatform(SessionLog logger) {
        synchronized(DBPlatformHelper.class) {
            if(_nameToVendorPlatform == null) {
                _nameToVendorPlatform = new ArrayList<String[]>();
                try {
                    loadFromResource(_nameToVendorPlatform, VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME,
                                            DBPlatformHelper.class.getClassLoader() );
                } catch (IOException e) {
                    logger.log(SessionLog.WARNING, SessionLog.CONNECTION, "dbPlatformHelper_noMappingFound", VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME);
                }
            }
        }
        return _nameToVendorPlatform;
    }

    /**
     * Match vendorName in properties specified by _nameToVendorPlatform.
     */
    private static String matchVendorNameInProperties(String vendorName,
            List<String[]> nameToVendorPlatform, SessionLog logger) {
        String dbPlatform = null;
        //Iterate over all properties till we find match.
        for( Iterator<String[]> iterator = nameToVendorPlatform.iterator();
                dbPlatform == null && iterator.hasNext();) {
            String[] entry = iterator.next();
            String regExpr = entry[0];
            String value = entry[1];
            if (logger.shouldLog(SessionLog.FINEST)) {
                logger.log(SessionLog.FINEST, SessionLog.CONNECTION, "dbPlatformHelper_regExprDbPlatform", regExpr,
                    value); // NOI18N
            }
            if( matchPattern(regExpr, vendorName, logger) ) {
                dbPlatform = value;
            }
        }
        return dbPlatform;
    }

   /** Matches target to pattern specified regExp. Returns false if there is
    * any error compiling regExp.
    * @param regExp The regular expression.
    * @param target The target against which we are trying to match regExp.
    * @param logger
    * @return false if there is error compiling regExp or target does not
    * match regExp. true if regExp matches pattern.
    */
    private static boolean matchPattern(String regExp, String target, SessionLog logger) {
        boolean matches = false;
        try {
            matches = Pattern.matches(regExp,target);
        } catch (PatternSyntaxException e){
            if(logger.shouldLog(SessionLog.FINE)) {
                logger.log(SessionLog.FINE, SessionLog.CONNECTION, "dbPlatformHelper_patternSyntaxException", e); // NOI18N
            }
        }
        return matches;
    }

    //-----Property Loading helper methods ----/
    private static void loadFromResource(List<String[]> properties, String resourceName, ClassLoader classLoader)
            throws IOException {
        load(properties, resourceName, classLoader);
    }

    /**
     * Loads properties list from the specified resource
     * into specified Properties object.
     * @param properties	Properties object to load
     * @param resourceName	Name of resource.
     *                      If loadFromFile  is true, this is fully qualified path name to a file.
     *                      param classLoader is ignored.
     *                      If loadFromFile  is false,this is resource name.
     * @param classLoader   The class loader that should be used to load the resource. If null,primordial
     *                      class loader is used.
     */
    private static void load(List<String[]> properties, final String resourceName,
            final ClassLoader classLoader)
                            throws IOException {

        BufferedReader bin = new BufferedReader(new InputStreamReader(openResourceInputStream(resourceName,classLoader)));

        try {
            for (String line = bin.readLine(); line != null; line = bin.readLine()) {
                String[] keyValue = validateLineForReturnAsKeyValueArray(line);
                if (keyValue != null) {
                    properties.add(keyValue);
                }
            }
        } finally {
            try {
                bin.close();
            } catch (Exception e) {
                // no action
            }
        }
    }

    /**
     * Open resourceName as input stream inside doPriviledged block
     */
    private static InputStream openResourceInputStream(final String resourceName, final ClassLoader classLoader) {
        return (InputStream) AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    if (classLoader != null) {
                        return classLoader.getResourceAsStream(resourceName);
                    } else {
                        return ClassLoader.getSystemResourceAsStream(resourceName);
                    }
                }
            }
        );
    }
    
    private static String[] validateLineForReturnAsKeyValueArray(String line) {
        if (line == null || line.length() == 0) {
            return null;
        }
        // trim leading AND trailing space
        line = line.trim();
        
        // check for comment and empty line
        if (line.length() == 0 || line.startsWith("#")) {
            return null;
        }
        
        // check line contains valid properties '=' separator        
        int indexOfEquals = line.indexOf('=');
        if (indexOfEquals == -1) {
            return null;
        }
        String key = line.substring(0, indexOfEquals);
        String value = line.substring(indexOfEquals + 1, line.length());
        return new String[] { key, value };
    }

}
