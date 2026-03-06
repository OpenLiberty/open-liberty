/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.war.annotatedbasic.deferred;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This bean will read LDAP identity store configuration settings from a well-known file
 * allowing tests to update the LDAP identity store dynamically by simply updating the
 * well-known file.
 */

@Named
@ApplicationScoped
public class LdapSettingsBean {
    private static final String CLASS_NAME = LdapSettingsBean.class.getName();
    private static final String PROPS_FILE = "LdapSettingsBean.props";
    
    private Properties props;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile long lastModified = 0;
    private volatile boolean initialized = false;

    public LdapSettingsBean() {
    }

    @PostConstruct
    public void init() {
        try {
            loadConfiguration();
            initialized = true;
        } catch (IOException e) {
            System.err.println(CLASS_NAME + ".init() failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load configuration from file. This method is synchronized to prevent
     * concurrent file access during CDI initialization.
     */
    private void loadConfiguration() throws IOException {
        lock.writeLock().lock();
        try {
            props = new Properties();
            FileReader fr = null;
            try {
                java.io.File file = new java.io.File(PROPS_FILE);
                fr = new FileReader(file);
                props.load(fr);
                lastModified = file.lastModified();
                System.out.println(CLASS_NAME + ".loadConfiguration() loaded properties from " + PROPS_FILE);
            } finally {
                if (fr != null) {
                    fr.close();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if the properties file has been modified and reload if necessary.
     */
    private void checkAndReloadIfModified() {
        try {
            java.io.File file = new java.io.File(PROPS_FILE);
            long currentModified = file.lastModified();
            
            if (currentModified != lastModified) {
                System.out.println(CLASS_NAME + ".checkAndReloadIfModified() detected file change, reloading...");
                loadConfiguration();
            }
        } catch (IOException e) {
            System.err.println(CLASS_NAME + ".checkAndReloadIfModified() failed: " + e.getMessage());
        }
    }

    /**
     * Get property value with read lock protection.
     */
    private String getProperty(String prop) {
        if (!initialized) {
            System.err.println(CLASS_NAME + ".getProperty() called before initialization for: " + prop);
            return null;
        }
        
        // Check for file modifications before reading
        checkAndReloadIfModified();
        
        lock.readLock().lock();
        try {
            String value = props.getProperty(prop);
            return "null".equalsIgnoreCase(value) ? null : value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getBindDn() {
        String prop = getProperty("bindDn");
        System.out.println(CLASS_NAME + ".getBindDn() returns: " + prop);
        return prop;
    }

    @Trivial
    public String getBindDnPassword() {
        return getProperty("bindDnPassword");
    }

    public String getCallerBaseDn() {
        String prop = getProperty("callerBaseDn");
        System.out.println(CLASS_NAME + ".getCallerBaseDn() returns: " + prop);
        return prop;
    }

    public String getCallerNameAttribute() {
        String prop = getProperty("callerNameAttribute");
        System.out.println(CLASS_NAME + ".getCallerNameAttribute() returns: " + prop);
        return prop;
    }

    public String getCallerSearchBase() {
        String prop = getProperty("callerSearchBase");
        System.out.println(CLASS_NAME + ".getCallerSearchBase() returns: " + prop);
        return prop;
    }

    public String getCallerSearchFilter() {
        String prop = getProperty("callerSearchFilter");
        System.out.println(CLASS_NAME + ".getCallerSearchFilter() returns: " + prop);
        return prop;
    }

    public LdapSearchScope getCallerSearchScope() {
        String prop = getProperty("callerSearchScope");
        LdapSearchScope result = null;
        if (prop != null) {
            if ("SUBTREE".equalsIgnoreCase(prop)) {
                result = LdapSearchScope.SUBTREE;
            } else {
                result = LdapSearchScope.ONE_LEVEL;
            }
        }
        System.out.println(CLASS_NAME + ".getCallerSearchScope() returns: " + result);
        return result;
    }

    public String getGroupMemberAttribute() {
        String prop = getProperty("groupMemberAttribute");
        System.out.println(CLASS_NAME + ".getGroupMemberAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupMemberOfAttribute() {
        String prop = getProperty("groupMemberOfAttribute");
        System.out.println(CLASS_NAME + ".getGroupMemberOfAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupNameAttribute() {
        String prop = getProperty("groupNameAttribute");
        System.out.println(CLASS_NAME + ".getGroupNameAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupSearchBase() {
        String prop = getProperty("groupSearchBase");
        System.out.println(CLASS_NAME + ".getGroupSearchBase() returns: " + prop);
        return prop;
    }

    public String getGroupSearchFilter() {
        String prop = getProperty("groupSearchFilter");
        System.out.println(CLASS_NAME + ".getGroupSearchFilter() returns: " + prop);
        return prop;
    }

    public LdapSearchScope getGroupSearchScope() {
        String prop = getProperty("groupSearchScope");
        LdapSearchScope result = null;
        if (prop != null) {
            if ("SUBTREE".equalsIgnoreCase(prop)) {
                result = LdapSearchScope.SUBTREE;
            } else {
                result = LdapSearchScope.ONE_LEVEL;
            }
        }
        System.out.println(CLASS_NAME + ".getGroupSearchScope() returns: " + result);
        return result;
    }

    public Integer getMaxResults() {
        String prop = getProperty("maxResults");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }
        System.out.println(CLASS_NAME + ".getMaxResults() returns: " + result);
        return result;
    }

    public Integer getPriority() {
        String prop = getProperty("priority");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }
        System.out.println(CLASS_NAME + ".getPriority() returns: " + result);
        return result;
    }

    public Integer getReadTimeout() {
        String prop = getProperty("readTimeout");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }
        System.out.println(CLASS_NAME + ".getReadTimeout() returns: " + result);
        return result;
    }

    public String getUrl() {
        String prop = getProperty("url");
        System.out.println(CLASS_NAME + ".getUrl() returns: " + prop);
        return prop;
    }

    public ValidationType[] getUseFor() {
        Set<ValidationType> resultsSet = new HashSet<ValidationType>();
        String prop = getProperty("useFor");
        if (prop != null) {
            if (prop.contains("VALIDATE")) {
                resultsSet.add(ValidationType.VALIDATE);
            }
            if (prop.contains("PROVIDE_GROUPS")) {
                resultsSet.add(ValidationType.PROVIDE_GROUPS);
            }
        }
        ValidationType[] results = null;
        if (resultsSet.size() > 0) {
            results = resultsSet.toArray(new ValidationType[resultsSet.size()]);
        }
        System.out.println(CLASS_NAME + ".getUseFor() returns: " + Arrays.toString(results));
        return results;
    }
}