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
package com.ibm.ws.security.credentials.internal;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider;

@SuppressWarnings("unchecked")
public class CredentialsServiceImplTest {

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<CredentialProvider> provider1Ref = mock.mock(ServiceReference.class, "provider1Ref");
    private final CredentialProvider provider1 = mock.mock(CredentialProvider.class, "provider1");
    private final ServiceReference<CredentialProvider> provider2Ref = mock.mock(ServiceReference.class, "provider2Ref");
    private final CredentialProvider provider2 = mock.mock(CredentialProvider.class, "provider2");
    private final ServiceReference<CredentialProvider> basicAuthCredentialProviderRef = mock.mock(ServiceReference.class, "basicAuthCredentialProviderRef");
    private final CredentialProvider basicAuthCredentialProvider = new BasicAuthCredentialProvider();
    private CredentialsServiceImpl credentialsService;

    @Before
    public void setUp() throws Exception {
        final List<String> uniqueGroupIds = new ArrayList<String>();
        uniqueGroupIds.add("group1");

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(CredentialsServiceImpl.KEY_CREDENTIAL_PROVIDER, provider1Ref);
                will(returnValue(provider1));
                allowing(cc).locateService(CredentialsServiceImpl.KEY_CREDENTIAL_PROVIDER, provider2Ref);
                will(returnValue(provider2));
                allowing(cc).locateService(CredentialsServiceImpl.KEY_BASIC_AUTH_CREDENTIAL_PROVIDER, basicAuthCredentialProviderRef);
                will(returnValue(basicAuthCredentialProvider));

                // Set up comparison expectations
                allowing(provider1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(provider1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(provider2Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(provider2Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });

        credentialsService = new CredentialsServiceImpl();
        credentialsService.setCredentialProvider(provider1Ref);
        credentialsService.setCredentialProvider(provider2Ref);
        credentialsService.setBasicAuthCredentialProvider(basicAuthCredentialProviderRef);
        credentialsService.activate(cc);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        credentialsService.unsetCredentialProvider(provider1Ref);
        credentialsService.unsetCredentialProvider(provider2Ref);
        credentialsService.unsetBasicAuthCredentialProvider(basicAuthCredentialProviderRef);
        credentialsService.deactivate(cc);
        credentialsService = null;
    }

    /**
     * Ensure delegation to the registered providers.
     */
    @Test
    public void setCredentialsSubject() throws Exception {
        final Subject subject = new Subject();
        mock.checking(new Expectations() {
            {
                one(provider1).setCredential(subject);
                one(provider2).setCredential(subject);
            }
        });

        credentialsService.setCredentials(subject);
    }

    @Test
    public void setBasicAuthCredential() throws Exception {
        final Subject subject = new Subject();

        credentialsService.setBasicAuthCredential(subject, "testRealm", "user1", "user1pwd");
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);
        assertTrue("The credential must be a basic auth credential.", wsCredential.isBasicAuth());
    }

}
