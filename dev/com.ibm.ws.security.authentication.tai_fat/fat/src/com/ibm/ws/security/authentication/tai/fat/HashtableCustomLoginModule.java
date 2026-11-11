/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.authentication.tai.fat;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Custom login module that adds another PublicCredential to the subject
 */
@SuppressWarnings("unchecked")
public class HashtableCustomLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(HashtableCustomLoginModule.class);
    protected Map<String, ?> _sharedState;
    protected Map<String, ?> _options;
    protected Subject _subject = null;
    private final String managerUser = "user2";
    private final String managerPassword = "user2pwd";
    private final String managerUniqueId = "user:BasicRealm/user2";
    private final String SharedState = "SharedState";
    private final String PublicCredentials = "PublicCredentials";

    /**
     * Initialization of login module
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        _sharedState = sharedState;
        _options = options;
        _subject = subject;
        Tr.debug(tc, "FAT login module: initialize()");
    }

    @Override
    public boolean login() throws LoginException {
        Map<String, String> myOptions = (Map<String, String>) _options;
        String scenario = myOptions.get("scenario");

        Tr.debug(tc, "FAT login module: login()");
        Tr.debug(tc, "FAT login module: test scenario = " + scenario);

        try {

            java.util.Hashtable<String, Object> customProperties = (java.util.Hashtable<String, Object>) _sharedState.get(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY);
            if (customProperties == null) {
                customProperties = new java.util.Hashtable<String, Object>();
            }

            String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                 AttributeNameConstants.WSCREDENTIAL_USERID,
                                                 AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                 AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                                 AuthenticationConstants.INTERNAL_ASSERTION_KEY };
            SubjectHelper subjectHelper = new SubjectHelper();
            customProperties = (Hashtable<String, Object>) subjectHelper.getHashtableFromSubject(_subject, hashtableLoginProperties);

            if (scenario.contains(SharedState)) {
                Tr.debug(tc, "FAT login module: login user = " + managerUser);
                Tr.debug(tc, "FAT login module: login password = " + managerPassword);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, managerUser);
                Map<String, java.util.Hashtable> mySharedState = (Map<String, java.util.Hashtable>) _sharedState;
                mySharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, customProperties);

            } else if (scenario.contains(PublicCredentials)) {
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUniqueId);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: customProperties before = " + customProperties);
                Set<Object> publicCredentials = _subject.getPublicCredentials();
                Tr.debug(tc, "FAT login module: publicCredentials = " + publicCredentials);
                publicCredentials.remove(customProperties);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUniqueId);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                Tr.debug(tc, "FAT login module: customProperties after = " + customProperties);
                publicCredentials.add(customProperties);
            }

        } catch (Exception e) {
            throw new LoginException("LoginException: " + e.getMessage());
        }

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() {
        return true;
    }

    @Override
    public boolean logout() {
        return true;
    }

}
