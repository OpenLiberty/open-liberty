/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.Identity;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.principals.WSIdentity;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.ready.SecurityReadyService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("deprecation")
public class EJBSecurityCollaboratorImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final EJBRequestData requestData = mock.mock(EJBRequestData.class);
    private final EJBMethodMetaData methodMetaData = mock.mock(EJBMethodMetaData.class);
    private final EJBComponentMetaData componentMetaData = mock.mock(EJBComponentMetaData.class);
    private SecurityCookieImpl secCookie = mock.mock(SecurityCookieImpl.class);

    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final SecurityReadyService securityReadyService = mock.mock(SecurityReadyService.class);

    private final ServiceReference<UnauthenticatedSubjectService> unauthSubjSrvRef = mock.mock(ServiceReference.class, "unauthSubjSrvRef");
    private final UnauthenticatedSubjectService unauthSubjSrv = mock.mock(UnauthenticatedSubjectService.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final AtomicServiceReference<SecurityService> securityAtomicServiceRef = mock.mock(AtomicServiceReference.class, "securityAtomicServiceRef");
    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);
    private final AuthorizationService authzService = mock.mock(AuthorizationService.class);
    private final J2EEName jen = mock.mock(J2EEName.class);

    private EJBSecurityCollaboratorImpl ejbSecColl = null;
    private SubjectManager subjectManager;
    private final Map<String, Object> configProps = new HashMap<String, Object>();
    private Subject callerSubject;
    private Subject invocationSubject;
    private Subject delegatedSubject;
    private Subject unauthSubject;
    private Principal callerPrincipal;
    private Collection<String> rolesAllowed = new Vector<String>();

    private final String APP_NAME = "appName";
    private final String METHOD_NAME = "methodName";
    private final String METHOD_SIGNATURE = "signature";
    private final J2EEName name = new J2EENameImpl(APP_NAME, null, null);
    private final String SECURITY_ROLE = "requiredRole";
    private final String RUNAS_ROLE = "runAsRole";

    @Mock
    private SecurityReadyService mockSecurityReadyService;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // ejbSecColl needs to be activated to be useful, this simulates DS
        // creating and starting the class
        configProps.put(EJBSecurityConfigImpl.CFG_KEY_REALM_QUALIFY_USER_NAME, false);
        configProps.put(EJBSecurityConfigImpl.CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS, false);
        rolesAllowed.clear();

        callerSubject = new Subject();
        callerPrincipal = new WSPrincipal("callerSubjectUserName", "accessId", "method");
        callerSubject.getPrincipals().add(callerPrincipal);
        invocationSubject = new Subject();
        invocationSubject.getPrincipals().add(new WSPrincipal("invokedSubjectUserName", "accessId", "method"));
        unauthSubject = new Subject();
        unauthSubject.getPrincipals().add(new WSPrincipal("UNAUTHENTICATED", "UNAUTHENTICATED", "UNAUTHENTICATED"));
        delegatedSubject = new Subject();
        delegatedSubject.getPrincipals().add(new WSPrincipal("delegatedSubjectUserName", "accessId", "method"));

        createSecurityServiceExpectations();
        createComponentContextExpectations();
        createEJBExpectations();

        subjectManager = new SubjectManager();
        ejbSecColl = new EJBSecurityCollaboratorImpl(subjectManager);
        ejbSecColl.setSecurityService(securityServiceRef);
        ejbSecColl.setCredentialService(credentialsServiceRef);
        ejbSecColl.setUnauthenticatedSubjectService(unauthSubjSrvRef);
        ejbSecColl.setSecurityReadyService(securityReadyService);
        ejbSecColl.activate(cc, configProps);
    }

    private void createComponentContextExpectations() {
        when(mockSecurityReadyService.isSecurityReady()).thenReturn(true);
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(EJBSecurityCollaboratorImpl.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));
                allowing(securityReadyService).isSecurityReady();
                will(returnValue(true));
            }
        });
    }

    private void createSecurityServiceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(securityAtomicServiceRef).getService();
                will(returnValue(securityService));
                allowing(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
                allowing(authenticationService).delegate(RUNAS_ROLE, APP_NAME);
                will(returnValue(delegatedSubject));
            }
        });
    }

    private void createAuthzServiceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(securityService).getAuthorizationService();
                will(returnValue(authzService));
            }
        });
    }

    private void createCredentialServiceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(EJBSecurityCollaboratorImpl.KEY_CREDENTIAL_SERVICE, credentialsServiceRef);
                will(returnValue(credentialsService));
            }
        });
    }

    private void createUnauthSubjServiceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(EJBSecurityCollaboratorImpl.KEY_UNAUTHENTICATED_SUBJECT_SERVICE, unauthSubjSrvRef);
                will(returnValue(unauthSubjSrv));
                allowing(unauthSubjSrv).getUnauthenticatedSubject();
                will(returnValue(unauthSubject));
            }
        });
    }

    private void createEJBExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(name));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
            }
        });
    }

    private void createEJBInterfaceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.LOCAL));
            }
        });
    }

    private void createEJBDenyAllExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).isDenyAll();
                will(returnValue(false));
            }
        });
    }

    private void createEJBPermitAllExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).isPermitAll();
                will(returnValue(false));
            }
        });
    }

    private void createEJBRolesAllowedExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).getRolesAllowed();
                will(returnValue(rolesAllowed));
            }
        });
    }

    private void createEJBRunAsExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).isUseSystemPrincipal();
                will(returnValue(false));
                allowing(methodMetaData).isUseCallerPrincipal();
                will(returnValue(false));
                allowing(methodMetaData).getRunAs();
                will(returnValue(RUNAS_ROLE));
            }
        });
    }

    private void createEJBRunAsSystemExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).isUseSystemPrincipal();
                will(returnValue(true));
            }
        });
    }

    private void createEJBRunAsCallerExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).isUseSystemPrincipal();
                will(returnValue(false));
                allowing(methodMetaData).isUseCallerPrincipal();
                will(returnValue(true));
            }
        });
    }

    private void createEJBSecurityExpecations() {
        createEJBInterfaceExpectations();
        createEJBPermitAllExpectations();
        createEJBDenyAllExpectations();
        createEJBRolesAllowedExpectations();
        createEJBRunAsExpectations();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        mock.assertIsSatisfied();
    }

    @Test
    public void testConstructor() {
        EJBSecurityCollaborator<SecurityCookieImpl> ejbSecurityService = new EJBSecurityCollaboratorImpl();
        assertNotNull("There must be a EJBSecurityCollaborator Service", ejbSecurityService);
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_isAuthorized() throws EJBAccessDeniedException {

        createEJBSecurityExpecations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, invocationSubject);
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_permitAll() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBDenyAllExpectations();
        createEJBRunAsExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {

                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getRolesAllowed();
                will(returnValue(rolesAllowed));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
                allowing(authzService).isEveryoneGranted(APP_NAME, rolesAllowed);
                will(returnValue(false));
                allowing(methodMetaData).isPermitAll();
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_roleAllowed() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBSecurityExpecations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(authzService).isEveryoneGranted(APP_NAME, rolesAllowed);
                will(returnValue(false));
                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, invocationSubject);
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_isAuthorized_nullInvocationSubject() throws EJBAccessDeniedException {
        createEJBSecurityExpecations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(null);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, callerSubject);
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be null", null, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original caller subject", callerSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_unprotectedMethod() throws EJBAccessDeniedException {
        createEJBRunAsExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.LIFECYCLE_INTERCEPTOR));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie.", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invoked subject", invocationSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     * if the property is set and the subjects are expired, the cookie should have null subjects.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_withExpiredCreds() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBSecurityExpecations();
        createAuthzServiceExpectations();
        createCredentialServiceExpectations();
        createUnauthSubjServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");
        configProps.put(EJBSecurityConfigImpl.CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS, true);
        ejbSecColl.modified(configProps);

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(credentialsService).isSubjectValid(invocationSubject);
                will(returnValue(false));
                allowing(credentialsService).isSubjectValid(callerSubject);
                will(returnValue(false));

                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, unauthSubject);
                will(returnValue(true));

            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);

        assertEquals("The invocation subject in the cookie should be null because it was expired.", null, secCookie.getInvokedSubject());
        assertEquals("The received subject in the cookie should be null because it was expired.", null, secCookie.getReceivedSubject());

        assertEquals("The invocation subject on the thread shoud be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread shoud be UNAUTHENTICATED", unauthSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test(expected = EJBAccessDeniedException.class)
    public void testPreInvoke_noAuthzService() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBDenyAllExpectations();
        createEJBPermitAllExpectations();
        createEJBRolesAllowedExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");
        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(securityService).getAuthorizationService();
                will(returnValue(null));
            }
        });
        ejbSecColl.preInvoke(requestData);
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test(expected = EJBAccessDeniedException.class)
    public void testPreInvoke_notAuthorized() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBDenyAllExpectations();
        createEJBPermitAllExpectations();
        createEJBRolesAllowedExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(authzService).isEveryoneGranted(APP_NAME, rolesAllowed);
                will(returnValue(false));
                one(authzService).isAuthorized(APP_NAME, rolesAllowed, invocationSubject);
                will(returnValue(false));
            }
        });

        ejbSecColl.preInvoke(requestData);
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_noRoles() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBDenyAllExpectations();
        createEJBPermitAllExpectations();
        createEJBRolesAllowedExpectations();
        createEJBRunAsExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();

        rolesAllowed = null;
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());

    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPreInvoke_emptyRoles() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBDenyAllExpectations();
        createEJBPermitAllExpectations();
        createEJBRolesAllowedExpectations();
        createAuthzServiceExpectations();
        createEJBRunAsExpectations();
        createComponentContextExpectations();

        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the delegated subject", delegatedSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());

    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test(expected = EJBAccessDeniedException.class)
    public void testPreInvoke_DenyAll() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));
                allowing(methodMetaData).getRolesAllowed();
                will(returnValue(rolesAllowed));

                allowing(methodMetaData).isDenyAll();
                will(returnValue(true));
            }
        });

        ejbSecColl.preInvoke(requestData);
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     *
     * @throws EJBAccessDeniedException
     */
    @Test(expected = EJBAccessDeniedException.class)
    public void testPreInvoke_runAsSystem() throws EJBAccessDeniedException {
        createEJBInterfaceExpectations();
        createEJBPermitAllExpectations();
        createEJBDenyAllExpectations();
        createEJBRolesAllowedExpectations();
        createEJBRunAsSystemExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(requestData).getEJBMethodMetaData();
                will(returnValue(methodMetaData));
                allowing(requestData).getMethodArguments();
                will(returnValue(null));
                allowing(methodMetaData).getEJBComponentMetaData();
                will(returnValue(componentMetaData));
                allowing(componentMetaData).getJ2EEName();
                will(returnValue(jen));
                allowing(methodMetaData).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                allowing(methodMetaData).getMethodName();
                will(returnValue(METHOD_NAME));
                allowing(methodMetaData).getMethodSignature();
                will(returnValue(METHOD_SIGNATURE));

                allowing(authzService).isEveryoneGranted(APP_NAME, rolesAllowed);
                will(returnValue(false));
                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, invocationSubject);
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#preInvoke(com.ibm.ws.ejbcontainer.EJBRequestData)}.
     */
    public void testPreInvoke_runAsCaller() {
        createEJBInterfaceExpectations();
        createEJBPermitAllExpectations();
        createEJBDenyAllExpectations();
        createEJBRolesAllowedExpectations();
        createEJBRunAsCallerExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);
        rolesAllowed.add("REQUIRED_ROLE");

        mock.checking(new Expectations() {
            {
                allowing(authzService).isEveryoneGranted(APP_NAME, rolesAllowed);
                will(returnValue(false));
                allowing(methodMetaData).isPermitAll();
                will(returnValue(true));
            }
        });

        secCookie = ejbSecColl.preInvoke(requestData);
        assertEquals("The invocation subject at the beginning of the preinvoke should be saved in the cookie", invocationSubject, secCookie.getInvokedSubject());
        assertEquals("The caller subject at the beginning of the preinvoke should be saved in the cookie", callerSubject, secCookie.getReceivedSubject());
        assertEquals("The invocation subject on the thread should be the caller subject", callerSubject, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread should be the original invocation subject", invocationSubject, subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#postInvoke(com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object)}.
     * Test that the subjects in the cookie are restored on the thread.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPostInvoke() throws EJBAccessDeniedException {
        subjectManager.setCallerSubject(null);
        subjectManager.setInvocationSubject(null);
        secCookie = new SecurityCookieImpl(invocationSubject, callerSubject);
        ejbSecColl.postInvoke(requestData, secCookie);

        assertEquals("The invocation subject on the thread shoud be the one in the cookie", secCookie.getInvokedSubject(), subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread shoud be the one in the cookie", secCookie.getReceivedSubject(), subjectManager.getCallerSubject());

    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#postInvoke(com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object)}.
     * Test that the subjects in the cookie are restored on the thread.
     *
     * @throws EJBAccessDeniedException
     */
    @Test
    public void testPostInvoke_noCookie() throws EJBAccessDeniedException {
        subjectManager.setCallerSubject(null);
        subjectManager.setInvocationSubject(null);
        ejbSecColl.postInvoke(requestData, null);

        assertEquals("The invocation subject on the thread shoud be null", null, subjectManager.getInvocationSubject());
        assertEquals("The caller subject on the thread shoud be null", null, subjectManager.getCallerSubject());

    }

    /**
     * Test method for {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#getCallerIdentity(com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object)}.
     */
//    @Test
    public void testGetCallerIdentity() {
        subjectManager.setCallerSubject(callerSubject);
        assertEquals("The caller identity should be the principal of the caller subject on the thread.", ejbSecColl.getCallerIdentity(null, null, null),
                     new WSIdentity(callerPrincipal.getName()));
        assertTrue("The caller identity should be an insance of Identity", ejbSecColl.getCallerIdentity(null, null, null) instanceof Identity);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#getCallerPrincipal(com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, boolean, boolean)}.
     */
//    @Test
    public void testGetCallerPrincipal() {
        subjectManager.setCallerSubject(callerSubject);
        assertEquals("The caller principal should be the principal of the caller subject on the thread.", callerPrincipal, ejbSecColl.getCallerPrincipal(null, null, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#getCallerPrincipal(com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, boolean, boolean)}.
     * The caller principal should be null when there is no caller subject on the thread.
     */
    @Test
    public void testGetCallerPrincipal_noCallerOnThread() {
        subjectManager.setCallerSubject(null);
        assertEquals("The caller principal should be null when there is no caller subject on the thread.", null, ejbSecColl.getCallerPrincipal(null, null, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#isCallerInRole(com.ibm.ws.ejbcontainer.EJBComponentMetaData, com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, java.lang.String, java.lang.String)}
     *
     */
    @Test
    public void testIsCallerInRole_true() {
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        rolesAllowed.add(SECURITY_ROLE);

        mock.checking(new Expectations() {
            {
                one(authzService).isAuthorized(APP_NAME, rolesAllowed, callerSubject);
                will(returnValue(true));
            }
        });

        boolean inRole = ejbSecColl.isCallerInRole(componentMetaData, requestData, secCookie, SECURITY_ROLE, null);
        assertTrue("The caller should be in the role.", inRole);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#isCallerInRole(com.ibm.ws.ejbcontainer.EJBComponentMetaData, com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, java.lang.String, java.lang.String)}
     *
     */
    @Test
    public void testIsCallerInRole_false() {
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        rolesAllowed.add(SECURITY_ROLE);

        mock.checking(new Expectations() {
            {
                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, callerSubject);
                will(returnValue(false));
            }
        });

        boolean inRole = ejbSecColl.isCallerInRole(componentMetaData, requestData, secCookie, SECURITY_ROLE, null);
        assertFalse("The caller should NOT be in the role.", inRole);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#isCallerInRole(com.ibm.ws.ejbcontainer.EJBComponentMetaData, com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, java.lang.String, java.lang.String)}
     *
     */
    @Test
    public void testIsCallerInRole_withExpiredCreds() {
        createCredentialServiceExpectations();
        createUnauthSubjServiceExpectations();
        createAuthzServiceExpectations();
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        rolesAllowed.add(SECURITY_ROLE);
        configProps.put(EJBSecurityConfigImpl.CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS, true);
        ejbSecColl.modified(configProps);

        mock.checking(new Expectations() {
            {
                allowing(credentialsService).isSubjectValid(invocationSubject);
                will(returnValue(false));
                allowing(credentialsService).isSubjectValid(callerSubject);
                will(returnValue(false));
                allowing(authzService).isAuthorized(APP_NAME, rolesAllowed, unauthSubject);
                will(returnValue(false));
            }
        });

        boolean inRole = ejbSecColl.isCallerInRole(componentMetaData, requestData, secCookie, SECURITY_ROLE, null);
        assertFalse("The caller should NOT be in the role.", inRole);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.ejbcontainer.security.internal.EJBSecurityCollaborator#isCallerInRole(com.ibm.ws.ejbcontainer.EJBComponentMetaData, com.ibm.ws.ejbcontainer.EJBRequestData, java.lang.Object, java.lang.String, java.lang.String)}
     *
     */
    @Test
    public void testIsCallerInRole_noAuthzService() {
        createComponentContextExpectations();
        subjectManager.setCallerSubject(callerSubject);
        rolesAllowed.add(SECURITY_ROLE);
        mock.checking(new Expectations() {
            {
                allowing(securityService).getAuthorizationService();
                will(returnValue(null));
            }
        });
        boolean inRole = ejbSecColl.isCallerInRole(componentMetaData, requestData, secCookie, SECURITY_ROLE, null);
        assertFalse("The caller should NOT be in the role.", inRole);
    }
}