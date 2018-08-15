/*
 * IBM Confidential
 * 
 * OCO Source Materials
 * 
 * Copyright IBM Corp. 2016
 * 
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.websphere.security.social;

import static org.junit.Assert.assertSame;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.context.SubjectManager;

public class UserProfileManagerTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private SubjectManager subjectManager;
    private Subject subject;
    private UserProfile subjectUserProfile;

    @Before
    public void setUp() {
        subjectManager = new SubjectManager();
        subject = new Subject();
        setupTestSubject();
    }

    @After
    public void tearDown() {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    @Test
    public void getUserProfileFromInvocationSubject() {
        UserProfile userProfile = UserProfileManager.getUserProfile();

        assertSame("The UserProfile must be the same as the UserProfile in the invocation Subject.", subjectUserProfile, userProfile);
    }

    @Test
    public void getUserProfileFromCallerSubject() {
        subjectManager.clearSubjects();
        subjectManager.setCallerSubject(subject);

        UserProfile userProfile = UserProfileManager.getUserProfile();

        assertSame("The UserProfile must be the same as the UserProfile in the caller Subject.", subjectUserProfile, userProfile);
    }

    private void setupTestSubject() {
        subjectUserProfile = mockery.mock(UserProfile.class);
        subject.getPrivateCredentials().add(subjectUserProfile);
        subjectManager.setInvocationSubject(subject);
    }

}
