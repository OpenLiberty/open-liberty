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

    private Properties props;

    public LdapSettingsBean() {}

    public String getBindDn() throws IOException {
        refereshConfiguration();
        return props.getProperty("bindDn");
    }

    public String getBindDnPassword() throws IOException {
        refereshConfiguration();
        return props.getProperty("bindDnPassword");
    }

    public String getCallerBaseDn() throws IOException {
        refereshConfiguration();
        return props.getProperty("callerBaseDn");
    }

    public String getCallerNameAttribute() throws IOException {
        refereshConfiguration();
        return props.getProperty("callerNameAttribute");
    }

    public String getCallerSearchBase() throws IOException {
        refereshConfiguration();
        return props.getProperty("callerSearchBase");
    }

    public String getCallerSearchFilter() throws IOException {
        refereshConfiguration();
        return props.getProperty("callerSearchFilter");
    }

    public LdapSearchScope getCallerSearchScope() throws IOException {
        refereshConfiguration();

        String prop = props.getProperty("callerSearchScope");
        if ("SUBTREE".equalsIgnoreCase(prop)) {
            return LdapSearchScope.SUBTREE;
        } else {
            return LdapSearchScope.ONE_LEVEL;
        }
    }

    public String getGroupMemberAttribute() throws IOException {
        refereshConfiguration();
        return props.getProperty("groupMemberAttribute");
    }

    public String getGroupMemberOfAttribute() throws IOException {
        refereshConfiguration();
        return props.getProperty("groupMemberOfAttribute");
    }

    public String getGroupNameAttribute() throws IOException {
        refereshConfiguration();
        return props.getProperty("groupNameAttribute");
    }

    public String getGroupSearchBase() throws IOException {
        refereshConfiguration();
        return props.getProperty("groupSearchBase");
    }

    public String getGroupSearchFilter() throws IOException {
        refereshConfiguration();
        return props.getProperty("groupSearchFilter");
    }

    public LdapSearchScope getGroupSearchScope() throws IOException {
        refereshConfiguration();

        String prop = props.getProperty("groupSearchScope");
        if ("SUBTREE".equalsIgnoreCase(prop)) {
            return LdapSearchScope.SUBTREE;
        } else {
            return LdapSearchScope.ONE_LEVEL;
        }
    }

    public Integer getReadTimeout() throws IOException {
        refereshConfiguration();
        return Integer.valueOf(props.getProperty("readTimeout"));
    }

    public String getUrl() throws IOException {
        refereshConfiguration();
        return props.getProperty("url");
    }

    public Set<ValidationType> getUseFor() throws IOException {
        refereshConfiguration();

        Set<ValidationType> results = new HashSet<ValidationType>();

        String prop = props.getProperty("useFor");
        if (prop.contains("VALIDATE")) {
            results.add(ValidationType.VALIDATE);
        }
        if (prop.contains("PROVIDE_GROUPS")) {
            results.add(ValidationType.PROVIDE_GROUPS);
        }

        return results;
    }

    private void refereshConfiguration() throws IOException {
        props = new Properties();
        props.load(new FileReader("LdapSettingsBean.props"));
    }
}
