/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Custom login module that adds another PublicCredential to the subject
 */
@SuppressWarnings("unchecked")
public class HashtableCustomLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(HashtableCustomLoginModule.class);
    protected Map<String, ?> _sharedState;
    protected Map<String, ?> _options;
    private final String employeeUser = "user1";
    private final String managerUser = "user2";
    private final String managerPassword = "user2pwd";
    private final String managerUniqueId = "user:BasicRealm/user2";
    private final String invalidUser = "invaliduser";
    private final String invalidPassword = "invalidpassword";
    private final String validRealm = "BasicRealm";
    private final ArrayList<String> groupList = new ArrayList<String>();
    private final String validGroup = "group2";
    private final String validGroupFormat = "group:BasicRealm/group2";
    private final String customRealm = "customRealm";
    private final String customGroup = "customGroup";
    private final String ValidUserAndPassword = "ValidUserAndPassword";
    private final String ValidUserOnly = "ValidUserOnly";
    private final String InvalidUserAndPassword = "InvalidUserAndPassword";
    private final String UniqueIdOnly = "UniqueIdOnly";
    private final String UniqueIdAndSecurityNameSame = "UniqueIdAndSecurityNameSame";
    private final String UniqueIdFormatAndSecurityNameSame = "UniqueIdFormatAndSecurityNameSame";
    private final String UniqueIdAndSecurityNameDifferent = "UniqueIdAndSecurityNameDifferent";
    private final String ValidRealm = "ValidRealm";
    private final String ValidGroup = "ValidGroup";
    private final String ValidGroupFormat = "ValidGroupFormat";
    private final String CustomRealm = "CustomRealm";
    private final String CustomGroup = "CustomGroup";
    private final String CacheKey = "CacheKey";
    private final String CustomCacheKey = "CustomCacheKey:" + managerUniqueId;
    private final String NoUserRegistry = "NoUserRegistry";
    private final String NoURNoBindings = "NoURNoBindings";

    /**
     * Initialization of login module
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        _sharedState = sharedState;
        _options = options;
        Tr.debug(tc, "FAT login module: initialize()");
    }

    @Override
    public boolean login() throws LoginException {
        Map<String, String> myOptions = (Map<String, String>) _options;
        String scenario = myOptions.get("scenario");

        Tr.debug(tc, "FAT login module: login()");
        Tr.debug(tc, "FAT login module: test scenario = " + scenario);

        try {
            Hashtable<String, Object> customProperties = (Hashtable<String, Object>) _sharedState.get(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY);
            if (customProperties == null) {
                customProperties = new java.util.Hashtable<String, Object>();
            }

            if (scenario.contains(ValidUserAndPassword)) {
                Tr.debug(tc, "FAT login module: login user = " + managerUser);
                Tr.debug(tc, "FAT login module: login password = " + managerPassword);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, managerPassword);

            } else if (scenario.contains(ValidUserOnly)) {
                Tr.debug(tc, "FAT login module: login user = " + managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, managerUser);

            } else if (scenario.contains(InvalidUserAndPassword)) {
                Tr.debug(tc, "FAT login module: login user = " + invalidUser);
                Tr.debug(tc, "FAT login module: login password = " + invalidPassword);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, invalidUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, invalidPassword);

            } else if (scenario.contains(UniqueIdOnly)) {
                // login with 1st user should succeed even though login here with uniquedId fails
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUniqueId);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUniqueId);

            } else if (scenario.contains(UniqueIdAndSecurityNameSame)) {
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);

            } else if (scenario.contains(UniqueIdFormatAndSecurityNameSame)) {
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUniqueId);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUniqueId);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);

            } else if (scenario.contains(UniqueIdAndSecurityNameDifferent)) {
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUniqueId);
                Tr.debug(tc, "FAT login module: login securityname = " + employeeUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUniqueId);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, employeeUser);

            } else if (scenario.contains(ValidRealm)) {
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: login realm = " + validRealm);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, validRealm);

            } else if (scenario.contains(CustomRealm)) {
                // Currently no OPT for custom realm until multiple realms are support
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: login realm = " + customRealm);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, customRealm);

            } else if (scenario.contains(ValidGroup)) {
                groupList.add(validGroup);
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: login group = " + validGroup);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupList);

            } else if (scenario.contains(ValidGroupFormat)) {
                groupList.add(validGroupFormat);
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: login group = " + validGroupFormat);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupList);

            } else if (scenario.contains(CustomGroup)) {
                groupList.add(customGroup);
                Tr.debug(tc, "FAT login module: login uniqueId = " + managerUser);
                Tr.debug(tc, "FAT login module: login securityname = " + managerUser);
                Tr.debug(tc, "FAT login module: login group = " + customGroup);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupList);

            } else if (scenario.contains(CacheKey)) {
                Tr.debug(tc, "FAT login module: login user = " + managerUser);
                Tr.debug(tc, "FAT login module: login password = " + managerPassword);
                Tr.debug(tc, "FAT login module: login cachekey = " + CustomCacheKey);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, managerPassword);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CustomCacheKey);
            } else if (scenario.contains(NoUserRegistry)) {
                Tr.debug(tc, "FAT login module: login user instead of uniqueid (run time will build the accessId with WSCREDENTIAL_REALM) = " + managerUser);
                String securityName = managerUser;
                Tr.debug(tc, "FAT login module: login securityname = " + securityName);
                Tr.debug(tc, "FAT login module: login custom realm = " + customRealm);

                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, managerUser);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, securityName);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, customRealm);
            } else if (scenario.contains(NoURNoBindings)) {
                Tr.debug(tc, "FAT login module: login user = " + managerUser);

                String uniqueid = "user" + ":" + "customRealm" + "/" + managerUser;

                // Retrieves the display name from the user registry based on the uniqueID.
                String securityName = managerUser;

                groupList.add("Employee");
                groupList.add("Manager");

                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueid);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, securityName);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupList);
            }

            Map<String, Hashtable<String, Object>> mySharedState = (Map<String, Hashtable<String, Object>>) _sharedState;
            mySharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, customProperties);
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
