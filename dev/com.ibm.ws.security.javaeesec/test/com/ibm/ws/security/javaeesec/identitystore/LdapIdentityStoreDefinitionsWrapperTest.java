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
package com.ibm.ws.security.javaeesec.identitystore;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Verify that the {@link LdapIdentityStoreDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in JSR375.
 */
public class LdapIdentityStoreDefinitionsWrapperTest {

    /*
     * TODO Need to fix ignored tests.
     * TODO Should really have better EL expressions that would represent how a customer would use them.
     */

    @Test
    public void bindDn() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("cn=root", wrapper.getBindDn());
    }

    @Test
    public void bindDn_EL() {
        /*
         * Override the bindDn setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("bindDn", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getBindDn());
    }

    @Test
    public void bindDnPassword() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("password", wrapper.getBindDnPassword());
    }

    @Test
    public void bindDnPassword_EL() {
        /*
         * Override the bindDnPassword setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("bindDnPassword", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getBindDnPassword());
    }

    @Test
    public void callerBaseDn() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("dc=domain,dc=com", wrapper.getCallerBaseDn());
    }

    @Test
    public void callerBaseDn_EL() {
        /*
         * Override the callerBaseDn setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("callerBaseDn", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getCallerBaseDn());
    }

    @Test
    public void callerNameAttribute() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("uid", wrapper.getCallerNameAttribute());
    }

    @Test
    public void callerNameAttribute_EL() {
        /*
         * Override the callerNameAttribute setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("callerNameAttribute", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getCallerNameAttribute());
    }

    @Test
    public void callerSearchBase() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("ou=users,dc=domain,dc=com", wrapper.getCallerSearchBase());
    }

    @Test
    public void callerSearchBase_EL() {
        /*
         * Override the callerSearchBase setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("callerSearchBase", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getCallerSearchBase());
    }

    @Test
    public void callerSearchFilter() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("(&(objectclass=inetorgperson)(uid=%v))", wrapper.getCallerSearchFilter());
    }

    @Test
    public void callerSearchFilter_EL() {
        /*
         * Override the callerSearchFilter setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("callerSearchFilter", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getCallerSearchFilter());
    }

    @Test
    public void callerSearchScope() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(LdapSearchScope.SUBTREE, wrapper.getCallerSearchScope());
    }

    @Test
    @Ignore("This will fail until the EL implementation can handle inner enums.")
    public void callerSearchScope_EL() {
        /*
         * Override the callerSearchScope with the callerSearchScopeExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("callerSearchScopeExpression", "LdapSearchScopeType.SUBTREE");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(LdapSearchScope.SUBTREE, wrapper.getCallerSearchScope());
    }

    @Test
    public void groupMemberAttribute() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("member", wrapper.getGroupMemberAttribute());
    }

    @Test
    public void groupMemberAttribute_EL() {
        /*
         * Override the groupMemberAttribute setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupMemberAttribute", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getGroupMemberAttribute());
    }

    @Test
    public void groupMemberOfAttribute() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("memberof", wrapper.getGroupMemberOfAttribute());
    }

    @Test
    public void groupMemberOfAttribute_EL() {
        /*
         * Override the groupMemberOfAttribute setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupMemberOfAttribute", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getGroupMemberOfAttribute());
    }

    @Test
    public void groupNameAttribute() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("cn", wrapper.getGroupNameAttribute());
    }

    @Test
    public void groupNameAttribute_EL() {
        /*
         * Override the groupNameAttribute setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupNameAttribute", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getGroupNameAttribute());
    }

    @Test
    public void groupSearchBase() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("ou=groups,dc=domain,dc=com", wrapper.getGroupSearchBase());
    }

    @Test
    public void groupSearchBase_EL() {
        /*
         * Override the groupSearchBase setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupSearchBase", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getGroupSearchBase());
    }

    @Test
    public void groupSearchFilter() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("(objectclass=groupofnames)", wrapper.getGroupSearchFilter());
    }

    @Test
    public void groupSearchFilter_EL() {
        /*
         * Override the groupSearchFilter setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupSearchFilter", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getGroupSearchFilter());
    }

    @Test
    public void groupSearchScope() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(LdapSearchScope.SUBTREE, wrapper.getGroupSearchScope());
    }

    @Test
    @Ignore("This will fail until the EL implementation can handle inner enums.")
    public void groupSearchScope_EL() {
        /*
         * Override the groupSearchScope with the groupSearchScopeExpressionExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("groupSearchScopeExpression", "LdapSearchScopeType.SUBTREE");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(LdapSearchScope.SUBTREE, wrapper.getGroupSearchScope());
    }

    @Test
    public void maxResults() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(1000, wrapper.getMaxResults());
    }

    @Test
    public void maxResults_EL() {
        /*
         * Override the maxResults with the maxResultsExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("maxResultsExpression", "100/20");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(5, wrapper.getMaxResults());
    }

    @Test
    public void priority() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(80, wrapper.getPriority());
    }

    @Test
    public void priority_EL() {
        /*
         * Override the priority with the priorityExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("priorityExpression", "80/20");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(4, wrapper.getPriority());
    }

    @Test
    public void readTimeout() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(0, wrapper.getReadTimeout());
    }

    @Test
    public void readTimeout_EL() {
        /*
         * Override the readTimeout with the readTimeoutExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("readTimeoutExpression", "100*100");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals(10000, wrapper.getReadTimeout());
    }

    @Test
    public void url() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("ldap://localhost", wrapper.getUrl());
    }

    @Test
    public void url_EL() {
        /*
         * Override the url setting with an EL expression.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("url", "'blah'.concat('blah')");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertEquals("blahblah", wrapper.getUrl());
    }

    @Test
    public void useFor() {
        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(null);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertTrue(wrapper.getUseFor().contains(ValidationType.PROVIDE_GROUPS));
        assertTrue(wrapper.getUseFor().contains(ValidationType.VALIDATE));
    }

    @Test
    @Ignore("This will fail until the EL implementation can handle inner enums.")
    public void useFor_EL() {
        /*
         * Override the useFor with the useForExpression setting.
         */
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("useForExpression", "{ValidationType.VALIDATE}");

        LdapIdentityStoreDefinition idStoreDefinition = getInstanceofAnnotation(overrides);
        LdapIdentityStoreDefinitionWrapper wrapper = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);

        assertFalse(wrapper.getUseFor().contains(ValidationType.PROVIDE_GROUPS));
        assertTrue(wrapper.getUseFor().contains(ValidationType.VALIDATE));
    }

    private LdapIdentityStoreDefinition getInstanceofAnnotation(final Map<String, Object> overrides) {
        LdapIdentityStoreDefinition annotation = new LdapIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String bindDn() {
                return (overrides != null && overrides.containsKey("bindDn")) ? (String) overrides.get("bindDn") : "cn=root";
            }

            @Override
            public String bindDnPassword() {
                return (overrides != null && overrides.containsKey("bindDnPassword")) ? (String) overrides.get("bindDnPassword") : "password";
            }

            @Override
            public String callerBaseDn() {
                return (overrides != null && overrides.containsKey("callerBaseDn")) ? (String) overrides.get("callerBaseDn") : "dc=domain,dc=com";
            }

            @Override
            public String callerNameAttribute() {
                return (overrides != null && overrides.containsKey("callerNameAttribute")) ? (String) overrides.get("callerNameAttribute") : "uid";
            }

            @Override
            public String callerSearchBase() {
                return (overrides != null && overrides.containsKey("callerSearchBase")) ? (String) overrides.get("callerSearchBase") : "ou=users,dc=domain,dc=com";
            }

            @Override
            public String callerSearchFilter() {
                return (overrides != null && overrides.containsKey("callerSearchFilter")) ? (String) overrides.get("callerSearchFilter") : "(&(objectclass=inetorgperson)(uid=%v))";

            }

            @Override
            public LdapSearchScope callerSearchScope() {
                return (overrides != null && overrides.containsKey("callerSearchScope")) ? (LdapSearchScope) overrides.get("callerSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String callerSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("callerSearchScopeExpression")) ? (String) overrides.get("callerSearchScopeExpression") : "";
            }

            @Override
            public String groupMemberAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberAttribute")) ? (String) overrides.get("groupMemberAttribute") : "member";
            }

            @Override
            public String groupMemberOfAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberOfAttribute")) ? (String) overrides.get("groupMemberOfAttribute") : "memberof";
            }

            @Override
            public String groupNameAttribute() {
                return (overrides != null && overrides.containsKey("groupNameAttribute")) ? (String) overrides.get("groupNameAttribute") : "cn";
            }

            @Override
            public String groupSearchBase() {
                return (overrides != null && overrides.containsKey("groupSearchBase")) ? (String) overrides.get("groupSearchBase") : "ou=groups,dc=domain,dc=com";
            }

            @Override
            public String groupSearchFilter() {
                return (overrides != null && overrides.containsKey("groupSearchFilter")) ? (String) overrides.get("groupSearchFilter") : "(objectclass=groupofnames)";
            }

            @Override
            public LdapSearchScope groupSearchScope() {
                return (overrides != null && overrides.containsKey("groupSearchScope")) ? (LdapSearchScope) overrides.get("groupSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String groupSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("groupSearchScopeExpression")) ? (String) overrides.get("groupSearchScopeExpression") : "";
            }

            @Override
            public int maxResults() {
                return (overrides != null && overrides.containsKey("maxResults")) ? (Integer) overrides.get("maxResults") : 1000;
            }

            @Override
            public String maxResultsExpression() {
                return (overrides != null && overrides.containsKey("maxResultsExpression")) ? (String) overrides.get("maxResultsExpression") : "";
            }

            @Override
            public int priority() {
                return (overrides != null && overrides.containsKey("priority")) ? (Integer) overrides.get("priority") : 80;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey("priorityExpression")) ? (String) overrides.get("priorityExpression") : "";
            }

            @Override
            public int readTimeout() {
                return (overrides != null && overrides.containsKey("readTimeout")) ? (Integer) overrides.get("readTimeout") : 0;
            }

            @Override
            public String readTimeoutExpression() {
                return (overrides != null && overrides.containsKey("readTimeoutExpression")) ? (String) overrides.get("readTimeoutExpression") : "";
            }

            @Override
            public String url() {
                return (overrides != null && overrides.containsKey("url")) ? (String) overrides.get("url") : "ldap://localhost";
            }

            @Override
            public ValidationType[] useFor() {
                return (overrides != null && overrides.containsKey("useFor")) ? (ValidationType[]) overrides.get("useFor") : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                    ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey("useForExpression")) ? (String) overrides.get("useForExpression") : "";
            }
        };

        return annotation;
    }
}
