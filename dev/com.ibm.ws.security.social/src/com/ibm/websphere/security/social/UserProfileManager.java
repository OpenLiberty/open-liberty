/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.social;

import java.util.Iterator;

import javax.security.auth.Subject;

import com.ibm.ws.security.context.SubjectManager;

/**
 * This is a helper class for client web application to interact with social media with access_token.
 *
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.0
 *
 * @ibm-api
 */
public class UserProfileManager {

    /**
     * Get UserProfile for the subject on the thread.
     *
     * @return the user profile for the social media authenticated user.
     */
    public static UserProfile getUserProfile() {
        UserProfile userProfile = null;
        Subject subject = getSubject();
        Iterator<UserProfile> userProfilesIterator = subject.getPrivateCredentials(UserProfile.class).iterator();
        if (userProfilesIterator.hasNext()) {
            userProfile = userProfilesIterator.next();
        }
        return userProfile;
    }

    private static Subject getSubject() {
        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }
        return subject;
    }

}
