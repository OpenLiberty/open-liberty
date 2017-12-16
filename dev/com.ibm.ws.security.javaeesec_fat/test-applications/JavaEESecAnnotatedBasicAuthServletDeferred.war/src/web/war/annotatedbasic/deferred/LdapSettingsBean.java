/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.war.annotatedbasic.deferred;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

/**
 * This bean will read LDAP identity store configuration settings from a well-known file
 * allowing tests to update the LDAP identity store dynamically by simply updating the
 * well-known file.
 */
@Named
@ApplicationScoped
public class LdapSettingsBean {
    private static final String CLASS_NAME = LdapSettingsBean.class.getName();

    private Properties props;

    public LdapSettingsBean() {}

    public String getBindDn() throws IOException {
        refreshConfiguration();

        String prop = getProperty("bindDn");
        System.out.println(CLASS_NAME + ".getBindDn() returns: " + prop);
        return prop;
    }

    public String getBindDnPassword() throws IOException {
        refreshConfiguration();

        String prop = getProperty("bindDnPassword");
        System.out.println(CLASS_NAME + ".getBindDnPassword() returns: " + prop);
        return prop;
    }

    public String getCallerBaseDn() throws IOException {
        refreshConfiguration();

        String prop = getProperty("callerBaseDn");
        System.out.println(CLASS_NAME + ".getCallerBaseDn() returns: " + prop);
        return prop;
    }

    public String getCallerNameAttribute() throws IOException {
        refreshConfiguration();

        String prop = getProperty("callerNameAttribute");
        System.out.println(CLASS_NAME + ".getCallerNameAttribute() returns: " + prop);
        return prop;
    }

    public String getCallerSearchBase() throws IOException {
        refreshConfiguration();

        String prop = getProperty("callerSearchBase");
        System.out.println(CLASS_NAME + ".getCallerSearchBase() returns: " + prop);
        return prop;
    }

    public String getCallerSearchFilter() throws IOException {
        refreshConfiguration();

        String prop = getProperty("callerSearchFilter");
        System.out.println(CLASS_NAME + ".getCallerSearchFilter() returns: " + prop);
        return prop;
    }

    public LdapSearchScope getCallerSearchScope() throws IOException {
        refreshConfiguration();

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

    public String getGroupMemberAttribute() throws IOException {
        refreshConfiguration();

        String prop = getProperty("groupMemberAttribute");
        System.out.println(CLASS_NAME + ".getGroupMemberAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupMemberOfAttribute() throws IOException {
        refreshConfiguration();

        String prop = getProperty("groupMemberOfAttribute");
        System.out.println(CLASS_NAME + ".getGroupMemberOfAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupNameAttribute() throws IOException {
        refreshConfiguration();

        String prop = getProperty("groupNameAttribute");
        System.out.println(CLASS_NAME + ".getGroupNameAttribute() returns: " + prop);
        return prop;
    }

    public String getGroupSearchBase() throws IOException {
        refreshConfiguration();

        String prop = getProperty("groupSearchBase");
        System.out.println(CLASS_NAME + ".getGroupSearchBase() returns: " + prop);
        return prop;
    }

    public String getGroupSearchFilter() throws IOException {
        refreshConfiguration();

        String prop = getProperty("groupSearchFilter");
        System.out.println(CLASS_NAME + ".getGroupSearchFilter() returns: " + prop);
        return prop;
    }

    public LdapSearchScope getGroupSearchScope() throws IOException {
        refreshConfiguration();

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

    public Integer getPriority() throws IOException {
        refreshConfiguration();

        String prop = getProperty("priority");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }

        System.out.println(CLASS_NAME + ".getPriority() returns: " + result);
        return result;
    }

    public Integer getReadTimeout() throws IOException {
        refreshConfiguration();

        String prop = getProperty("readTimeout");
        Integer result = null;
        if (prop != null) {
            result = Integer.valueOf(prop);
        }

        System.out.println(CLASS_NAME + ".getReadTimeout() returns: " + result);
        return result;
    }

    public String getUrl() throws IOException {
        refreshConfiguration();

        String prop = getProperty("url");
        System.out.println(CLASS_NAME + ".getUrl() returns: " + prop);
        return prop;
    }

    public Set<ValidationType> getUseFor() throws IOException {
        refreshConfiguration();

        String prop = getProperty("useFor");
        Set<ValidationType> results = null;
        if (prop != null) {
            results = new HashSet<ValidationType>();

            if (prop.contains("VALIDATE")) {
                results.add(ValidationType.VALIDATE);
            }
            if (prop.contains("PROVIDE_GROUPS")) {
                results.add(ValidationType.PROVIDE_GROUPS);
            }
        }

        System.out.println(CLASS_NAME + ".getUseFor() returns: " + results);
        return results;
    }

    private void refreshConfiguration() throws IOException {
        props = new Properties();
        props.load(new FileReader("LdapSettingsBean.props"));
    }

    /**
     * Common logic for returning a property. If the property's value is a string "null",
     * return null. This will allow testing null handling from beans.
     *
     * @param prop
     * @return
     */
    private String getProperty(String prop) {
        String value = props.getProperty(prop);
        return "null".equalsIgnoreCase(value) ? null : value;
    }
}
