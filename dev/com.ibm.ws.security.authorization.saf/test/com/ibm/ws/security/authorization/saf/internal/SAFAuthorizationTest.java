/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.saf.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authorization.AccessDecisionService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.authorization.saf.SAFRoleMapper;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.security.authorization.saf.AccessLevel;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Unit test for the SAF authorization service. The SAF authorization service requires
 * a z/OS native environment; however, this unit test is designed to run on
 * distributed platforms. Hence, the native environment is mocked using JMock.
 *
 * Since the native environment is mocked, we're not actually running these tests
 * against an actual SAF database. Instead, the mockery environment is configured by
 * each test to receive and return expected values for every native call expected
 * during the test.
 *
 * Thus, this unit test is only focused on testing the Java code.
 */
@RunWith(JMock.class)
@SuppressWarnings("unchecked")
public class SAFAuthorizationTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * The mocked SAFAuthorizationService. This mock object handles all of
     * SAFAuthorizationService's native methods.
     */
    protected SAFAuthorizationServiceImpl mockSAFAuthz = null;

    /**
     * Mock ComponentContext used by SAFAuthorizationService.
     */
    protected ComponentContext mockCC = null;

    /**
     * Mock NativeMethodManager used by SAFAuthorizationService.
     */
    protected NativeMethodManager mockNmm = null;

    /**
     * Mock SAFCredentialsService used by SAFAuthorizationService.
     */
    protected SAFCredentialsService mockSAFCS = null;

    @SuppressWarnings("rawtypes")
    protected ServiceReference authorizationTableServiceRef = null;

    @SuppressWarnings("rawtypes")
    protected ServiceReference accessDecisionServiceRef = null;

    /**
     * Mock SAFServiceResult that allows the test code to insert various SAF results.
     */
    protected SAFCredential mockSAFCred = null;

    /**
     * Mock SAFServiceResult that allows the test code to insert various SAF results.
     */
    protected SAFServiceResult mockSSR = null;

    /**
     * Pseudo-mocked SAFRoleMapper.
     */
    protected SAFRoleMapper safRoleMapper;

    /**
     * Need these.
     */
    protected Map<String, Object> safRoleMapperProps;

    /**
     * Default value for <safAuthorization roleMapper="xx" />.
     * Note: Must be kept in sync with com.ibm.ws.security.authorization.saf/metatype.xml.
     */
    protected static final String ROLE_MAPPER_DEFAULT = "com.ibm.ws.security.authorization.saf.internal.SAFRoleMapperImpl";

    /**
     * Default value for <safRoleMapper profilePattern="xx" />.
     * Note: Must be kept in sync with com.ibm.ws.security.authorization.saf/metatype.xml.
     */
    protected static final String PROFILE_PATTERN_DEFAULT = "%profilePrefix%.%resource%.%role%";

    /**
     * Before each test.
     */
    @Before
    public void beforeTest() throws Exception {
        System.setSecurityManager(null);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Create the Mockery environemnt and all the mock objects. Call this method at the
     * beginning of each test, to create a fresh isolated Mockery environment for the test.
     * This makes debugging easier when a test fails, because all the Expectations from
     * previous tests don't get dumped to the console.
     */
    protected void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        ++uniqueMockNameCount;
        mockSAFAuthz = mockery.mock(SAFAuthorizationServiceImpl.class, "SAFAuthorizationService" + uniqueMockNameCount);
        mockCC = mockery.mock(ComponentContext.class, "ComponentContext" + uniqueMockNameCount);
        mockNmm = mockery.mock(NativeMethodManager.class, "NativeMethodManager" + uniqueMockNameCount);
        mockSAFCS = mockery.mock(SAFCredentialsService.class, "SAFCredentialsService" + uniqueMockNameCount);
        mockSSR = mockery.mock(SAFServiceResult.class, "SAFServiceResult" + uniqueMockNameCount);
        mockSAFCred = mockery.mock(SAFCredential.class, "SAFCredential" + uniqueMockNameCount);
        authorizationTableServiceRef = mockery.mock(ServiceReference.class, "authorizationTableServiceRef");
        accessDecisionServiceRef = mockery.mock(ServiceReference.class, "accessDecisionServiceRef");

        // TODO remove when removing beta fencing
        System.setProperty("com.ibm.ws.beta.edition", "true");
    }

    /**
     * Create a SAFAuthorizationService for the unit test. The SAFAuthorizationService
     * impl returned by this method forwards all native method invocations (ntv_*) to the
     * SAFAuthorizationService mock object in this class (mockSAFAuthz).
     */
    protected SAFAuthorizationServiceImpl getSAFAuthorizationServiceMockNative() throws Exception {
        return new SAFAuthorizationServiceImpl() {
            @Override
            protected int ntv_checkAccess(byte[] safCredentialToken,
                                          byte[] resource,
                                          byte[] className,
                                          byte[] applid,
                                          byte[] jvolser,
                                          int accessLevel,
                                          int logOption,
                                          boolean suppressMessage,
                                          boolean fastAuth,
                                          boolean vsam,
                                          byte[] safServiceResultBytes) {
                // Copy the SAFServiceResult bytes from the test into safServiceResultBytes
                // (which the SAFAuthorizationService will use to detect SAF failures).
                System.arraycopy(mockSSR.getBytes(), 0, safServiceResultBytes, 0, safServiceResultBytes.length);

                return mockSAFAuthz.ntv_checkAccess(safCredentialToken,
                                                    resource,
                                                    className,
                                                    applid,
                                                    safServiceResultBytes,
                                                    accessLevel,
                                                    logOption,
                                                    suppressMessage,
                                                    fastAuth,
                                                    vsam,
                                                    safServiceResultBytes);
            }

            @Override
            protected int ntv_isSAFClassActive(byte[] className) {
                return mockSAFAuthz.ntv_isSAFClassActive(className);
            }
        };
    }

    /**
     * Set up the SAFRoleMapper.
     */
    protected SAFRoleMapper loadDefaultSAFRoleMapper() {
        final SAFRoleMapperImpl defaultRoleMapper = new SAFRoleMapperImpl();
        final Dictionary<String, Object> safRoleMapperConfig = new Hashtable<String, Object>();
        safRoleMapperConfig.put(SAFRoleMapperImpl.PROFILE_PATTERN_KEY, PROFILE_PATTERN_DEFAULT);
        safRoleMapperConfig.put("toUpperCase", new Boolean(false));
        defaultRoleMapper.updateConfig((Map<String, Object>) safRoleMapperConfig);
        defaultRoleMapper.setSafCredentialsService(mockSAFCS);
        return defaultRoleMapper;
    }

    /**
     * New up a SAFAuthorizationService and set all its required service references
     * (all of which are mocked).
     */
    protected SAFAuthorizationServiceImpl createSAFAuthorizationService() throws Exception {
        createMockEnv();

        final Map<String, Object> safAuthzConfig = new HashMap<String, Object>();
        safAuthzConfig.put(SAFAuthorizationServiceImpl.ROLE_MAPPER_KEY, ROLE_MAPPER_DEFAULT);

        safRoleMapper = loadDefaultSAFRoleMapper();
        safRoleMapperProps = new Hashtable<String, Object>();
        safRoleMapperProps.put("component.name", ROLE_MAPPER_DEFAULT);

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                // NativeMethodManager.registerNatives is called by SAFAuthorizationService.setNativeMethodManager.
                oneOf(mockNmm).registerNatives(with(equal(SAFAuthorizationServiceImpl.class)));
            }
        });

        // Create the SAFAuthorizationService and inject the dependencies.
        SAFAuthorizationServiceImpl safAuthz = getSAFAuthorizationServiceMockNative();
        safAuthz.setNativeMethodManager(mockNmm);
        safAuthz.setSafCredentialsService(mockSAFCS);
        safAuthz.setSafRoleMapper(safRoleMapper, safRoleMapperProps);
        safAuthz.setAuthorizationTableService(authorizationTableServiceRef);
        safAuthz.setAccessDecisionService(accessDecisionServiceRef);
        safAuthz.activate(mockCC, safAuthzConfig);
        safAuthz.getRoleMapper();

        return safAuthz;
    }

    /**
     * Unset the (mocked) services from SAFAuthorizationService and deactivate it.
     */
    protected void deactivateSAFAuthorizationService(SAFAuthorizationServiceImpl safAuthz) throws Exception {
        safAuthz.unsetSafRoleMapper(safRoleMapper, safRoleMapperProps);
        safAuthz.deactivate(mockCC);
    }

    /**
     * Test for basic lifecycle operations.
     */
    @Test
    public void basicLifecycle() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        deactivateSAFAuthorizationService(safAuthz);
    }

    /**
     * Setup common mockery Expectations for SAFAuthorizationService.checkRoles.
     */
    protected void setupCheckRolesExpectations(final byte[] safCredToken,
                                               final byte[] safResultBytes,
                                               final int rc) throws Exception {

        final String profilePrefix = "BBGZDFLT";

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                // checkRoles calls getSAFCredentialTokenBytes to get the native SAF credential token.
                allowing(mockSAFCS).getSAFCredentialTokenBytes(with(equal(mockSAFCred)));
                will(returnValue(safCredToken));

                // Called by SAFRoleMapper.getProfileFromRole()
                allowing(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));

                allowing(mockSAFCred).getUserId();
                will(returnValue("userid"));
            }
        });

        setupCheckAccessExpectations(safCredToken, profilePrefix, safResultBytes, rc);
    }

    /**
     * Setup common mockery Expectations for SAFAuthorizationService.checkAccess.
     */
    protected void setupCheckAccessExpectations(final byte[] safCredToken,
                                                final String profilePrefix,
                                                final byte[] safResultBytes,
                                                final int rc) throws Exception {

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                // Use the mock SAFServiceResult to copy in the safResultBytes for the test.
                oneOf(mockSSR).getBytes();
                will(returnValue(safResultBytes));

                // Native method called by checkAccess.
                oneOf(mockSAFAuthz).ntv_checkAccess(with(equal(safCredToken)),
                                                    with(any(byte[].class)),
                                                    with(any(byte[].class)),
                                                    with(any(byte[].class)),
                                                    with(any(byte[].class)),
                                                    with(any(int.class)),
                                                    with(any(int.class)),
                                                    with(any(boolean.class)),
                                                    with(any(boolean.class)),
                                                    with(any(boolean.class)),
                                                    with(equal((safResultBytes))));
                will(returnValue(rc));
            }
        });
    }

    /**
     * Setup a SecurityManager.
     */
    protected void setupSecurityManager(final boolean pass) {
        final SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                if (!pass && permission.getName().equals("safAuthorizationService")) {
                    throw new SecurityException();
                }
            }
        };
        System.setSecurityManager(sm);
    }

    /**
     * Test method for SAFAuthorizationService.isEveryoneGranted.
     * Test getDefaultCredential fails.
     */
    @Test
    public void isEveryoneGranted_getDefaultCredentialFailure() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        mockery.checking(new Expectations() {
            {
                // SAFCredentialsService.getDefaultCredential shall throw a SAFException.
                oneOf(mockSAFCS).getDefaultCredential();
                will(throwException(new SAFException("getDefaultCredential failed")));
            }
        });

        assertFalse(safAuthz.isEveryoneGranted("snoop", new ArrayList<String>() {
            {
                add("user");
            }
        }));
    }

    /**
     * Test method for SAFAuthorizationService.isEveryoneGranted.
     * Passing a null resource shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isEveryoneGranted_nullResource() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isEveryoneGranted(null, new ArrayList<String>() {
            {
                add("user");
            }
        });
    }

    /**
     * Test method for SAFAuthorizationService.isEveryoneGranted.
     * Passing a null requiredRoles list shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isEveryoneGranted_nullRequiredRoles() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isEveryoneGranted("snoop", null);
    }

    /**
     * Test that SAFAuthorizationService.isEveryoneGranted returns true when
     * requiredRoles is empty.
     */
    public void isEveryoneGranted_emptyRequiredRoles() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        assertTrue(safAuthz.isEveryoneGranted(null, new ArrayList<String>()));
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection<String>, Subject).
     * Passing a null resource shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_RRS_nullResource() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized(null, new ArrayList<String>() {
            {
                add("user");
            }
        }, null);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection<String>, Subject).
     * Passing a null requiredRoles list shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_RRS_nullRequiredRoles() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized("snoop", (Collection<String>) null, null);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, String, AccessLevel).
     * Passing a null resource shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_CRA_nullResource() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized("SERVER", null, AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, String, AccessLevel).
     * Passing a null className shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_CRA_nullClass() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized(null, null, AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(Subject, String, String, AccessLevel).
     * Passing a null resource shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_SCRA_nullResource() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized(new Subject(), "SERVER", null, AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(Subject, String, String, AccessLevel).
     * Passing a null className shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_SCRA_nullClass() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized(new Subject(), null, "RESOURCE", AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(SAFCredential, String, String, AccessLevel).
     * Passing a null resource shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_FCRA_nullResource() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized((SAFCredential) null, "SERVER", null, AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(SAFCredential, String, String, AccessLevel).
     * Passing a null className shall result in a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void isAuthorized_FCRA_nullClass() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized((SAFCredential) null, null, "RESOURCE", AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection , Subject).
     * Passing '**' will return true.
     */
    @Test
    public void isAuthorized_allAuth() throws Exception {

        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getSAFCredentialFromSubject(with(any(Subject.class)));
                will(returnValue(mockSAFCred));

                oneOf(mockSAFCred).isAuthenticated();
                will(returnValue(true));
            }
        });

        List<String> roles = new ArrayList<String>();
        roles.add("**");
        assertTrue(safAuthz.isAuthorized("RESOURCE", roles, new Subject()));
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection , Subject).
     * Passing a null className shall result in a NPE.
     */
    @Test
    public void isAuthorized_testRole() throws Exception {

        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        List<String> roles = new ArrayList<String>();
        roles.add("test");

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 0, 0, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // all zeros for success.

        setupCheckRolesExpectations(new byte[0], safResultBytes, 0);

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getSAFCredentialFromSubject(with(any(Subject.class)));
                will(returnValue(mockSAFCred));

                oneOf(mockSAFCred).isAuthenticated();
                will(returnValue(true));
            }
        });

        assertTrue(safAuthz.isAuthorized("RESOURCE", roles, new Subject()));
    }

    @Test(expected = NullPointerException.class)
    public void isAuthorizedToDataset_volserNull() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorizedToDataset("MVSUSER1", "MSTONE1.DATASET", null, false, AccessLevel.READ, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isAuthorizedToDataset_volserTooLong() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorizedToDataset("MVSUSER1", "MSTONE1.DATASET", "LONGVOLSER", false, AccessLevel.READ, null, false);
    }

    @Test(expected = NullPointerException.class)
    public void isAuthorizedToDataset_resourceNameNull() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorizedToDataset("MVSUSER1", null, "VOLSER", false, AccessLevel.READ, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isAuthorizedToDataset_resourceNameTooLong() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        String resourceName = "THIS.RESOURCE.NAME.IS.GREATER.THAN.FORTYFOUR.CHARACTERS";
        safAuthz.isAuthorizedToDataset("MVSUSER1", resourceName, "VOLSER", false, AccessLevel.READ, null, false);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection , Subject).
     * Use management authorization for Collective.
     */
    @Test
    public void isAuthorized_managementCollective() throws Exception {
        String accessId = "server:collective/dc=com.ibm.ws.collective,o=collectiveUUID,ou=collectiveRole,cn=controllerRoot";
        assertManagementAuthorizationTableIsUsedToAuthorize(accessId);
    }

    private void assertManagementAuthorizationTableIsUsedToAuthorize(String accessId) throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        Set<String> requiredRoles = createRequiredRoles(accessId);
        String resourceName = "com.ibm.ws.management.security.resource";
        Subject subject = createTestSubjectForAccessId(accessId);
        noSAFCredentialInSubjectForCollectiveOrODR();
        userHasAccessToRequiredRolesInManagementAuthorizationTable(resourceName, requiredRoles, accessId, subject);

        assertTrue(safAuthz.isAuthorized(resourceName, requiredRoles, subject));
    }

    private Set<String> createRequiredRoles(String accessId) {
        String realm = AccessIdUtil.getRealm(accessId);
        Set<String> requiredRoles = new HashSet<String>();
        requiredRoles.add("Administrator");
        if ("odr".equals(realm)) {
            requiredRoles.add("allAuthenticatedUsers");
        }
        return requiredRoles;
    }

    private Subject createTestSubjectForAccessId(final String accessId) throws CredentialDestroyedException, CredentialExpiredException {
        final WSCredential wsCredential = mockery.mock(WSCredential.class);
        mockery.checking(new Expectations() {
            {
                one(wsCredential).getAccessId();
                will(returnValue(accessId));
            }
        });
        Subject subject = new Subject();
        subject.getPublicCredentials().add(wsCredential);
        return subject;
    }

    private void noSAFCredentialInSubjectForCollectiveOrODR() {
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getSAFCredentialFromSubject(with(any(Subject.class)));
                will(returnValue(null));
            }
        });
    }

    private void userHasAccessToRequiredRolesInManagementAuthorizationTable(final String resourceName, final Set<String> requiredRoles, final String accessId,
                                                                            final Subject subject) {
        final RoleSet assignedRoles = new RoleSet(requiredRoles);
        final AccessDecisionService accessDecisionService = mockery.mock(AccessDecisionService.class);
        final AuthorizationTableService managementAuthzTableSvc = mockery.mock(AuthorizationTableService.class);
        mockery.checking(new Expectations() {
            {
                one(mockCC).locateService("authorizationTableService", authorizationTableServiceRef);
                will(returnValue(managementAuthzTableSvc));
                one(mockCC).locateService("accessDecisionService", accessDecisionServiceRef);
                will(returnValue(accessDecisionService));
                one(managementAuthzTableSvc).getRolesForSpecialSubject(resourceName, AuthorizationTableService.ALL_AUTHENTICATED_USERS);
                if ("collective".equals(AccessIdUtil.getRealm(accessId))) {
                    will(returnValue(null));
                    one(accessDecisionService).isGranted(resourceName, requiredRoles, null, subject);
                    will(returnValue(false));
                } else {
                    Set<String> roles = new HashSet<String>();
                    roles.add("allAuthenticatedUsers");
                    RoleSet roleSet = new RoleSet(roles);
                    will(returnValue(roleSet));
                    one(accessDecisionService).isGranted(resourceName, requiredRoles, roleSet, subject);
                    will(returnValue(true));
                }
            }
        });

        mockery.checking(new Expectations() {
            {
                allowing(managementAuthzTableSvc).getRolesForAccessId(resourceName, accessId);
                will(returnValue(assignedRoles));
                allowing(accessDecisionService).isGranted(resourceName, requiredRoles, assignedRoles, subject);
                will(returnValue(true));
            }
        });
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, Collection , Subject).
     * Use management authorization for ODR.
     */
    @Test
    public void isAuthorized_managementODR() throws Exception {
        String accessId = "user:odr/dc=com.ibm.ws.dynamic.routing,ou=dynamicrouting,cn=WebServer";
        assertManagementAuthorizationTableIsUsedToAuthorize(accessId);
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test ntv_checkAccess returns 0 (success).
     */
    @Test
    public void checkRoles_success() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 0, 0, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // all zeros for success.

        setupCheckRolesExpectations(new byte[0], safResultBytes, 0);

        assertTrue(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("user");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test ntv_checkAccess returns 0 (success).
     */
    @Test
    public void checkRoles_success_starstar() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 0, 0, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // all zeros for success.

        setupCheckRolesExpectations(new byte[0], safResultBytes, 0);

        assertTrue(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("_starstar_");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test ntv_checkAccess returns 8 and SAFServiceResult indicates an expected failure.
     */
    @Test
    public void checkRoles_expectedSAFFailure() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 8, 8, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // 8/8/0 == user NOT authorized

        setupCheckRolesExpectations(new byte[0], safResultBytes, 8);

        assertFalse(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("user");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test ntv_checkAccess returns 8 and SAFServiceResult indicates an expected failure.
     */
    @Test
    public void checkRoles_expectedSAFFailure_starstar() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 8, 8, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // 8/8/0 == user NOT authorized

        setupCheckRolesExpectations(new byte[0], safResultBytes, 8);

        assertFalse(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("_starstar_");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test ntv_checkAccess returns 4 and SAFServiceResult indicates a severe error.
     */
    @Test
    public void checkRoles_severeSAFFailure() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 4, 0, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // 4/0/0 = RACF not installed.

        setupCheckRolesExpectations(new byte[0], safResultBytes, 8);

        assertFalse(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("user");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test isAuthorized path for empty role list. checkRoles shall return true if
     * the role list is empty.
     */
    @Test
    public void checkRoles_EmptyList() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        assertTrue(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>(), null));
    }

    /**
     * Test method for SAFAuthorizationService.checkRoles.
     * Test that a null SAFCredentialToken behaves OK, at least in the Java code.
     * The null SAFCredentialToken should cause the native code to throw a NPE.
     */
    @SuppressWarnings("serial")
    @Test
    public void checkRoles_NullSAFCredentialToken() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();

        final byte[] safResultBytes = createSAFServiceResultBytes(0, 4, 0, 0, SAFServiceResult.SAFService.RACROUTE_AUTH); // 4/0/0 = RACF not installed.

        setupCheckRolesExpectations(null, safResultBytes, 8);

        assertFalse(safAuthz.checkRoles(mockSAFCred, "snoop", new ArrayList<String>() {
            {
                add("user");
            }
        }, null));
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(String, String, AccessLevel).
     * Activate SecurityManager and fail checkPermission.
     */
    @Test(expected = SecurityException.class)
    public void isAuthorized_CRA_SecurityManagerFail() throws Exception {
        setupSecurityManager(false);
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized("SERVER", "MYRESOURCE", AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(Subject, String, String, AccessLevel).
     * Activate SecurityManager and fail checkPermission.
     */
    @Test(expected = SecurityException.class)
    public void isAuthorized_SCRA_SecurityManagerFail() throws Exception {
        setupSecurityManager(false);
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized(new Subject(), "SERVER", "MYRESOURCE", AccessLevel.READ);
    }

    /**
     * Test method for SAFAuthorizationService.isAuthorized(Subject, String, String, AccessLevel).
     * Activate SecurityManager and fail checkPermission.
     */
    @Test(expected = SecurityException.class)
    public void isAuthorized_FCRA_SecurityManagerFail() throws Exception {
        setupSecurityManager(false);
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        safAuthz.isAuthorized((SAFCredential) null, "SERVER", "MYRESOURCE", AccessLevel.READ);
    }

    /**
     * Test success path for isSAFClassActive
     */
    @Test
    public void isSAFClassActivePass_test() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFAuthz).ntv_isSAFClassActive(with(any(byte[].class)));
                will(returnValue(1));
            }
        });
        assertTrue(safAuthz.isSAFClassActive("EJBROLE"));
    }

    /**
     * Test failure path for isSAFClassActive
     */
    @Test
    public void isSAFClassActiveFail_test() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFAuthz).ntv_isSAFClassActive(with(any(byte[].class)));
                will(returnValue(0));
            }
        });
        assertFalse(safAuthz.isSAFClassActive("EJBLOL"));
    }

    /**
     * Test exception path for isSAFClassActive
     */
    @Test(expected = RuntimeException.class)
    public void isSAFClassActiveException_test() throws Exception {
        SAFAuthorizationServiceImpl safAuthz = createSAFAuthorizationService();
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFAuthz).ntv_isSAFClassActive(with(any(byte[].class)));
                will(returnValue(-1));
            }
        });
        safAuthz.isSAFClassActive(null);
    }

    /**
     * Populate a SAFServiceResult byte[] with the given rc/rsn codes.
     *
     * Note: If the internal format of SAFServiceResult ever changes,
     * then this method will have to change too.
     */
    private static byte[] createSAFServiceResultBytes(int wasRc, int safRc, int racfRc, int racfRsn, SAFServiceResult.SAFService safService) {

        SAFServiceResult safResult = new SAFServiceResult();
        IntBuffer ibuff = ByteBuffer.wrap(safResult.getBytes()).asIntBuffer();
        ibuff.put(wasRc);
        ibuff.put(safRc);
        ibuff.put(racfRc);
        ibuff.put(racfRsn);
        ibuff.put(safService.getServiceCode());

        return safResult.getBytes();
    }
}
