/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.security.authorization.jacc.internal.proxy.AuthzModuleTracker.ModuleType;
import io.openliberty.security.authorization.jacc.internal.proxy.JakartaPolicyConfigProxy.ContextState;
import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.EJBRoleRefPermission;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebRoleRefPermission;
import jakarta.security.jacc.WebUserDataPermission;

/**
 * Validates that the JakartaPolicyConfigFactoryProxy class correctly follows the
 * contract of the PolicyConfigurationFactory class's Javadoc
 */
public class PolicyConfigFactoryProxyTest {

    private static final JakartaPolicyConfigFactoryProxy configFactoryProxy = JakartaPolicyConfigFactoryProxy.getInstance();

    @BeforeClass
    public static void beforeClass() {
        // If any tests fail, do not cause the first test to fail in this suite due to left over state.
        AuthzModuleTracker.clear();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(null);
        PolicyContext.setContextID(null);
        PolicyFactory.setPolicyFactory(new PolicyFactoryImpl());
    }

    @After
    public void afterTest() {
        // If any tests fail, do not cause tests afterward to fail as well due to left over state.
        beforeClass();
    }

    static void populatePolicyConfig(PolicyConfigurationFactory configFactory, PolicyConfiguration config, boolean withDelegate) throws PolicyContextException {
        String contextId = config.getContextID();
        config.addToExcludedPolicy(new WebUserDataPermission("/example", "POST"));
        config.addToExcludedPolicy(new EJBMethodPermission("ExampleBean", "doWork,Local"));
        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(Permission)"));
        }

        config.addToUncheckedPolicy(new WebResourcePermission("/example", "GET"));
        config.addToUncheckedPolicy(new EJBMethodPermission("ExampleBean", "moreWork,Local"));
        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 2,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(Permission)"));
        }

        config.addToRole("user1", new EJBRoleRefPermission("ExampleBean", "mostWork"));
        config.addToRole("user2", new WebRoleRefPermission("/example", "PUT"));
        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToRole(String, Permission)"));
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 6, AuthzModuleTracker.getOperationCount(contextId));
            AuthzModuleTracker.remove(contextId);
            assertFalse(AuthzModuleTracker.hasTrackedData());
        }

        String linkContext1 = contextId + "/link1";
        String linkContext2 = contextId + "/link2";
        String linkContext3 = contextId + "/link3";

        PolicyConfiguration link1Config = configFactory.getPolicyConfiguration(linkContext1);
        PolicyConfiguration link2Config = configFactory.getPolicyConfiguration(linkContext2);
        PolicyConfiguration link3Config = configFactory.getPolicyConfiguration(linkContext3);

        config.linkConfiguration(link1Config);
        if (withDelegate) {
            validateLinkOperation(contextId, linkContext1);
        }
        config.linkConfiguration(link2Config);
        if (withDelegate) {
            validateLinkOperation(contextId, linkContext2);
        }
        config.linkConfiguration(link3Config);
        if (withDelegate) {
            validateLinkOperation(contextId, linkContext3);
        }
    }

    private static void validateLinkOperation(String contextId, String linkContextId) {
        String authzModuleString = AuthzModuleTracker.getTrackerMapString();
        assertEquals(authzModuleString, 1, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "linkConfiguration"));
        // getContextID is called on the linked configs in order to determine if the context IDs match
        assertEquals(authzModuleString, 1, AuthzModuleTracker.getOperationCount(linkContextId, ModuleType.POLICY_CONFIG, "getContextID"));

        assertEquals(authzModuleString, 1, AuthzModuleTracker.getOperationCount(contextId));
        assertEquals(authzModuleString, 1, AuthzModuleTracker.getOperationCount(linkContextId));
        AuthzModuleTracker.remove(contextId);
        AuthzModuleTracker.remove(linkContextId);
        assertFalse(authzModuleString, AuthzModuleTracker.hasTrackedData());
    }

    static void validateEmptyPolicyConfig(PolicyConfiguration config, Set<? extends PolicyConfiguration> linkedConfigs, boolean withDelegate) throws PolicyContextException {
        String contextId = config.getContextID();
        PermissionCollection perms = config.getExcludedPermissions();
        Enumeration<Permission> permEnum = perms.elements();
        assertFalse(permEnum.hasMoreElements());

        perms = config.getUncheckedPermissions();
        permEnum = perms.elements();
        assertFalse(permEnum.hasMoreElements());

        Map<String, PermissionCollection> rolesMap = config.getPerRolePermissions();
        assertEquals(0, rolesMap.size());

        assertEquals(0, linkedConfigs.size());

        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getExcludedPermissions"));
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getUncheckedPermissions"));
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getPerRolePermissions"));
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 3,
                         AuthzModuleTracker.getOperationCount(contextId));
            AuthzModuleTracker.remove(contextId);
            assertFalse(AuthzModuleTracker.hasTrackedData());
        }
    }

    static void validatePopulatedPolicyConfig(String contextId, PolicyConfiguration config, Set<? extends PolicyConfiguration> linkedConfigs,
                                              boolean withDelegate) throws PolicyContextException {
        boolean wrappedConfig = config instanceof WrappedPolicyConfigImpl;
        PermissionCollection perms = config.getExcludedPermissions();
        Enumeration<Permission> permEnum = perms.elements();
        assertTrue(permEnum.hasMoreElements());
        Permission exclude1 = permEnum.nextElement();
        assertTrue(permEnum.hasMoreElements());
        Permission exclude2 = permEnum.nextElement();
        assertFalse(permEnum.hasMoreElements());

        WebUserDataPermission webExclude = null;
        EJBMethodPermission ejbExclude = null;
        if (exclude1 instanceof WebUserDataPermission) {
            webExclude = (WebUserDataPermission) exclude1;
        } else if ((exclude2 instanceof WebUserDataPermission)) {
            webExclude = (WebUserDataPermission) exclude2;
        }

        if (exclude1 instanceof EJBMethodPermission) {
            ejbExclude = (EJBMethodPermission) exclude1;
        } else if ((exclude2 instanceof EJBMethodPermission)) {
            ejbExclude = (EJBMethodPermission) exclude2;
        }
        assertNotNull(webExclude);
        assertNotNull(ejbExclude);

        assertEquals("/example", webExclude.getName());
        assertEquals("POST", webExclude.getActions());

        assertEquals("ExampleBean", ejbExclude.getName());
        assertEquals("doWork,Local", ejbExclude.getActions());

        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getExcludedPermissions"));
            if (wrappedConfig) {
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "getExcludedPermissions"));
            }
        }

        perms = config.getUncheckedPermissions();
        permEnum = perms.elements();
        assertTrue(permEnum.hasMoreElements());
        Permission unchecked1 = permEnum.nextElement();
        assertTrue(permEnum.hasMoreElements());
        Permission unchecked2 = permEnum.nextElement();
        assertFalse(permEnum.hasMoreElements());

        WebResourcePermission webUncheck = null;
        EJBMethodPermission ejbUncheck = null;
        if (unchecked1 instanceof WebResourcePermission) {
            webUncheck = (WebResourcePermission) unchecked1;
        } else if ((unchecked2 instanceof WebResourcePermission)) {
            webUncheck = (WebResourcePermission) unchecked2;
        }

        if (unchecked1 instanceof EJBMethodPermission) {
            ejbUncheck = (EJBMethodPermission) unchecked1;
        } else if ((unchecked2 instanceof EJBMethodPermission)) {
            ejbUncheck = (EJBMethodPermission) unchecked2;
        }
        assertNotNull(webUncheck);
        assertNotNull(ejbUncheck);

        assertEquals("/example", webUncheck.getName());
        assertEquals("GET", webUncheck.getActions());

        assertEquals("ExampleBean", ejbUncheck.getName());
        assertEquals("moreWork,Local", ejbUncheck.getActions());

        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getUncheckedPermissions"));
            if (wrappedConfig) {
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "getUncheckedPermissions"));
            }
        }

        Map<String, PermissionCollection> rolesMap = config.getPerRolePermissions();
        assertEquals(2, rolesMap.size());
        PermissionCollection user1Perms = rolesMap.get("user1");
        PermissionCollection user2Perms = rolesMap.get("user2");
        assertNotNull(user1Perms);
        assertNotNull(user2Perms);
        Enumeration<Permission> user1PermEnum = user1Perms.elements();
        Enumeration<Permission> user2PermEnum = user2Perms.elements();
        assertTrue(user1PermEnum.hasMoreElements());
        assertTrue(user2PermEnum.hasMoreElements());
        Permission user1Perm = user1PermEnum.nextElement();
        Permission user2Perm = user2PermEnum.nextElement();
        assertFalse(user1PermEnum.hasMoreElements());
        assertFalse(user2PermEnum.hasMoreElements());
        assertTrue(user1Perm instanceof EJBRoleRefPermission);
        assertTrue(user2Perm instanceof WebRoleRefPermission);
        EJBRoleRefPermission ejbRolePerm = (EJBRoleRefPermission) user1Perm;
        WebRoleRefPermission webRolePerm = (WebRoleRefPermission) user2Perm;

        assertEquals("ExampleBean", ejbRolePerm.getName());
        assertEquals("mostWork", ejbRolePerm.getActions());

        assertEquals("/example", webRolePerm.getName());
        assertEquals("PUT", webRolePerm.getActions());

        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                         AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getPerRolePermissions"));
            if (wrappedConfig) {
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "getPerRolePermissions"));
            }
        }

        assertEquals(3, linkedConfigs.size());
        Iterator<? extends PolicyConfiguration> it = linkedConfigs.iterator();
        PolicyConfiguration linkConfig1 = it.next();
        PolicyConfiguration linkConfig2 = it.next();
        PolicyConfiguration linkConfig3 = it.next();

        String expectedLink1 = contextId + "/link1";
        String expectedLink2 = contextId + "/link2";
        String expectedLink3 = contextId + "/link3";
        String context1 = linkConfig1.getContextID();
        String context2 = linkConfig2.getContextID();
        String context3 = linkConfig3.getContextID();

        PolicyConfiguration link1 = null;
        PolicyConfiguration link2 = null;
        PolicyConfiguration link3 = null;
        if (expectedLink1.equals(context1)) {
            link1 = linkConfig1;
            if (expectedLink2.equals(context2)) {
                assertEquals(expectedLink3, context3);
                link2 = linkConfig2;
                link3 = linkConfig3;
            } else {
                assertEquals(expectedLink2, context3);
                assertEquals(expectedLink3, context2);
                link2 = linkConfig3;
                link3 = linkConfig2;
            }
        } else if (expectedLink1.equals(context2)) {
            link1 = linkConfig2;
            if (expectedLink2.equals(context1)) {
                assertEquals(expectedLink3, context3);
                link2 = linkConfig1;
                link3 = linkConfig3;
            } else {
                assertEquals(expectedLink2, context3);
                assertEquals(expectedLink3, context1);
                link2 = linkConfig3;
                link3 = linkConfig1;
            }
        } else {
            expectedLink1.equals(context3);
            link1 = linkConfig3;
            if (expectedLink2.equals(context1)) {
                assertEquals(expectedLink3, context2);
                link2 = linkConfig1;
                link3 = linkConfig2;
            } else {
                assertEquals(expectedLink2, context2);
                assertEquals(expectedLink3, context1);
                link2 = linkConfig2;
                link3 = linkConfig1;
            }
        }

        // deleted
        assertFalse(link1.inService());
        try {
            link1.commit();
            fail("Didn't get expected exception");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }

        // committed
        assertTrue(link2.inService());

        // open
        assertFalse(link3.inService());
        // if open it won't get an exception but doesn't change anything either.
        link3.removeRole("fakerole");

        if (withDelegate) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), wrappedConfig ? 6 : 3,
                         AuthzModuleTracker.getOperationCount(contextId));

            AuthzModuleTracker.remove(contextId);
            if ((linkConfig1 instanceof JakartaPolicyConfigProxy)) {
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink3, ModuleType.POLICY_CONFIG, "removeRole"));

                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink3));
                AuthzModuleTracker.remove(expectedLink3);
            } else {
                // getContextID calls above
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink1, ModuleType.POLICY_CONFIG, "getContextID"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink1, ModuleType.POLICY_CONFIG, "inService"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink1, ModuleType.POLICY_CONFIG, "commit"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink2, ModuleType.POLICY_CONFIG, "getContextID"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink2, ModuleType.POLICY_CONFIG, "inService"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink3, ModuleType.POLICY_CONFIG, "getContextID"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink3, ModuleType.POLICY_CONFIG, "inService"));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 1,
                             AuthzModuleTracker.getOperationCount(expectedLink3, ModuleType.POLICY_CONFIG, "removeRole"));

                assertEquals(AuthzModuleTracker.getTrackerMapString(), 3,
                             AuthzModuleTracker.getOperationCount(expectedLink1));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 2,
                             AuthzModuleTracker.getOperationCount(expectedLink2));
                assertEquals(AuthzModuleTracker.getTrackerMapString(), 3,
                             AuthzModuleTracker.getOperationCount(expectedLink3));
                AuthzModuleTracker.remove(expectedLink1);
                AuthzModuleTracker.remove(expectedLink2);
                AuthzModuleTracker.remove(expectedLink3);
            }
            assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
        }
    }

    private void createLinkedConfigurations(PolicyConfiguration policyConfig, String contextId, boolean withDelegate) throws Exception {
        // create linked policy configuration that are used later
        String linkContexts[] = { contextId + "/link1", contextId + "/link2", contextId + "/link3" };

        for (int i = 0; i < linkContexts.length; ++i) {
            PolicyConfiguration config = configFactoryProxy.getPolicyConfiguration(linkContexts[i], false);
            config.linkConfiguration(policyConfig);
            if (i == 0) {
                config.delete();
            } else if (i == 1) {
                config.commit();
            }
        }
        if (withDelegate) {

            for (int i = 0; i < linkContexts.length; ++i) {
                String linkContext = linkContexts[i];
                // Should have three operations.  getPolicyConfiguration(String, boolean) called
                // on the PCF and the constructor called.  Delete or reOpen should not be called since it
                // is a new config.  linkConfiguration should be called since there is a link to the base config.

                /* 1 */assertTrue(AuthzModuleTracker.getTrackerMapString(),
                                  AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
                /* 2 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "ctor"));
                /* 3 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "linkConfiguration"));

                int expectedCount = 3;
                if (i != 2) {
                    expectedCount += 6;
                    /* 4 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, i == 0 ? "delete" : "commit"));
                    /* 5 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
                    /* 6 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY, "ctor"));
                    /* 7 */assertTrue(AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY, "refresh"));
                    // once in the constructor and once in refresh
                    /* 9 */assertEquals(2, AuthzModuleTracker.getOperationCount(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
                }
                assertEquals(AuthzModuleTracker.getTrackerMapString(), expectedCount, AuthzModuleTracker.getOperationCount(linkContext));
                AuthzModuleTracker.remove(linkContext);
            }

            // called 3 times when the link is called
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 3, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getContextID"));
            AuthzModuleTracker.remove(contextId);

            assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
        }
    }

    private void createPolicy(String contextId) throws Exception {
        PolicyFactory.getPolicyFactory().getPolicy(contextId);

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY, "ctor"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
        assertEquals(AuthzModuleTracker.getTrackerMapString(), 3, AuthzModuleTracker.getOperationCount(contextId));
        AuthzModuleTracker.remove(contextId);
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
    }

    @Test
    public void test_getPolicyConfigurationStringBooleanNoDelegate() throws Exception {

        // ContextId is required to be non null
        try {
            configFactoryProxy.getPolicyConfiguration(null, false);
            fail("Expected an exception when passed a null contextId");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            configFactoryProxy.getPolicyConfiguration(null, true);
            fail("Expected an exception when passed a null contextId");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        String contextId = "StringBoolean";

        JakartaPolicyConfigProxy policyConfig = configFactoryProxy.getPolicyConfiguration(contextId, true);

        createLinkedConfigurations(policyConfig, contextId, false);

        assertNotNull(policyConfig);

        assertTrue(policyConfig.isState(ContextState.OPEN));

        validateEmptyPolicyConfig(policyConfig, policyConfig.getLinkedConfigurations(), false);

        // validate that if you call remove again with false, it keeps the same policy config and
        // is still open
        JakartaPolicyConfigProxy policyConfig2 = configFactoryProxy.getPolicyConfiguration(contextId, false);

        assertSame(policyConfig, policyConfig2);

        assertTrue(policyConfig.isState(ContextState.OPEN));

        validateEmptyPolicyConfig(policyConfig, policyConfig.getLinkedConfigurations(), false);

        // validate that if you call remove again with true, it keeps the same policy config and
        // is still open
        policyConfig2 = configFactoryProxy.getPolicyConfiguration(contextId, true);

        assertSame(policyConfig, policyConfig2);

        assertTrue(policyConfig.isState(ContextState.OPEN));

        validateEmptyPolicyConfig(policyConfig, policyConfig.getLinkedConfigurations(), false);

        populatePolicyConfig(configFactoryProxy, policyConfig, false);

        validatePopulatedPolicyConfig(contextId, policyConfig, policyConfig.getLinkedConfigurations(), false);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
    }

    private void setState(PolicyConfiguration policyConfig, ContextState contextState) throws Exception {
        if (contextState == ContextState.DELETED) {
            policyConfig.delete();
        } else if (contextState == ContextState.IN_SERVICE) {
            policyConfig.commit();
        }
    }

    private void validateStateChange(String contextId, ContextState contextState, boolean withDelegate) throws Exception {
        String operation = null;
        if (contextState == ContextState.DELETED) {
            operation = "delete";
        } else if (contextState == ContextState.IN_SERVICE) {
            operation = "commit";
        }
        if (withDelegate && operation != null) {
            assertEquals(AuthzModuleTracker.getTrackerMapString(), 4, AuthzModuleTracker.getOperationCount(contextId));

            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, operation));
            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY, "refresh"));
            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));

            AuthzModuleTracker.remove(contextId);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
    }

    @Test
    public void test_getPolicyConfigurationStringBooleanWithDelegateWithOpenState() throws Exception {
        test_getPolicyConfigurationStringBooleanWithDelegate("StringBoolean2", ContextState.OPEN);
    }

    @Test
    public void test_getPolicyConfigurationStringBooleanWithDelegateWithDELETEDState() throws Exception {
        test_getPolicyConfigurationStringBooleanWithDelegate("StringBoolean3", ContextState.DELETED);
    }

    @Test
    public void test_getPolicyConfigurationStringBooleanWithDelegateWithIn_ServiceState() throws Exception {
        test_getPolicyConfigurationStringBooleanWithDelegate("StringBoolean4", ContextState.IN_SERVICE);
    }

    private void test_getPolicyConfigurationStringBooleanWithDelegate(String contextId, ContextState contextState) throws Exception {

        PolicyConfigFactoryImpl configFactory = new PolicyConfigFactoryImpl();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory);

        // ContextId is required to be non null
        try {
            configFactoryProxy.getPolicyConfiguration(null, false);
            fail("Expected an exception when passed a null contextId");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            configFactoryProxy.getPolicyConfiguration(null, true);
            fail("Expected an exception when passed a null contextId");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Nothing should be called on the delegate PolicyConfigurationFactory when passed a null contextID
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        createPolicy(contextId);

        JakartaPolicyConfigProxy policyConfigProxy = configFactoryProxy.getPolicyConfiguration(contextId, true);

        assertNotNull(policyConfigProxy);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have two operations.  getPolicyConfiguration(String, boolean) called
        // on the PCF and the constructor called.  Delete or reOpen should not be called since it
        // is a new config.
        assertEquals(2, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "ctor"));

        AuthzModuleTracker.remove(contextId);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        createLinkedConfigurations(policyConfigProxy, contextId, true);

        setState(policyConfigProxy, contextState);

        validateStateChange(contextId, contextState, true);

        validateEmptyPolicyConfig(policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        // validate that if you call again with remove as false, it keeps the same policy config and
        // is still open
        JakartaPolicyConfigProxy policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, false);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have two operations.  getPolicyConfiguration(String, boolean) called
        // on the PCF and reOpen.
        assertEquals(2, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "reOpen"));

        AuthzModuleTracker.remove(contextId);
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validateEmptyPolicyConfig(policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        // validate that if you call remove again with true, it keeps the same policy config and
        // is still open
        policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, true);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have three operations.  getPolicyConfiguration(String, boolean) called
        // on the PCF, delete and reOpen.
        assertEquals(3, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "delete"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "reOpen"));

        AuthzModuleTracker.remove(contextId);
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validateEmptyPolicyConfig(policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        populatePolicyConfig(configFactoryProxy, policyConfigProxy, true);

        AuthzModuleTracker.clear();

        validatePopulatedPolicyConfig(contextId, policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        // validate that if you call remove again with true and populated config, it keeps the same policy config and
        // is still open, but now it will be empty
        policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, true);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have three operations.  getPolicyConfiguration(String, boolean) called
        // on the PCF, delete and reOpen.
        assertEquals(3, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "delete"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "reOpen"));

        AuthzModuleTracker.remove(contextId);
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validateEmptyPolicyConfig(policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        populatePolicyConfig(configFactoryProxy, policyConfigProxy, true);

        AuthzModuleTracker.clear();

        validatePopulatedPolicyConfig(contextId, policyConfigProxy, policyConfigProxy.getLinkedConfigurations(), true);

        PolicyConfigImpl policyConfig = configFactory.getPolicyConfiguration(contextId);

        // Should have one operations.  getPolicyConfiguration(String)
        assertEquals(1, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));

        AuthzModuleTracker.remove(contextId);

        validatePopulatedPolicyConfig(contextId, policyConfig, policyConfig.getLinkedConfigurations(), true);

        // Add a different factory.  The PolicyConfig objects should be the same, only the factory is different
        DelegatingPolicyConfigFactoryImpl delegatingFactory = new DelegatingPolicyConfigFactoryImpl(configFactory);
        PolicyConfigurationFactory.setPolicyConfigurationFactory(delegatingFactory);

        policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, false);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have three operations.  getPolicyConfiguration(String, boolean) called both both PCFs and reOpen.
        assertEquals(3, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "reOpen"));

        AuthzModuleTracker.remove(contextId);
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        PolicyConfigImpl policyConfig2 = delegatingFactory.getPolicyConfiguration(contextId);

        assertSame(policyConfig, policyConfig2);

        // Should have two operations.  getPolicyConfiguration(String) for both delegating factory and its delegate
        assertEquals(2, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.DELEGATING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));

        AuthzModuleTracker.remove(contextId);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validatePopulatedPolicyConfig(contextId, policyConfig2, policyConfig2.getLinkedConfigurations(), true);

        // Add a different factory.  The PolicyConfig objects are different so they are meant to be re-populated when new PCF is added
        WrappingPolicyConfigFactoryImpl wrappingFactory = new WrappingPolicyConfigFactoryImpl(configFactory);
        PolicyConfigurationFactory.setPolicyConfigurationFactory(wrappingFactory);

        policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, false);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        // Should have 22 operations.  getPolicyConfiguration(String, boolean) called both both PCFs and ctor for wrapping PolicyConfig and
        // reOpen for the config that is wrapped.  After that the new config is populated with the stored config.
        String trackerMapString = AuthzModuleTracker.getTrackerMapString();
        assertEquals(trackerMapString, 24, AuthzModuleTracker.getOperationCount(contextId));

        /* 1 */assertTrue(trackerMapString,
                          AuthzModuleTracker.hasOperation(contextId, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
        /* 2 */assertTrue(trackerMapString,
                          AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
        /* 3 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "reOpen"));
        /* 4 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "ctor"));
        /* 6 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "addToExcludedPolicy(Permission)"));
        /* 8 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(Permission)"));
        /* 10 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "addToUncheckedPolicy(Permission)"));
        /* 12 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(Permission)"));
        /* 14 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "addToRole(String, PermissionCollection)"));
        /* 16 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToRole(String, PermissionCollection)"));
        /* 19 */assertEquals(trackerMapString, 3, AuthzModuleTracker.getOperationCount(contextId, ModuleType.WRAPPING_POLICY_CONFIG, "linkConfiguration"));
        /* 22 */assertEquals(trackerMapString, 3, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "linkConfiguration"));
        /* 24 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getContextID"));

        AuthzModuleTracker.remove(contextId);

        String linkContexts[] = { contextId + "/link1", contextId + "/link2", contextId + "/link3" };
        for (int i = 0; i < linkContexts.length; ++i) {
            String linkContext = linkContexts[i];
            /* 1 */assertTrue(trackerMapString,
                              AuthzModuleTracker.hasOperation(linkContext, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
            /* 2 */assertTrue(trackerMapString,
                              AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
            /* 3 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "delete"));
            /* 4 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "reOpen"));
            /* 5 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.WRAPPING_POLICY_CONFIG, "ctor"));
            /* 6 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "getContextID"));

            int expectedCount = 6;
            if (i != 2) {
                expectedCount += 5;
                /* 7 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.WRAPPING_POLICY_CONFIG, (i == 0) ? "delete" : "commit"));
                /* 8 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, (i == 0) ? "delete" : "commit"));
                /* 9 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
                /* 10 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY, "refresh"));
                /* 11 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
            }

            if (i != 0) {
                expectedCount += 2;
                /* 6 or 11 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.WRAPPING_POLICY_CONFIG, "linkConfiguration"));
                /* 7 or 12 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "linkConfiguration"));
            }
            assertEquals(trackerMapString, expectedCount, AuthzModuleTracker.getOperationCount(linkContext));
            AuthzModuleTracker.remove(linkContext);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        WrappedPolicyConfigImpl policyConfig3 = wrappingFactory.getPolicyConfiguration(contextId);

        // Should have one operations.  getPolicyConfiguration(String) for the wrapping factory
        assertEquals(1, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.WRAPPING_POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));

        AuthzModuleTracker.remove(contextId);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validatePopulatedPolicyConfig(contextId, policyConfig3, policyConfig3.getLinkedConfigurations(), true);

        // Test transitioning from having a policy config factory to not having one.
        PolicyConfigurationFactory.setPolicyConfigurationFactory(null);

        policyConfigProxy2 = configFactoryProxy.getPolicyConfiguration(contextId, false);

        assertSame(policyConfigProxy, policyConfigProxy2);

        assertTrue(policyConfigProxy.isState(ContextState.OPEN));

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        validatePopulatedPolicyConfig(contextId, policyConfig2, policyConfig2.getLinkedConfigurations(), false);
    }

    @Test
    public void test_getPolicyConfigurationNoArgsNoDelegate() throws Exception {
        noArgsTest("NoArgs", false);
    }

    @Test
    public void test_getPolicyConfigurationNoArgsWithDelegate() throws Exception {
        PolicyConfigFactoryImpl configFactory = new PolicyConfigFactoryImpl();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory);

        String contextId = "NoArgs2";
        noArgsTest(contextId, true);

        // examine the content of the actual PolicyConfiguration
        PolicyContext.setContextID(contextId);
        PolicyConfigImpl config = configFactory.getPolicyConfiguration();
        assertNotNull(config);

        // Should have one operations.  getPolicyConfiguration()
        assertEquals(1, AuthzModuleTracker.getOperationCount(null));

        assertTrue(AuthzModuleTracker.hasOperation(null, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration"));

        AuthzModuleTracker.remove(null);

        validatePopulatedPolicyConfig(contextId, config, config.getLinkedConfigurations(), true);

        // Test with the PolicyConfigFactory changed, but the proxy is already populated
        PolicyConfigFactoryImpl configFactory2 = new PolicyConfigFactoryImpl();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory2);

        // if there isn't a PolicyContext set, the method just returns null
        configFactoryProxy.getPolicyConfiguration();

        // Should have 9 operations.  getPolicyConfiguration(String, boolean) and ctor for PolicyConfigs.
        // After that the new config is populated with the stored config.
        String trackerMapString = AuthzModuleTracker.getTrackerMapString();
        assertEquals(trackerMapString, 13, AuthzModuleTracker.getOperationCount(contextId));

        /* 1 */assertTrue(trackerMapString,
                          AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
        /* 2 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "ctor"));
        /* 4 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(Permission)"));
        /* 6 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(Permission)"));
        /* 8 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToRole(String, PermissionCollection)"));
        /* 11 */assertEquals(trackerMapString, 3, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "linkConfiguration"));

        // The linked configuration has a link back to this config.  There is logic in the ConfigProxy to not require another
        // getPolicyConfiguration on the delegate PolicyConfigurationFactory to get PolicyConfiguration that is currently being configured.
        /* 13 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getContextID"));

        AuthzModuleTracker.remove(contextId);

        String linkContexts[] = { contextId + "/link1", contextId + "/link2", contextId + "/link3" };
        for (int i = 0; i < linkContexts.length; ++i) {
            String linkContext = linkContexts[i];
            /* 1 */assertTrue(trackerMapString,
                              AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
            /* 2 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "ctor"));
            /* 3 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "getContextID"));

            int expectedCount = 3;
            if (i != 2) {
                expectedCount += 4;
                /* 4 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, (i == 0) ? "delete" : "commit"));
                /* 5 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
                /* 6 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY, "refresh"));
                /* 7 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
            }

            if (i != 0) {
                expectedCount++;
                /* 4 or 8 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "linkConfiguration"));
            }
            assertEquals(trackerMapString, expectedCount, AuthzModuleTracker.getOperationCount(linkContext));
            AuthzModuleTracker.remove(linkContext);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
        PolicyContext.setContextID(contextId);
    }

    private void noArgsTest(String contextId, boolean withDelegate) throws PolicyContextException, Exception {
        // if there isn't a PolicyContext set, the method just returns null
        assertNull(configFactoryProxy.getPolicyConfiguration());

        PolicyContext.setContextID(contextId);

        // if one hasn't been populated yet, it also just returns null
        assertNull(configFactoryProxy.getPolicyConfiguration());

        PolicyContext.setContextID(null);

        JakartaPolicyConfigProxy configProxy = configFactoryProxy.getPolicyConfiguration(contextId, false);

        if (withDelegate) {
            // Should have two operations.  getPolicyConfiguration(String, boolean) called
            // on the PCF and reOpen.
            assertEquals(2, AuthzModuleTracker.getOperationCount(contextId));

            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "ctor"));

            AuthzModuleTracker.remove(contextId);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        createLinkedConfigurations(configProxy, contextId, withDelegate);

        populatePolicyConfig(configFactoryProxy, configProxy, withDelegate);

        validatePopulatedPolicyConfig(contextId, configProxy, configProxy.getLinkedConfigurations(), withDelegate);

        // if there isn't a PolicyContext set, the method just returns null
        assertNull(configFactoryProxy.getPolicyConfiguration());

        PolicyContext.setContextID(contextId);

        // if one has been populated, it should be found
        JakartaPolicyConfigProxy configProxy2 = configFactoryProxy.getPolicyConfiguration();

        assertSame(configProxy, configProxy2);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        PolicyContext.setContextID(null);
    }

    @Test
    public void test_getPolicyConfigurationStringNoDelegate() throws Exception {
        stringArgTest("String", false);
    }

    @Test
    public void test_getPolicyConfigurationStringWithDelegate() throws Exception {
        PolicyConfigFactoryImpl configFactory = new PolicyConfigFactoryImpl();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory);

        String contextId = "String2";
        stringArgTest(contextId, true);

        // examine the content of the actual PolicyConfiguration
        PolicyConfigImpl config = configFactory.getPolicyConfiguration(contextId);
        assertNotNull(config);

        // Should have one operations.  getPolicyConfiguration(String)
        assertEquals(1, AuthzModuleTracker.getOperationCount(contextId));

        assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));

        AuthzModuleTracker.remove(contextId);

        validatePopulatedPolicyConfig(contextId, config, config.getLinkedConfigurations(), true);

        // Test with the PolicyConfigFactory changed, but the proxy is already populated
        PolicyConfigFactoryImpl configFactory2 = new PolicyConfigFactoryImpl();
        PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory2);

        // if there isn't a PolicyContext set, the method just returns null
        configFactoryProxy.getPolicyConfiguration(contextId);

        // Should have 9 operations.  getPolicyConfiguration(String, boolean) and ctor for PolicyConfigs.
        // After that the new config is populated with the stored config.
        String trackerMapString = AuthzModuleTracker.getTrackerMapString();
        assertEquals(trackerMapString, 13, AuthzModuleTracker.getOperationCount(contextId));

        /* 1 */assertTrue(trackerMapString,
                          AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
        /* 2 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "ctor"));
        /* 4 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToExcludedPolicy(Permission)"));
        /* 6 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToUncheckedPolicy(Permission)"));
        /* 8 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "addToRole(String, PermissionCollection)"));
        /* 11 */assertEquals(trackerMapString, 3, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "linkConfiguration"));
        /* 13 */assertEquals(trackerMapString, 2, AuthzModuleTracker.getOperationCount(contextId, ModuleType.POLICY_CONFIG, "getContextID"));

        AuthzModuleTracker.remove(contextId);

        String linkContexts[] = { contextId + "/link1", contextId + "/link2", contextId + "/link3" };
        for (int i = 0; i < linkContexts.length; ++i) {
            String linkContext = linkContexts[i];
            /* 1 */assertTrue(trackerMapString,
                              AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, true)"));
            /* 2 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "ctor"));
            /* 3 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "getContextID"));

            int expectedCount = 3;
            if (i != 2) {
                expectedCount += 4;
                /* 4 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, (i == 0) ? "delete" : "commit"));
                /* 5 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_FACTORY, "getPolicy(String)"));
                /* 6 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY, "refresh"));
                /* 7 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String)"));
            }
            if (i != 0) {
                expectedCount++;
                /* 4 or 8 */assertTrue(trackerMapString, AuthzModuleTracker.hasOperation(linkContext, ModuleType.POLICY_CONFIG, "linkConfiguration"));
            }
            assertEquals(trackerMapString, expectedCount, AuthzModuleTracker.getOperationCount(linkContext));
            AuthzModuleTracker.remove(linkContext);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
    }

    private void stringArgTest(String contextId, boolean withDelegate) throws Exception {
        // ContextId is required to be non null
        try {
            configFactoryProxy.getPolicyConfiguration(null);
            fail("Expected an exception when passed a null contextId");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // if there isn't a PolicyContext set, the method just returns null
        assertNull(configFactoryProxy.getPolicyConfiguration(contextId));

        // Even with a delegate, we do not call the PCF.getPolicyConfiguration methods since nothing
        // has been set in the configuration yet for the Proxy
        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        JakartaPolicyConfigProxy configProxy = configFactoryProxy.getPolicyConfiguration(contextId, false);

        if (withDelegate) {
            // Should have two operations.  getPolicyConfiguration(String, boolean) called
            // on the PCF and reOpen.
            assertEquals(2, AuthzModuleTracker.getOperationCount(contextId));

            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG_FACTORY, "getPolicyConfiguration(String, false)"));
            assertTrue(AuthzModuleTracker.hasOperation(contextId, ModuleType.POLICY_CONFIG, "ctor"));

            AuthzModuleTracker.remove(contextId);
        }

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());

        createLinkedConfigurations(configProxy, contextId, withDelegate);

        populatePolicyConfig(configFactoryProxy, configProxy, withDelegate);

        validatePopulatedPolicyConfig(contextId, configProxy, configProxy.getLinkedConfigurations(), withDelegate);

        // if one has been populated, it should be found
        JakartaPolicyConfigProxy configProxy2 = configFactoryProxy.getPolicyConfiguration(contextId);

        assertSame(configProxy, configProxy2);

        assertFalse(AuthzModuleTracker.getTrackerMapString(), AuthzModuleTracker.hasTrackedData());
    }
}
