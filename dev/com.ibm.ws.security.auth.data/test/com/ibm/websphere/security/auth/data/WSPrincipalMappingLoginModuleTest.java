/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.jca.WSPrincipalMappingLoginModule;
import com.ibm.ws.security.jca.AuthDataService;
import com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandler;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSPrincipalMappingLoginModuleTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private Map properties;
    private ManagedConnectionFactory managedConnectionFactory;
    private Subject subject;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;
    private AuthDataProvider authDataProvider;
    private AuthDataService authDataService;
    private AuthData authData;

    private final String alias = "myAuthData";
    private final String username = "testUser";
    private final char[] password = "testPassword".toCharArray();

    private ComponentContext cc;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("AuthData=all");

    @Rule
    public TestRule managerRule = outputMgr;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() {
        properties = new HashMap();
        properties.put(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS, alias);
        cc = mockery.mock(ComponentContext.class);
        managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
        authDataService = mockery.mock(AuthDataService.class);
        authData = mockery.mock(AuthData.class);
        authDataProvider = new AuthDataProvider();
        authDataProvider.activate(cc);

        subject = new Subject();
        sharedState = new HashMap<String, Object>();
        options = new HashMap<String, Object>();
    }

    @After
    public void tearDown() {
        authDataProvider.deactivate(cc);
        mockery.assertIsSatisfied();
    }

    @Test
    public void login() throws Exception {
        createAuthDataServiceExpectations(alias, username, password);
        createCommittedLoginModule();

        assertSubject();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void loginTrimmedAlias() throws Exception {
        properties.put(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS, " myAuthData  ");
        createAuthDataServiceExpectations(alias, username, password);
        createCommittedLoginModule();

        assertSubject();
    }

    @Test
    public void abort() throws Exception {
        createAuthDataServiceExpectations(alias, username, password);
        LoginModule module = createCommittedLoginModule();

        module.abort();

        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertTrue("There must not be a password credential in the subject after invoking abort.", passwordCredentials.isEmpty());
    }

    @Test
    public void logout() throws Exception {
        createAuthDataServiceExpectations(alias, username, password);
        LoginModule module = createCommittedLoginModule();
        module.logout();

        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertTrue("There must not be a password credential in the subject after invoking logout.", passwordCredentials.isEmpty());
    }

    @Test
    public void noAliasNoPasswordCredential() throws Exception {
        properties.remove(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS);
        createCommittedLoginModule();

        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertTrue("There must not be a password credential in the subject when the mapping alias was not specified in the map.", passwordCredentials.isEmpty());

        String missingPropertiesWarning = "CWWKS1351E: The DefaultPrincipalMapping JAAS programmatic login cannot be performed because the com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS entry was not found in the mapping properties HashMap object.";
        assertTrue("Expected warning message was not logged", outputMgr.checkForMessages(missingPropertiesWarning));
    }

    @Test
    public void noPropertiesNoPasswordCredential() throws Exception {
        properties = null;
        createCommittedLoginModule();

        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        assertTrue("There must not be a password credential in the subject when the mapping alias was not specified in the map.", passwordCredentials.isEmpty());

        String missingPropertiesWarning = "CWWKS1350E: The DefaultPrincipalMapping JAAS programmatic login cannot be performed because the WSMappingCallbackHandler was created without the mapping properties HashMap object.";
        assertTrue("Expected warning message was not logged", outputMgr.checkForMessages(missingPropertiesWarning));
    }

    private LoginModule createCommittedLoginModule() throws Exception {
        LoginModule module = createInitializedLoginModule();
        module.login();
        module.commit();
        return module;
    }

    private LoginModule createInitializedLoginModule() throws Exception {
        CallbackHandler callbackHandler = new WSMappingCallbackHandler(properties, managedConnectionFactory);
        LoginModule module = new WSPrincipalMappingLoginModule();
        module.initialize(subject, callbackHandler, sharedState, options);
        return module;
    }

    @SuppressWarnings({ "unchecked" })
    private void createAuthDataServiceExpectations(final String alias, final String user, final char[] password) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(authData).getUserName();
                will(returnValue(user));
                allowing(authData).getPassword();
                will(returnValue(password));
                allowing(authData).getKrb5Principal();
                will(returnValue(null));
            }
        });

        final ServiceReference<AuthData> authDataRef = mockery.mock(ServiceReference.class, alias);
        mockery.checking(new Expectations() {
            {
                allowing(authDataRef).getProperty("id");
                will(returnValue(alias));
                allowing(authDataRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(authDataRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });

        authDataProvider.setAuthData(authDataRef);

        mockery.checking(new Expectations() {
            {
                one(cc).locateService("authData", authDataRef);
                will(returnValue(authData));
            }
        });
    }

    private void assertSubject() throws Exception {
        PasswordCredential passwordCredential = getPasswordCredential(subject);
        assertEquals("The user name must be set in the password credential.", username, passwordCredential.getUserName());
        assertEquals("The password must be set in the password credential.", String.valueOf(password), String.valueOf(passwordCredential.getPassword()));
        assertSame("There must be a managed connection factory in the password credential.",
                   managedConnectionFactory, passwordCredential.getManagedConnectionFactory());
    }

    private PasswordCredential getPasswordCredential(Subject subject) {
        Set<PasswordCredential> passwordCredentials = subject.getPrivateCredentials(PasswordCredential.class);
        Iterator<PasswordCredential> passwordCredentialsIterator = passwordCredentials.iterator();
        return passwordCredentialsIterator.next();
    }

}
