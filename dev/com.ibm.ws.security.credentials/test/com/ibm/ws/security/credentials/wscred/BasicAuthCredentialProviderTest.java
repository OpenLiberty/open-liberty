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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Iterator;

import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class BasicAuthCredentialProviderTest {

    private final String realm = "testRealm";
    private final String username = "user1";
    private final SerializableProtectedString password = new SerializableProtectedString("user1pwd".toCharArray());
    private BasicAuthCredentialProvider provider;
    SubjectHelper subjectHelper;
    private Subject subject;

    @Before
    public void setUp() throws Exception {
        provider = new BasicAuthCredentialProvider();
        subjectHelper = new SubjectHelper();
        subject = new Subject();
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void testSetCredential() throws Exception {
        setSubjectBasicAuthTable();
        provider.setCredential(subject);
        Iterator<WSCredential> it = subject.getPublicCredentials(WSCredential.class).iterator();

        final String[] properties = { BasicAuthCredentialProvider.KEY_BASIC_AUTH_REALM,
                                     BasicAuthCredentialProvider.KEY_BASIC_AUTH_USER,
                                     BasicAuthCredentialProvider.KEY_BASIC_AUTH_PASSWORD };

        Hashtable<String, ?> customProperties = subjectHelper.getSensitiveHashtableFromSubject(subject, properties);

        assertTrue("There must be a WSCredential credential.", it.hasNext());
        assertTrue("The WSCredential must be of type basic auth.", it.next().isBasicAuth());
        assertNull("The basic auth hashtable must be removed from the subject.", customProperties);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void testSetCredentialNoHashtableNoCredential() throws Exception {
        provider.setCredential(subject);
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);

        assertNull("There must not be a WSCredential credential.", wsCredential);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void testSetCredentialHashtableWithMissingRealmNoCredential() throws Exception {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_USER, username);
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_PASSWORD, password);
        subject.getPrivateCredentials().add(hashtable);

        provider.setCredential(subject);
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);

        assertNull("There must not be a WSCredential credential.", wsCredential);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void testSetCredentialHashtableWithMissingUsernameNoCredential() throws Exception {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_REALM, realm);
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_PASSWORD, password);
        subject.getPrivateCredentials().add(hashtable);

        provider.setCredential(subject);
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);

        assertNull("There must not be a WSCredential credential.", wsCredential);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#setCredential(javax.security.auth.Subject)}.
     */
    @Test
    public void testSetCredentialHashtableWithMissingPasswordNoCredential() throws Exception {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_REALM, realm);
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_USER, username);
        subject.getPrivateCredentials().add(hashtable);

        provider.setCredential(subject);
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);

        assertNull("There must not be a WSCredential credential.", wsCredential);
    }

    private void setSubjectBasicAuthTable() {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_REALM, realm);
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_USER, username);
        hashtable.put(BasicAuthCredentialProvider.KEY_BASIC_AUTH_PASSWORD, password);
        subject.getPrivateCredentials().add(hashtable);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider#isSubjectValid(javax.security.auth.Subject)}.
     */
    @Test
    public void testIsSubjectValid() {
        assertTrue("The BasicAuthProvider must always return true to isSubjectValid.", provider.isSubjectValid(subject));
    }
}
