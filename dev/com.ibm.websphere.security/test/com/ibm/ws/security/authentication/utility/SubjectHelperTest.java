/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.websphere.security.cred.WSCredential;

public class SubjectHelperTest {

    private final Mockery mock = new JUnit4Mockery();
    private final WSCredential wsCredential = mock.mock(WSCredential.class);
    private final SubjectHelper subjectHelper = new SubjectHelper();

    /**
     * Test subject is null
     */
    @Test
    public void isUnauthenticated_nullSubject() throws Exception {
        assertTrue(subjectHelper.isUnauthenticated(null));
    }

    /**
     * Test subject is not null but no WSCredential
     */
    @Test
    public void isUnauthenticated_nullWSCredential() throws Exception {
        Subject subject = new Subject();
        assertTrue(subjectHelper.isUnauthenticated(subject));
    }

    @Test
    /**
     * Test subject with WSCredential
     */
    public void isUnauthenticated() throws Exception {
        Subject subject = new Subject();
        subject.getPublicCredentials().add(wsCredential);

        mock.checking(new Expectations() {
            {
                one(wsCredential).isUnauthenticated();
                will(returnValue(false));
            }
        });

        assertFalse("Subject has credential",
                    subjectHelper.isUnauthenticated(subject));
    }

    /**
     * Test subject with WSCredential can get realm from credential
     */
    @Test
    public void getRealm() throws Exception {
        final String testRealm = "realm";
        Subject subject = new Subject();
        subject.getPublicCredentials().add(wsCredential);

        mock.checking(new Expectations() {
            {
                one(wsCredential).getRealmName();
                will(returnValue(testRealm));
            }
        });

        assertEquals("The realm must be obtained from the subject's credential.",
                     testRealm, subjectHelper.getRealm(subject));
    }
}
