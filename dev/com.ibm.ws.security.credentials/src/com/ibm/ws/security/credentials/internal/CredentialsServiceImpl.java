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

import java.util.Hashtable;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class CredentialsServiceImpl implements CredentialsService {

    private static final String KEY_BASIC_AUTH_REALM = "com.ibm.ws.security.cred.realm";
    private static final String KEY_BASIC_AUTH_USER = "com.ibm.ws.security.cred.user";
    private static final String KEY_BASIC_AUTH_PASSWORD = "com.ibm.ws.security.cred.password";
    static final String KEY_CREDENTIAL_PROVIDER = "credentialProvider";
    public static final String KEY_BASIC_AUTH_CREDENTIAL_PROVIDER = "basicAuthCredentialProvider";

    private final ConcurrentServiceReferenceSet<CredentialProvider> credentialProviders = new ConcurrentServiceReferenceSet<CredentialProvider>(KEY_CREDENTIAL_PROVIDER);
    private final AtomicServiceReference<CredentialProvider> basicAuthCredentialProvider = new AtomicServiceReference<CredentialProvider>(KEY_BASIC_AUTH_CREDENTIAL_PROVIDER);
    private String unauthenticatedUser = "UNAUTHENTICATED";

    protected void setCredentialProvider(ServiceReference<CredentialProvider> reference) {
        credentialProviders.addReference(reference);
    }

    protected void unsetCredentialProvider(ServiceReference<CredentialProvider> reference) {
        credentialProviders.removeReference(reference);
    }

    public void setBasicAuthCredentialProvider(ServiceReference<CredentialProvider> reference) {
        basicAuthCredentialProvider.setReference(reference);
    }

    protected void unsetBasicAuthCredentialProvider(ServiceReference<CredentialProvider> reference) {
        basicAuthCredentialProvider.unsetReference(reference);
    }

    public void activate(ComponentContext cc) {
        credentialProviders.activate(cc);
        basicAuthCredentialProvider.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        credentialProviders.deactivate(cc);
        basicAuthCredentialProvider.deactivate(cc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCredentials(Subject subject) throws CredentialException {
        // Step through the list of registered providers
        Iterator<CredentialProvider> itr = credentialProviders.getServices();
        while (itr.hasNext()) {
            CredentialProvider provider = itr.next();
            provider.setCredential(subject);
        }
        //create jwt sso token and add it to the subject
//        JwtSSOTokenHelper.createJwtSSOToken(subject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBasicAuthCredential(Subject basicAuthSubject, String realm, String username, @Sensitive String password) throws CredentialException {
        //@AV999
//        System.out.println("@AV999, stack from the thread = ");
        CredentialProvider provider = basicAuthCredentialProvider.getService();
        if (provider != null) {
            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            hashtable.put(KEY_BASIC_AUTH_REALM, realm);
            hashtable.put(KEY_BASIC_AUTH_USER, username);
            hashtable.put(KEY_BASIC_AUTH_PASSWORD, new SerializableProtectedString(password.toCharArray()));
            basicAuthSubject.getPrivateCredentials().add(hashtable);

            provider.setCredential(basicAuthSubject);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setUnauthenticatedUserid(String userid) {
        unauthenticatedUser = userid;
    }

    /** {@inheritDoc} */
    @Override
    public String getUnauthenticatedUserid() {
        return unauthenticatedUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubjectValid(Subject subject) {
        boolean isValid = true;
        // Step through the list of registered providers
        Iterator<CredentialProvider> itr = credentialProviders.getServices();
        while (itr.hasNext()) {
            CredentialProvider provider = itr.next();
            isValid = isValid && provider.isSubjectValid(subject);
            if (!isValid) {
                break;
            }
        }
        return isValid;
    }
}
