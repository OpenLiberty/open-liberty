/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.appbnd.internal.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.security.SecurityRoles;
import com.ibm.ws.javaee.dd.appbnd.Group;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.ws.javaee.dd.appbnd.SpecialSubject;
import com.ibm.ws.javaee.dd.appbnd.User;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.appbnd.internal.delegation.DefaultDelegationProvider;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.adaptable.module.Container;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class AppBndAuthorizationTableServiceTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final static String appName = "myApp";
    private final static String realm = "The Realm";

    private final static String userName1 = "user1";
    private final static String userAccessId1 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, userName1);
    private final static String userAccessId1UpperCase = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, userName1.toUpperCase());
    private final static String userName2 = "user2";
    private final static String userAccessId2 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, userName2);
    private final static String userName3 = "user3";
    private final static String userAccessId3 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, realm, userName3);

    private final static String groupName1 = "group1";
    private final static String groupAccessId1 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupName1);
    private final static String groupAccessId1UpperCase = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupName1.toUpperCase());
    private final static String groupName2 = "group2";
    private final static String groupAccessId2 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupName2);
    private final static String groupName3 = "group3";
    private final static String groupAccessId3 = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupName3);

    private final static String roleName1 = "roleName1";
    private final static String roleName2 = "roleName2";

    private final static String KEY_SECURITY_SERVICE = "securityService";
    private final static String KEY_CONFIG_ADMIN = "configurationAdmin";
    private final static String KEY_LDAP_REGISTRY = "(service.factoryPid=com.ibm.ws.security.registry.ldap.config)";
    private final static String KEY_IGNORE_CASE = "ignoreCase";

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final BundleContext bc = mock.mock(BundleContext.class);
    private final ServiceRegistration<DelegationProvider> delegationRegistration = mock.mock(ServiceRegistration.class);
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class);
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final ServiceReference<ConfigurationAdmin> car = mock.mock(ServiceReference.class, "ConfigurationAdmin");
    private final ConfigurationAdmin ca = mock.mock(ConfigurationAdmin.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);

    private final ApplicationMetaData appMetaData = mock.mock(ApplicationMetaData.class);
    private final J2EEName appJ2EEName = mock.mock(J2EEName.class);
    private final Container appContainer = mock.mock(Container.class, "appContainer");
    private final MetaDataEvent<ApplicationMetaData> appEvent = mock.mock(MetaDataEvent.class);
    private final ModuleInfo moduleInfo = mock.mock(ModuleInfo.class);
    private final SecurityRoles securityRoles = mock.mock(SecurityRoles.class);
    private final List<SecurityRole> listSecurityRoles = new ArrayList<SecurityRole>();
    private final List<SpecialSubject> listSpecialSubjects = new ArrayList<SpecialSubject>();
    private final SpecialSubject specialSubject = mock.mock(SpecialSubject.class);
    private final User user1 = mock.mock(User.class, "user1");
    private final User user2 = mock.mock(User.class, "user2");
    private final Group group1 = mock.mock(Group.class, "group1");
    private final Group group2 = mock.mock(Group.class, "group2");
    private final SecurityRole secRole1 = mock.mock(SecurityRole.class, "secRole1");
    private final SecurityRole secRole2 = mock.mock(SecurityRole.class, "secRole2");
    private final Configuration lrc = mock.mock(Configuration.class);
    private AppBndAuthorizationTableServiceDouble authzTableListener;

    private final Configuration lrcs[] = { lrc };

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                will(returnValue(bc));
                allowing(bc).getBundle();
                allowing(bc).registerService(with(DelegationProvider.class),
                                             with(any(DefaultDelegationProvider.class)),
                                             with(any(Dictionary.class)));
                will(returnValue(delegationRegistration));

                allowing(cc).locateService(KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));
                allowing(cc).locateService(KEY_CONFIG_ADMIN, car);
                will(returnValue(ca));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
            }
        });

        authzTableListener = new AppBndAuthorizationTableServiceDouble();
        authzTableListener.setSecurityService(securityServiceRef);
        authzTableListener.setConfigurationAdmin(car);
        authzTableListener.activate(cc);
    }

    @After
    public void tearDown() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegationRegistration).unregister();
            }
        });

        authzTableListener.deactivate(cc);
        authzTableListener.unsetSecurityService(securityServiceRef);
        authzTableListener.unsetConfigurationAdmin(car);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#deactivate(org.osgi.service.component.Componentmock)}.
     */
    @Test
    public void deactivateUnregistersDelegationService() {
        mock.checking(new Expectations() {
            {
                one(delegationRegistration).unregister();
            }
        });
        authzTableListener.deactivate(cc);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#appDeployed(com.ibm.ws.container.service.app.deploy.ApplicationInfo)}.
     */
    @Test
    public void applicationStarting_serverXML() throws Exception {
        mock.checking(new Expectations() {
            {
                one(appEvent).getMetaData();
                will(returnValue(appMetaData));
                one(appMetaData).getJ2EEName();
                will(returnValue(appJ2EEName));
                one(appJ2EEName).getApplication();
                will(returnValue(appName));
                one(appEvent).getContainer();
                will(returnValue(appContainer));
                one(appContainer).adapt(SecurityRoles.class);
                will(returnValue(securityRoles));
                allowing(securityRoles).getSecurityRoles();
                //allowing(securityRoles).getSecurityRoles(SecurityRoles.Source.EarApplicationBnd);
            }
        });
        authzTableListener.applicationMetaDataCreated(appEvent);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#appDeployed(com.ibm.ws.container.service.app.deploy.ApplicationInfo)}.
     */
    @Test
    public void applicationStarting_earAppBnd() throws Exception {
        mock.checking(new Expectations() {
            {
                one(appEvent).getMetaData();
                will(returnValue(appMetaData));
                one(appMetaData).getJ2EEName();
                will(returnValue(appJ2EEName));
                one(appJ2EEName).getApplication();
                will(returnValue(appName));
                one(appEvent).getContainer();
                will(returnValue(appContainer));
                one(appContainer).adapt(SecurityRoles.class);
                will(returnValue(securityRoles));
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));
            }
        });
        authzTableListener.applicationMetaDataCreated(appEvent);
    }

    /**
     * appDeployed should issue a warning message when the authorization
     * table is not created.
     */
    @Test
    public void appDeployed_sameApplicationName() throws Exception {
        mock.checking(new Expectations() {
            {
                exactly(2).of(appEvent).getMetaData();
                will(returnValue(appMetaData));
                exactly(2).of(appMetaData).getJ2EEName();
                will(returnValue(appJ2EEName));
                exactly(2).of(appJ2EEName).getApplication();
                will(returnValue(appName));
                exactly(2).of(appEvent).getContainer();
                will(returnValue(appContainer));
                exactly(2).of(appContainer).adapt(SecurityRoles.class);
                will(returnValue(securityRoles));
                allowing(securityRoles).getSecurityRoles();
            }
        });

        authzTableListener.applicationMetaDataCreated(appEvent);
        try {
            authzTableListener.applicationMetaDataCreated(appEvent);
            fail("Excepted StateChangeException when application with the same name is deployed.");
        } catch (MetaDataException e) {
            String expectedMsg = "CWWKS9110E:.*" + appName + ".*";

            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedMsg));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#applicationStopped(java.lang.String)}.
     */
    @Test
    public void applicationMetaDataDestroyed() throws Exception {
        mock.checking(new Expectations() {
            {
                one(appEvent).getMetaData();
                will(returnValue(appMetaData));
                one(appMetaData).getJ2EEName();
                will(returnValue(appJ2EEName));
                one(appJ2EEName).getApplication();
                will(returnValue(appName));
            }
        });
        authzTableListener.applicationMetaDataDestroyed(appEvent);
    }

    private void applicationMetaDataCreated(final MetaDataEvent<ApplicationMetaData> event) throws Exception {
        mock.checking(new Expectations() {
            {
                one(event).getMetaData();
                will(returnValue(appMetaData));
                one(appMetaData).getJ2EEName();
                will(returnValue(appJ2EEName));
                one(appJ2EEName).getApplication();
                will(returnValue(appName));
                one(event).getContainer();
                will(returnValue(appContainer));
                one(appContainer).adapt(SecurityRoles.class);
                will(returnValue(securityRoles));
            }
        });
        authzTableListener.applicationMetaDataCreated(event);
    }

    @Test
    public void getRolesForSpecialSubject_noResourceMatch() {
        assertNull("Null should be returned when the resource doesn't match",
                   authzTableListener.getRolesForAccessId("mismatch", null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_FromEar() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);
        listSpecialSubjects.add(specialSubject);
        final String roleName2 = "roleName2";

        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getSpecialSubjects();
                will(returnValue(new ArrayList<SpecialSubject>()));
                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getSpecialSubjects();
                will(returnValue(listSpecialSubjects));
                allowing(specialSubject).getType();
                will(returnValue(SpecialSubject.Type.ALL_AUTHENTICATED_USERS));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        assertEquals("Roles should only contain roleName2.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName2.",
                   roles.contains(roleName2));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_FromServer() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);
        listSpecialSubjects.add(specialSubject);
        final String roleName2 = "roleName2";

        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));
                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getSpecialSubjects();
                will(returnValue(new ArrayList<SpecialSubject>()));
                // second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getSpecialSubjects();
                will(returnValue(listSpecialSubjects));
                allowing(specialSubject).getType();
                will(returnValue(SpecialSubject.Type.ALL_AUTHENTICATED_USERS));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        assertEquals("Roles should only contain roleName2.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName2.",
                   roles.contains(roleName2));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_noSpecialSubjects() throws Exception {
        listSecurityRoles.add(secRole1);

        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getSpecialSubjects();
                will(returnValue(listSpecialSubjects));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be an empty list when no special subjects are defined.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.ALL_AUTHENTICATED_USERS));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_noMatchingSpecialSubject() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);
        listSpecialSubjects.add(specialSubject);
        final String roleName2 = "roleName2";
        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getSpecialSubjects();
                will(returnValue(new ArrayList<SpecialSubject>()));
                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getSpecialSubjects();
                will(returnValue(listSpecialSubjects));
                allowing(specialSubject).getType();
                will(returnValue(SpecialSubject.Type.ALL_AUTHENTICATED_USERS));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be an empty list when no matching special subjects are found.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.EVERYONE));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_noRoles() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be an empty list when no matching special subjects are found.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.EVERYONE));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForSpecialSubject(java.lang.String, java.lang.String)} .
     */
    @Test
    public void getRolesForSpecialSubject_cached() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);
        listSpecialSubjects.add(specialSubject);
        final String roleName2 = "roleName2";

        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getSpecialSubjects();
                will(returnValue(new ArrayList<SpecialSubject>()));
                // second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getSpecialSubjects();
                will(returnValue(listSpecialSubjects));

                allowing(specialSubject).getType();
                will(returnValue(SpecialSubject.Type.ALL_AUTHENTICATED_USERS));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        assertEquals("Roles should only contain roleName2.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName2.",
                   roles.contains(roleName2));

        // Call again, should get a cache hit
        roles = authzTableListener.getRolesForSpecialSubject(appName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
        assertEquals("Roles should only contain roleName2.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName2.",
                   roles.contains(roleName2));
    }

    @Test
    public void getRolesForAccessId_noResourceMatch() {
        assertNull("Null should be returned when the resource doesn't match",
                   authzTableListener.getRolesForAccessId("mismatch", null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_FromServer() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<User> secRole1Users = new ArrayList<User>();
        secRole1Users.add(user1);
        final List<User> secRole2Users = new ArrayList<User>();
        secRole2Users.add(user2);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getUsers();
                will(returnValue(secRole1Users));

                //first user
                one(user1).getAccessId();
                will(returnValue(null));
                exactly(2).of(user1).getName();
                will(returnValue(userName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueUserId(userName1);
                will(returnValue(userName1));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getUsers();
                will(returnValue(secRole2Users));

                //second user
                one(user2).getName();
                will(returnValue(userName2));
                one(user2).getAccessId();
                will(returnValue(userAccessId2));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForAccessId(appName, userAccessId1);
        assertEquals("Roles should only contain roleName1.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName1.",
                   roles.contains(roleName1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_noMatchingAccessIds() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<User> secRole1Users = new ArrayList<User>();
        secRole1Users.add(user1);
        final List<User> secRole2Users = new ArrayList<User>();
        secRole2Users.add(user2);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getUsers();
                will(returnValue(secRole1Users));

                //first user
                one(user1).getAccessId();
                will(returnValue(null));
                exactly(2).of(user1).getName();
                will(returnValue(userName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueUserId(userName1);
                will(returnValue(userName1));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getUsers();
                will(returnValue(secRole2Users));

                //second user
                one(user2).getName();
                will(returnValue(userName2));
                one(user2).getAccessId();
                will(returnValue(userAccessId2));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty when userAccessId3 is not mapped to any of the roles.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, userAccessId3));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_NoUsers() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<User> noUsers = new ArrayList<User>();

        mock.checking(new Expectations() {
            {
                //first role
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getUsers();
                will(returnValue(noUsers));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getUsers();
                will(returnValue(noUsers));
            }
        });

        applicationMetaDataCreated(appEvent);
        assertEquals("Roles should be empty when there are no users in the security-role element.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, userAccessId1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForGroupAccessId_FromServer() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<Group> secRole1Groups = new ArrayList<Group>();
        secRole1Groups.add(group1);
        final List<Group> secRole2Groups = new ArrayList<Group>();
        secRole2Groups.add(group2);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                //first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getGroups();
                will(returnValue(secRole1Groups));

                //first group
                one(group1).getAccessId();
                will(returnValue(null));
                exactly(2).of(group1).getName();
                will(returnValue(groupName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueGroupId(groupName1);
                will(returnValue(groupName1));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getGroups();
                will(returnValue(secRole2Groups));

                //second group
                one(group2).getName();
                will(returnValue(groupName2));
                one(group2).getAccessId();
                will(returnValue(groupAccessId2));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForAccessId(appName, groupAccessId1);
        assertEquals("Roles should only contain roleName1.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName1.",
                   roles.contains(roleName1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForGroupAccessId_noMatchingGroupAccessIds() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<Group> secRole1Groups = new ArrayList<Group>();
        secRole1Groups.add(group1);
        final List<Group> secRole2Groups = new ArrayList<Group>();
        secRole2Groups.add(group2);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                //first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getGroups();
                will(returnValue(secRole1Groups));

                //first group
                one(group1).getAccessId();
                will(returnValue(null));
                exactly(2).of(group1).getName();
                will(returnValue(groupName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueGroupId(groupName1);
                will(returnValue(groupName1));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getGroups();
                will(returnValue(secRole2Groups));

                //second group
                one(group2).getName();
                will(returnValue(groupName2));
                one(group2).getAccessId();
                will(returnValue(groupAccessId2));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty when groupAccessId3 is not mapped to any of the roles.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, groupAccessId3));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForGroupAccessId_NoGroups() throws Exception {
        listSecurityRoles.add(secRole1);
        listSecurityRoles.add(secRole2);

        final List<Group> noGroups = new ArrayList<Group>();

        mock.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                //first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getGroups();
                will(returnValue(noGroups));

                //second role
                one(secRole2).getName();
                will(returnValue(roleName2));
                one(secRole2).getGroups();
                will(returnValue(noGroups));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty when there are no users in the security-role element.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, groupAccessId1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_NoRoles() throws Exception {
        mock.checking(new Expectations() {
            {
                //first role
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty when there are no users in the security-role element.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, userAccessId1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_FromServer_ignoreCase_True() throws Exception {
        listSecurityRoles.add(secRole1);

        final List<User> secRole1Users = new ArrayList<User>();
        secRole1Users.add(user1);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));

                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));

                one(lrc).getProperties();
                will(returnValue(props));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getUsers();
                will(returnValue(secRole1Users));

                //first user
                one(user1).getAccessId();
                will(returnValue(null));
                exactly(2).of(user1).getName();
                will(returnValue(userName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueUserId(userName1);
                will(returnValue(userName1));
            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForAccessId(appName, userAccessId1UpperCase);
        assertEquals("Roles should only contain roleName1.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName1.",
                   roles.contains(roleName1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForAccessId_FromServer_NoRole_ignoreCase_False() throws Exception {
        listSecurityRoles.add(secRole1);

        final List<User> secRole1Users = new ArrayList<User>();
        secRole1Users.add(user1);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));

                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));

                one(lrc).getProperties();
                will(returnValue(props));

                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                // first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getUsers();
                will(returnValue(secRole1Users));

                //first user
                one(user1).getAccessId();
                will(returnValue(null));
                exactly(2).of(user1).getName();
                will(returnValue(userName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueUserId(userName1);
                will(returnValue(userName1));
            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty since ignoreCase is set as false.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, userAccessId1UpperCase));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForGroupAccessId_FromServer_ignoreCase_True() throws Exception {
        listSecurityRoles.add(secRole1);

        final List<Group> secRole1Groups = new ArrayList<Group>();
        secRole1Groups.add(group1);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                //first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getGroups();
                will(returnValue(secRole1Groups));

                //first group
                one(group1).getAccessId();
                will(returnValue(null));
                exactly(2).of(group1).getName();
                will(returnValue(groupName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueGroupId(groupName1);
                will(returnValue(groupName1));

            }
        });

        applicationMetaDataCreated(appEvent);

        RoleSet roles = authzTableListener.getRolesForAccessId(appName, groupAccessId1UpperCase);
        assertEquals("Roles should only contain roleName1.",
                     roles.size(), 1);
        assertTrue("Roles should only contain roleName1.",
                   roles.contains(roleName1));
    }

    /**
     * Test method for {@link com.ibm.ws.security.appbnd.internal.authorization.AppBndAuthorizationTableService#getRolesForAccessId(java.lang.String, java.lang.String)} .
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    @Test
    public void getRolesForGroupAccessId_FromServer_NoRole_ignoreCase_False() throws Exception {
        listSecurityRoles.add(secRole1);

        final List<Group> secRole1Groups = new ArrayList<Group>();
        secRole1Groups.add(group1);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(listSecurityRoles));

                //first role
                one(secRole1).getName();
                will(returnValue(roleName1));
                one(secRole1).getGroups();
                will(returnValue(secRole1Groups));

                //first group
                one(group1).getAccessId();
                will(returnValue(null));
                exactly(2).of(group1).getName();
                will(returnValue(groupName1));
                one(userRegistry).getRealm();
                will(returnValue(realm));
                one(userRegistry).getUniqueGroupId(groupName1);
                will(returnValue(groupName1));

            }
        });

        applicationMetaDataCreated(appEvent);

        assertEquals("Roles should be empty since ignoreCase is set as false.",
                     RoleSet.EMPTY_ROLESET,
                     authzTableListener.getRolesForAccessId(appName, groupAccessId1UpperCase));
    }

    class AppBndAuthorizationTableServiceDouble extends AppBndAuthorizationTableService {
        public AppBndAuthorizationTableServiceDouble() {
            super();
        }

        @Override
        protected void setSecurityService(ServiceReference<SecurityService> reference) {
            super.setSecurityService(reference);
        }

        @Override
        protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
            super.unsetSecurityService(reference);
        }

        @Override
        protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
            super.setConfigurationAdmin(reference);
        }

        @Override
        protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
            super.unsetConfigurationAdmin(reference);
        }
    }

}
