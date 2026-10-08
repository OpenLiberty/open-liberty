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

    private Properties props = new Properties();

    public LdapSettingsBean() {
    }

    /**
     * Perform the initial file load after CDI construction is complete.
     * Doing I/O here rather than in the constructor prevents z/OS CDI
     * initialization hangs caused by blocking I/O during bean wiring.
     */
    @PostConstruct
    public void init() {
        try {
            refreshConfiguration();
        } catch (IOException e) {
            System.err.println(CLASS_NAME + ".init() failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Reload the properties file from disk.
     * Called both at startup (via @PostConstruct) and on every property access,
     * so tests always see the latest values written by updateLdapSettingsBean().
     */
    private void refreshConfiguration() throws IOException {
        Properties p = new Properties();
        FileReader fr = new FileReader(PROPS_FILE);
        try {
            p.load(fr);
        } finally {
            fr.close();
        }
        props = p;
    }

    /**
     * Return the named property, reloading the file first so tests always get
     * the current configuration. Returns null if the stored value is the
     * sentinel string "null", allowing tests to drive null-handling paths.
     */
    private String getProperty(String prop) {
        try {
            refreshConfiguration();
        } catch (IOException e) {
            System.err.println(CLASS_NAME + ".getProperty() failed to refresh configuration for '" + prop + "': " + e.getMessage());
        }
        String value = props.getProperty(prop);
        return "null".equalsIgnoreCase(value) ? null : value;
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
