/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.wscred;

import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Provides a mechanism to create WSCredentials for a basic auth subject.
 */
@Component(service = CredentialProvider.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "type=BasicAuthCredential" })
public class BasicAuthCredentialProvider implements CredentialProvider {

    protected static final String KEY_BASIC_AUTH_REALM = "com.ibm.ws.security.cred.realm";
    protected static final String KEY_BASIC_AUTH_USER = "com.ibm.ws.security.cred.user";
    protected static final String KEY_BASIC_AUTH_PASSWORD = "com.ibm.ws.security.cred.password";

    @Activate
    protected void activate() {}

    @Deactivate
    protected void deactivate() {}

    /**
     * {@inheritDoc} Create a WSCredential for the realm, user, and password in the subject.
     * If a basic auth map is not found, take no action.
     */

    @Override
    public void setCredential(Subject subject) throws CredentialException {
        Hashtable<String, ?> customProperties = getBasicAuthHashtableFromSubject(subject);
        try {
            // Hashtable with basic auth information, all properties must be there.
            if (customProperties != null && !customProperties.isEmpty() && customProperties.size() == 3) {
                String realm = getRealm(customProperties);
                String securityName = getSecurityName(customProperties);
                SerializableProtectedString password = getPassword(customProperties);
                setBasicAuthCredential(subject, realm, securityName, password);
            }
        } finally {
            removeBasicAuthHashtable(subject, customProperties);
        }
    }

    /**
     * Gets the SSO token from the subject.
     * 
     * @param subject {@code null} is not supported.
     * @return
     */
    @Sensitive
    private Hashtable<String, ?> getBasicAuthHashtableFromSubject(@Sensitive final Subject subject) {
        final String[] properties = { KEY_BASIC_AUTH_REALM, KEY_BASIC_AUTH_USER, KEY_BASIC_AUTH_PASSWORD };
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getSensitiveHashtableFromSubject(subject, properties);
    }

    private String getRealm(@Sensitive Hashtable<String, ?> customProperties) {
        return (String) customProperties.get(KEY_BASIC_AUTH_REALM);
    }

    private String getSecurityName(@Sensitive Hashtable<String, ?> customProperties) {
        return (String) customProperties.get(KEY_BASIC_AUTH_USER);
    }

    @Sensitive
    private SerializableProtectedString getPassword(@Sensitive Hashtable<String, ?> customProperties) {
        return (SerializableProtectedString) customProperties.get(KEY_BASIC_AUTH_PASSWORD);
    }

    private void setBasicAuthCredential(@Sensitive Subject subject, String realm, String securityName, SerializableProtectedString password) throws CredentialException {
        WSCredential cred = new WSCredentialImpl(realm, securityName, new String(password.getChars()));
        subject.getPublicCredentials().add(cred);
    }

    private void removeBasicAuthHashtable(@Sensitive Subject subject, @Sensitive Hashtable<String, ?> customProperties) {
        if (customProperties != null) {
            subject.getPrivateCredentials().remove(customProperties);
        }
    }

    @Override
    public boolean isSubjectValid(Subject subject) {
        return true;
    }

}
