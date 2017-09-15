/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class QuickStartSecurityTest {
    private final Mockery mock = new JUnit4Mockery();
    private final BundleContext bc = mock.mock(BundleContext.class);

    private final ServiceReference<UserRegistry> otherURConfigRef = mock.mock(ServiceReference.class, "otherConfigRef");
    private final ServiceReference<UserRegistry> yetAnotherURConfigRef = mock.mock(ServiceReference.class, "yetAnotherURConfigRef");
    private final ServiceReference<ManagementRole> otherRoleRef = mock.mock(ServiceReference.class, "otherRoleRef");
    private final ServiceReference<ManagementRole> yetAnotherRoleRef = mock.mock(ServiceReference.class, "yetAnotherRoleRef");

    private final ServiceRegistration<UserRegistry> mgmtURConfigReg = mock.mock(ServiceRegistration.class, "mgmtConfigReg");
    private final ServiceRegistration<ManagementRole> mgmtRoleReg = mock.mock(ServiceRegistration.class, "mgmtRoleReg");

    private QuickStartSecurity quickStartSecurity;

    @Rule
    public TestName name = new TestName();

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {

                allowing(otherURConfigRef).compareTo(otherURConfigRef);
                will(returnValue(0));
                allowing(otherURConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(67890L));
                allowing(otherURConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(yetAnotherURConfigRef).compareTo(otherURConfigRef);
                will(returnValue(1));
                allowing(otherURConfigRef).compareTo(yetAnotherURConfigRef);
                will(returnValue(1));
                allowing(yetAnotherURConfigRef).compareTo(yetAnotherURConfigRef);
                will(returnValue(0));
                allowing(yetAnotherURConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(54321L));
                allowing(yetAnotherURConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(otherRoleRef).compareTo(otherRoleRef);
                will(returnValue(0));
                allowing(otherRoleRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(67890L));
                allowing(otherRoleRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(yetAnotherRoleRef).compareTo(yetAnotherRoleRef);
                will(returnValue(0));
                allowing(yetAnotherRoleRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(54321L));
                allowing(yetAnotherRoleRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
        quickStartSecurity = new QuickStartSecurity();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    private QuickStartSecurityConfig config(final String user, final String pw, final String[] urs, final String[] ars) {
        return new QuickStartSecurityConfig() {

            //osgi object converter converts null to "" for known types.
            @Override
            public String userName() {
                return user == null ? "" : user;
            }

            @Override
            public SerializableProtectedString userPassword() {
                return pw == null ? null : new SerializableProtectedString(pw.toCharArray());
            }

            @Override
            public String[] UserRegistry() {
                return urs == null ? new String[] {} : urs;
            }

            @Override
            public String[] ManagementRole() {
                return ars == null ? new String[] {} : ars;
            }
        };
    }

    /**
     * Set up the mock expectations to handle a registration of the
     * quick start security UserRegistry.
     *
     * @param user
     * @param password
     */
    private void registerConfigurationExpectations(final String user, final String password) {
        final Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put("id", QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_ID);
        configProps.put("config.id", QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_ID);
        configProps.put(UserRegistryService.REGISTRY_TYPE, QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_TYPE);
        configProps.put("service.vendor", "IBM");
        configProps.put(QuickStartSecurity.CFG_KEY_USER, user);
        configProps.put(QuickStartSecurity.CFG_KEY_PASSWORD, new ProtectedString(password.toCharArray()));

        mock.checking(new Expectations() {
            {
                one(bc).registerService(with(UserRegistry.class),
                                        with(any(UserRegistry.class)),
                                        with(equal(configProps)));
                will(returnValue(mgmtURConfigReg));
            }
        });
    }

    /**
     * Set up the mock expectations to prevent a registration of the
     * quick start security UserRegistry.
     *
     * @param user defaultAdminUser. If null, the property won't be set for the expectations.
     * @param passwordd efaultAdminPassword. If null, the property won't be set for the expectations.
     */
    private void doesNotRegisterConfigurationExpectations() {
        mock.checking(new Expectations() {
            {
                never(bc).registerService(with(UserRegistry.class),
                                          with(any(UserRegistry.class)),
                                          with(any(Dictionary.class)));
            }
        });
    }

    /**
     * Set up the mock expectations to handle a de-registration of the
     * quick start security UserRegistry.
     */
    private void unregisterConfigurationExpectations() {
        mock.checking(new Expectations() {
            {
                one(mgmtURConfigReg).unregister();
            }
        });
    }

    /**
     * Set up the mock expectations to handle a registration of the
     * QuickStartSecurityAdministratorRole.
     */
    private void registerRoleExpectations() {
        final Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put(ManagementRole.MANAGEMENT_ROLE_NAME, QuickStartSecurity.QUICK_START_ADMINISTRATOR_ROLE_NAME);
        configProps.put("service.vendor", "IBM");

        mock.checking(new Expectations() {
            {
                one(bc).registerService(with(ManagementRole.class),
                                        with(any(QuickStartSecurityAdministratorRole.class)),
                                        with(equal(configProps)));
                will(returnValue(mgmtRoleReg));
            }
        });
    }

    /**
     * Set up the mock expectations to prevent a registration of the
     * quick start security UserRegistry.
     *
     * @param user defaultAdminUser. If null, the property won't be set for the expectations.
     * @param passwordd efaultAdminPassword. If null, the property won't be set for the expectations.
     */
    private void doesNotRegisterRoleExpectations() {
        mock.checking(new Expectations() {
            {
                never(bc).registerService(with(ManagementRole.class),
                                          with(any(QuickStartSecurityAdministratorRole.class)),
                                          with(any(Dictionary.class)));
            }
        });
    }

    /**
     * Set up the mock expectations to handle a de-registration of the
     * QuickStartSecurityAdministratorRole.
     */
    private void unregisterRoleExpectations() {
        mock.checking(new Expectations() {
            {
                one(mgmtRoleReg).unregister();
            }
        });
    }

    /**
     * Check output for no user error message. Clear the streams to indicate it was found.
     */
    private void checkForNoUser() {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userName. Define the missing attributes."));
    }

    /**
     * Check output for no password error message. Clear the streams to indicate it was found.
     */
    private void checkForNoPassword() {
        checkForNoPassword(true);
    }

    /**
     * Check output for no password error message.
     * <p>
     * Clear the streams if resetStreams is true.
     */
    private void checkForNoPassword(boolean resetStreams) {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0900E: The <quickStartSecurity> element is missing required attributes: userPassword. Define the missing attributes."));
    }

    /**
     * Check output for another registry error. Clear the streams to indicate it was found.
     */
    private void checkForAnotherRegistry() {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0901E: The <quickStartSecurity> configuration will be ignored as another UserRegistry is configured."));
    }

    /**
     * Check output for another registry error. Clear the streams to indicate it was found.
     */
    private void checkForOtherManagementBindings() {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0902E: The <quickStartSecurity> configuration will be ignored "
                                                 + "as the management security authorization bindings are explicitly defined."));
    }

    @Test
    public void activate_noConfigurationRegistersURFactory() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("", null, null, null));
    }

    @Test
    public void activate_incompleteConfigurationNoPassword() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", null, null, null));

        checkForNoPassword();
    }

    @Test
    public void activate_incompleteConfigurationEmptyPassword() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "", null, null));

        checkForNoPassword();
    }

    @Test
    public void activate_incompleteConfigurationWhiteSpacePassword() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "  ", null, null));

        checkForNoPassword();
    }

    @Test
    public void activate_incompleteConfigurationNoName() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config(null, "adminPassword", null, null));

        checkForNoUser();
    }

    @Test
    public void activate_incompleteConfigurationEmptyName() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        Map<String, Object> props = new HashMap<String, Object>();
        quickStartSecurity.activate(bc, config("", "adminPassword", null, null));

        checkForNoUser();
    }

    @Test
    public void activate_incompleteConfigurationWhiteSpaceName() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("  ", "adminPassword", null, null));

        checkForNoUser();
    }

    @Test
    public void activate_completeConfiguration() {

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, null));
    }

    @Test
    public void activate_incompleteConfigurationWithAnotherRegistry() {

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.setUserRegistry(otherURConfigRef);
        quickStartSecurity.activate(bc, config("adminUser", null, new String[] { "otherURPid" }, null));

        checkForNoPassword(false);
        checkForAnotherRegistry();
    }

    @Test
    public void activate_otherURConfigWithCompleteConfiguration() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid" }, null));

        checkForAnotherRegistry();
    }

    @Test
    public void activate_otherManagementRoleWithCompleteConfiguration() {
        quickStartSecurity.setManagementRole(otherRoleRef);

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, new String[] { "otherARPid" }));

        checkForOtherManagementBindings();
    }

    @Test
    public void activate_otherUserRegistryConfigAndManagementRoleWithCompleteConfiguration() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);
        quickStartSecurity.setManagementRole(otherRoleRef);

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid" }, new String[] { "otherARPid" }));

        checkForAnotherRegistry();
    }

    @Test
    public void deactivate_unregistersURFactory() {

        Map<String, Object> props = new HashMap<String, Object>();
        quickStartSecurity.activate(bc, config(null, null, null, null));

        mock.checking(new Expectations() {
            {
                never(mgmtURConfigReg).unregister();
                never(mgmtRoleReg).unregister();
            }
        });
        quickStartSecurity.deactivate();
    }

    /**
     * deactivate should unregister the
     * UserRegistry (if it is registered).
     */
    @Test
    public void deactivate_unregistersURFactoryAndConfig() {

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, null));

        unregisterConfigurationExpectations();
        unregisterRoleExpectations();
        quickStartSecurity.deactivate();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#setUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setUserRegistry_otherURConfigWithoutPriorRegistration() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#setUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setUserRegistry_yetAnotherURConfigRef() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid" }, null));

        checkForAnotherRegistry();

        mock.checking(new Expectations() {
            {
                never(mgmtURConfigReg).unregister();
            }
        });
        quickStartSecurity.setUserRegistry(yetAnotherURConfigRef);

        checkForAnotherRegistry();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#setUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setUserRegistry_otherURConfigWithPriorRegistration() {

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(QuickStartSecurity.CFG_KEY_USER, "adminUser");
        props.put(QuickStartSecurity.CFG_KEY_PASSWORD, new ProtectedString("adminPassword".toCharArray()));
        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, null));

        unregisterConfigurationExpectations();
        unregisterRoleExpectations();
        quickStartSecurity.modify(config("adminUser", "adminPassword", new String[] { "otherURPid" }, null));
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        checkForAnotherRegistry();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#unsetUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetUserRegistry_afterDeactivateSuchNoBundleContext() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid" }, null));

        checkForAnotherRegistry();

        quickStartSecurity.deactivate();

        quickStartSecurity.unsetUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#unsetUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetUserRegistry_stillHasNonDefaultConfig() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);
        quickStartSecurity.setUserRegistry(yetAnotherURConfigRef);

        doesNotRegisterConfigurationExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid", "yetAnotherURPid" }, null));

        checkForAnotherRegistry();

        quickStartSecurity.modify(config("adminUser", "adminPassword", new String[] { "yetAnotherURPid" }, null));
        quickStartSecurity.unsetUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#unsetUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetUserRegistry_otherURConfigRefWithNoUserProp() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();

        quickStartSecurity.activate(bc, config(null, "adminPassword", new String[] { "yetAnotherURPid" }, null));

        checkForNoUser();

        quickStartSecurity.modify(config(null, "adminPassword", null, null));
        quickStartSecurity.unsetUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#unsetUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetUserRegistry_otherURConfigRefWithNoPasswordProp() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();
        doesNotRegisterRoleExpectations();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(QuickStartSecurity.CFG_KEY_USER, "adminUser");
        quickStartSecurity.activate(bc, config("adminUser", null, new String[] { "yetAnotherURPid" }, null));

        checkForNoPassword();

        quickStartSecurity.modify(config("adminUser", null, null, null));
        quickStartSecurity.unsetUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#unsetUserRegistry(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetUserRegistry_otherURConfigRefSetsMgmtURConfig() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "yetAnotherURPid" }, null));

        checkForAnotherRegistry();

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.modify(config("adminUser", "adminPassword", null, null));
        quickStartSecurity.unsetUserRegistry(otherURConfigRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_noProps() {

        quickStartSecurity.activate(bc, config(null, null, null, null));

        quickStartSecurity.modify(config(null, null, null, null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_noPassword() {

        quickStartSecurity.activate(bc, config(null, null, null, null));

        quickStartSecurity.modify(config("adminUser", null, null, null));

        checkForNoPassword();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_noUser() {

        quickStartSecurity.activate(bc, config(null, null, null, null));

        quickStartSecurity.modify(config(null, "adminPassword", null, null));

        checkForNoUser();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_goodPropsWhenRegistered() {

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, null));

        final Dictionary<String, Object> newProps = new Hashtable<String, Object>();
        newProps.put("id", QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_ID);
        newProps.put("config.id", QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_ID);
        newProps.put(UserRegistryService.REGISTRY_TYPE, QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_TYPE);
        newProps.put("service.vendor", "IBM");
        newProps.put(QuickStartSecurity.CFG_KEY_USER, "adminUser2");
        newProps.put(QuickStartSecurity.CFG_KEY_PASSWORD, new ProtectedString("adminPassword2".toCharArray()));
        mock.checking(new Expectations() {
            {
                one(mgmtURConfigReg).setProperties(with(equal(newProps)));
            }
        });

        unregisterRoleExpectations();
        registerRoleExpectations();

        quickStartSecurity.modify(config("adminUser2", "adminPassword2", null, null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_goodPropsWhenNotRegisteredAndNoOtherConfig() {

        quickStartSecurity.activate(bc, config(null, null, null, null));

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.modify(config("adminUser", "adminPassword", null, null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_goodPropsWhenNotRegisteredWithOtherConfig() {
        quickStartSecurity.setUserRegistry(otherURConfigRef);

        doesNotRegisterConfigurationExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", new String[] { "otherURPid" }, null));

        checkForAnotherRegistry();

        quickStartSecurity.modify(config("adminUser2", "adminPassword2", new String[] { "otherURPid" }, null));

        checkForAnotherRegistry();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.QuickStartSecurityRegistry#modify(java.util.Map)}.
     */
    @Test
    public void modify_incompletePropsWhenRegistered() {

        registerConfigurationExpectations("adminUser", "adminPassword");
        registerRoleExpectations();

        quickStartSecurity.activate(bc, config("adminUser", "adminPassword", null, null));

        mock.checking(new Expectations() {
            {
                never(mgmtURConfigReg).setProperties(with(any(Dictionary.class)));
                one(mgmtURConfigReg).unregister();
                one(mgmtRoleReg).unregister();
            }
        });

        quickStartSecurity.modify(config(null, null, null, null));
    }
}
