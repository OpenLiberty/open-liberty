/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.security.internal;

import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

// 673415

public class J2CSecurityHelper {

    final static TraceComponent tc = Tr.register(J2CSecurityHelper.class, "WAS.j2c.security", "com.ibm.ws.jca.security.resources.J2CAMessages");

    private static ThreadLocal<Subject> subjectStorage = new ThreadLocal<Subject>();

    private static final String CACHE_KEY_PREFIX = "j2c:inboundSecurity:";

    private static final String CACHE_KEY_SEPARATOR = ":";

    public static Subject getRunAsSubject() {
        return subjectStorage.get();
    }

    public static void setRunAsSubject(Subject subject) {
        subjectStorage.set(subject);
    }

    public static void removeRunAsSubject() {
        subjectStorage.remove();
    }

    /**
     * This method updates the hashtable provided with the information that is required for custom hashtable
     * login. The hashtable should contain the following parameters for this to succeed.
     * Key: com.ibm.wsspi.security.cred.uniqueId, Value: user: ldap.austin.ibm.com:389/cn=pbirk,o=ibm,c=us
     * Key: com.ibm.wsspi.security.cred.realm, Value: ldap.austin.ibm.com:389
     * Key: com.ibm.wsspi.security.cred.securityName, Value: pbirk
     * Key: com.ibm.wsspi.security.cred.longSecurityName, Value: cn=pbirk,o=ibm,c=us (Optional)
     * Key: com.ibm.wsspi.security.cred.groups, Value: group:cn=group1,o=ibm,c=us| group:cn=group2,o=ibm,c=us|group:cn=group3,o=ibm,c=us
     * Key: com.ibm.wsspi.security.cred.cacheKey, Value: Location:9.43.21.23 (note: accessID gets appended to this value for cache lookup).
     * 
     * @param credData The hashtable that we are going to populate with the required properties
     * @param realmName The name of the realm that this user belongs to
     * @param uniqueId The uniqueId of the user
     * @param securityName The securityName of the user.
     * @param groupList The list of groups that this user belongs to.
     */
    private static void updateCustomHashtable(Hashtable<String, Object> credData, String realmName, String uniqueId, String securityName, List<?> groupList) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateCustomHashtable", new Object[] { credData, realmName, uniqueId, securityName, groupList });
        }
        String cacheKey = getCacheKey(uniqueId, realmName);
        credData.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cacheKey);
        credData.put(AttributeNameConstants.WSCREDENTIAL_REALM, realmName);
        credData.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, securityName);
        if (uniqueId != null) {
            credData.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueId);
        }
        // If uniqueId is not set then the login will fail later and the work will not execute
        if (groupList != null && !groupList.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding groups found in registry", groupList);
            }
            credData.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupList);
        } else {
            credData.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateCustomHashtable");
        }
    }

    /**
     * Class used to add the custom Hashtable to the private credentials of the
     * Subject
     * 
     * @author jroast
     * 
     */
    static class AddPrivateCredentials implements PrivilegedAction<Object> {
        private final Subject execSubject;
        private final Hashtable<String, Object> newCred;

        public AddPrivateCredentials(Subject subject, Hashtable<String, Object> credentials) {
            execSubject = subject;
            newCred = credentials;
        }

        @Override
        public Object run() {
            execSubject.getPrivateCredentials().add(newCred);
            return null;
        }
    }

    /**
     * Class used to get the user registry for a realm. This call needs
     * to be wrapped as required permissions won't be available when
     * this call is made from an application thread when the application
     * has its own security domain.
     * 
     * @author jroast
     * 
     */
    static class GetRegistry implements PrivilegedExceptionAction<UserRegistry> {
        private final String appRealm;

        public GetRegistry(String realm) {
            appRealm = realm;
        }

        @Override
        public UserRegistry run() throws Exception {
            UserRegistry registry = RegistryHelper.getUserRegistry(appRealm);
            return registry;
        }
    }

    /**
     * This method adds the custom Hashtable provided to the private credentials of the
     * Subject passed in.
     * 
     * @param callSubject The Subject which should have its private credentials updated with the custom Hashtable
     * @param newCred The custom Hashtable with information in the format required for WebSphere
     *            to do a Hashtable login.
     */
    public static void addSubjectCustomData(Subject callSubject, Hashtable<String, Object> newCred) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addSubjectCustomData", newCred);
        }
        AddPrivateCredentials action = new AddPrivateCredentials(callSubject, newCred);
        AccessController.doPrivileged(action);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addSubjectCustomData");
        }
    }

    /**
     * This class is used to get the Custom Hashtable that contains the
     * provided cache key
     * 
     * @author jroast
     * 
     */
    static class GetCustomCredentials implements PrivilegedAction<Object> {
        private final Subject execSubject;
        private final String cacheKey;

        public GetCustomCredentials(Subject subject, String key) {
            execSubject = subject;
            cacheKey = key;
        }

        @Override
        public Object run() {
            Set<Hashtable> s = execSubject.getPrivateCredentials(Hashtable.class);
            if (s == null || s.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Subject has no Hashtable with custom credentials, return null.");
                }
                return null;
            } else {
                for (Iterator<Hashtable> iterator = s.iterator(); iterator.hasNext();) {
                    Hashtable t = iterator.next();
                    String key = (String) t.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Hashtable custom key", key);
                    }
                    if (cacheKey.equals(key)) {
                        return t;
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Subject has no Hashtable that matches cacheKey, return null.");
                }
                return null;
            }
        }

    }

    /**
     * This method extracts the custom hashtable from the provided Subject using the
     * cacheKey.
     * 
     * @param callSubject The Subject which is checked for the custom hashtable
     * @param cacheKey The cache key for the entries in the custom hashtable
     * @return The custom hashtable containing the security information.
     */
    public static Hashtable<String, Object> getCustomCredentials(Subject callSubject, String cacheKey) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getCustomCredentials", cacheKey);
        }
        if (callSubject == null || cacheKey == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getCustomCredentials", " null");
            }
            return null;
        }
        GetCustomCredentials action = new GetCustomCredentials(callSubject, cacheKey);
        Hashtable<String, Object> cred = (Hashtable<String, Object>) AccessController.doPrivileged(action);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getCustomCredentials", objectId(cred));
        }
        return cred;
    }

    /**
     * The PasswordValidationCallback is used for password validation. This callback is used by the Resource Adapter
     * to employ the password validation facilities of its containing runtime. This Callback is passed to the CallbackHandler
     * provided by the J2C runtime during invocation of the handle method by the Resource Adapter.
     * 
     * Verify user/password in application user registry. If password is valid, set Callback result = true and
     * add a CustomCredential to the clientSubject with custom data that is used later by Security to do a
     * login.
     * 
     * @param callback The PasswordValidationCallback Instance
     * @param executionSubject The JAAS Subject that is passed in by the J2C Container
     * @param addedCred A hashtable that contains the custom properties that are required by Security to a hashtable login
     * @param appRealm The application Realm.
     * @param invocations An array that indicates which callbacks have been invoked.
     * 
     * @throws RemoteException This exception can be thrown by any operation on the UserRegistry as it extends java.rmi.Remote
     * @throws WSSecurityException Thrown when user registry operations fail.
     */
    public static void handlePasswordValidationCallback(PasswordValidationCallback callback, Subject executionSubject, Hashtable<String, Object> addedCred, String appRealm,
                                                        Invocation[] invocations)
                    throws RemoteException, WSSecurityException {
        invocations[2] = Invocation.PASSWORDVALIDATIONCALLBACK;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handlePasswordValidationCallback", new Object[] { objectId(callback), callback.getUsername() });
        }
        Subject callSubject = callback.getSubject();
        if (!executionSubject.equals(callSubject)) {
            Tr.warning(tc, "EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673", new Object[] { "PasswordValidationCallback" });
            callSubject = executionSubject;
        }
        try { // 673415
            String userName = callback.getUsername();
            String password = null;
            if (callback.getPassword() != null) {
                password = new String(callback.getPassword());
            }
            if (callSubject != null) {
                GetRegistry action = new GetRegistry(appRealm);
                UserRegistry registry = AccessController.doPrivileged(action);
                if (checkUserPassword(userName, password, registry, appRealm, addedCred, invocations[0])) {
                    callback.setResult(true);
                } else {
                    callback.setResult(false);
                    addedCred.clear(); // 673415
                }
            }
        } catch (PrivilegedActionException pae) { // End 673415
            callback.setResult(false);
            addedCred.clear();
            Exception ex = pae.getException();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handlePasswordValidationCallback", callback.getResult());
            }
            if (ex instanceof WSSecurityException) {
                throw (WSSecurityException) ex;
            } else { // This means an unexpected runtime exception is wrapped in the PrivilegedActionException
                throw new WSSecurityException(ex);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handlePasswordValidationCallback", callback.getResult());
        }
    }

    /**
     * This method is used to handle the CallerPrincipalCallback that is passed in by the resource adapter. The
     * CallerPrincipalCallback is the Callback for setting the container's caller (or Remote user) principal.
     * 
     * During CallerPrincipalCallback processing the Principal is extracted from the Callback and the principal
     * name is checked against the user registry for the Application Realm. If it is valid then the required
     * information about that user is fetched from the registry and used to populate the custom Hashtable that
     * is passed in which will finally be added to the private credentials of the Subject to enable custom
     * Hashtable login in WebSphere.
     * 
     * @param callback The CallerPrincipalCallback that the resource adapter passes in to the application server.
     * @param executionSubject The Subject from the CallbackHandler under which the work will be executed
     * @param addedCred The custom Hashtable that will be used to create the credentials during login.
     * @param appRealm The name of the application realm.
     * @param unauthenticated The constant representing an unauthenticated user.
     * @param invocations An array denoting the callbacks that have been invoked
     * 
     * @throws RemoteException This exception can be thrown by any operation on the UserRegistry as it extends java.rmi.Remote
     * @throws WSSecurityException Thrown when user registry operations fail.
     */
    public static void handleCallerPrincipalCallback(CallerPrincipalCallback callback, Subject executionSubject, Hashtable<String, Object> addedCred, String appRealm,
                                                     String unauthenticated, Invocation[] invocations) throws WSSecurityException, RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleCallerPrincipalCallback");
        }
        if (invocations[0] == Invocation.CALLERPRINCIPALCALLBACK) {
            String message = getNLS().getString("MULTIPLE_CALLERPRINCIPALCALLBACKS_NOT_SUPPORTED_J2CA0676",
                                                "J2CA0676E: The inflown security context supplied multiple instances of a JASPIC CallerPrincipalCallback " +
                                                                "in order to establish the security context of the Work instance.");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handleCallerPrincipalCallback");
            }
            throw new WSSecurityException(message);
        } else {
            invocations[0] = Invocation.CALLERPRINCIPALCALLBACK;
        }
        String userName = callback.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The userName got from the callback is : ", userName);
        }
        Principal userPrincipal = callback.getPrincipal();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "handleCallerPrincipalCallback", new Object[] { "user=" + userName, "principal=" + userPrincipal });
        Subject callSubject = callback.getSubject();
        if (!executionSubject.equals(callSubject)) {
            Tr.warning(tc, "EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673", new Object[] { "CallerPrincipalCallback" });
            // callSubject = executionSubject;  // Change from twas - jms - TODO - Not used after this point, may need to look at this more.
        }

        String securityName = null;
        if (userName == null && userPrincipal == null) {
            securityName = unauthenticated;
        } else if (userPrincipal != null) {
            securityName = userPrincipal.getName();
        } else {
            securityName = userName;
        }
        if (securityName == null) {
            securityName = unauthenticated;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The securityName is : ", securityName);
        }
        if (!securityName.equals(unauthenticated)) {
            addUniqueIdAndGroupsForUser(securityName, addedCred, appRealm); // 675924
        } else {
            addedCred.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, securityName);
            addedCred.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, getCacheKey(null, null));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Added Credentials: ", addedCred);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleCallerPrincipalCallback");
        }
    }

    /**
     * Method to handle the GroupPrincipalCallback thereby establishing group principals within the argument subject.
     * This callback is passed in by the Resource Adapter and handled by the J2CSecurityCallbackHandler that the
     * Application Server implements.
     * 
     * @param callback The GroupPrincipalCallback instance that is passed in by the Resource Adapter.
     * @param executionSubject The Subject from the CallbackHandler under which the work will be executed.
     * @param addedCred The custom hashtable that will be used to create the credentials during login.
     * @param appRealm The name of the application realm.
     * @param invocations An array denoting the callbacks that have been invoked
     * 
     * @throws RemoteException This exception can be thrown by any operation on the UserRegistry as it extends java.rmi.Remote
     * @throws WSSecurityException Thrown when user registry operations fail.
     */
    public static void handleGroupPrincipalCallback(GroupPrincipalCallback callback, Subject executionSubject, Hashtable<String, Object> addedCred, String appRealm,
                                                    Invocation[] invocations)
                    throws RemoteException, WSSecurityException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleGroupPrincipalCallback", objectId(callback));
        }
        invocations[1] = Invocation.GROUPPRINCIPALCALLBACK;

        Subject callSubject = callback.getSubject();
        if (!executionSubject.equals(callSubject)) {
            Tr.warning(tc, "EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673", new Object[] { "GroupPrincipalCallback" });
            // callSubject = executionSubject; // Change from twas - jms - TODO - Not used, may need to look at this some more.
        }
        String[] groups = callback.getGroups();
        if (groups != null && groups.length > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Group names in Callback: ", Arrays.asList(groups));
            }
            List<String> groupNames = (List<String>) addedCred.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
            if (groupNames == null) {
                groupNames = new ArrayList<String>();
                addedCred.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupNames);
            }
            GetRegistry action = new GetRegistry(appRealm);
            UserRegistry registry = null;
            try {
                registry = AccessController.doPrivileged(action);
            } catch (PrivilegedActionException pae) {
                Exception ex = pae.getException();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "handleGroupPrincipalCallback");
                }
                if (ex instanceof WSSecurityException) {
                    throw (WSSecurityException) ex;
                } else { // This means an unexpected runtime exception is wrapped in the PrivilegedActionException
                    throw new WSSecurityException(ex);
                }
            }
            for (int i = 0; i < groups.length; i++) {
                String group = groups[i];
                if (group == null || group.isEmpty()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Group is null or an empty string, it has been ignored.");
                    }
                    continue;
                }
                if (registry.isValidGroup(group)) {
                    String groupName = registry.getUniqueGroupId(group);
                    if (!groupNames.contains(groupName)) {
                        groupNames.add(groupName);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Added groupId: " + groupName);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, groupName + " already exists in custom credential data, avoid duplicates.");
                        }
                    }
                } else {
                    Tr.warning(tc, "INVALID_GROUP_ENCOUNTERED_J2CA0678", group);
                }
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Callback has no groups.");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Added Credentials", addedCred);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleGroupPrincipalCallback");
        }
    }

    /**
     * Checks if the provided securityName is valid against the user registry. In case it is
     * valid it then gets uniqueId and the groups for the user with the given securityName.
     * It then uses this information to create the custom hashtable required for login.
     * 
     * @param securityName The user security name
     * @param credData The hashtable that needs to be populated with the security information
     * @param appRealm The application realm name.
     * 
     * @throws RemoteException This exception can be thrown by any operation on the UserRegistry as
     *             it extends java.rmi.Remote
     * @throws WSSecurityException Thrown when user registry operations fail.
     */
    private static void addUniqueIdAndGroupsForUser(String securityName,
                                                    Hashtable<String, Object> credData, String appRealm) // 675924
    throws WSSecurityException, RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addUniqueIdAndGroupsForUser", securityName);
        }

        GetRegistry action = new GetRegistry(appRealm);
        UserRegistry registry = null;
        try {
            registry = AccessController.doPrivileged(action);
        } catch (PrivilegedActionException pae) {
            Exception ex = pae.getException();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "addUniqueIdAndGroupsForUser");
            }
            if (ex instanceof WSSecurityException) {
                throw (WSSecurityException) ex;
            } else { // This means an unexpected runtime exception is wrapped in the PrivilegedActionException
                throw new WSSecurityException(ex);
            }
        }
        if (registry.isValidUser(securityName)) {
            String uniqueId = registry.getUniqueUserId(securityName);
            String uidGrp = stripRealm(uniqueId, appRealm); // 669627 // 673415

            List<?> groups = null;
            try {
                groups = registry.getUniqueGroupIds(uidGrp); // 673415

            } catch (EntryNotFoundException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception is ", ex);
                Tr.warning(tc, "NO_GROUPS_FOR_UNIQUEID_J2CA0679", uidGrp); // 673415
            }
            updateCustomHashtable(credData, appRealm, uniqueId, securityName, groups);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Added uniqueId: " + uniqueId + " and uniqueGroups: "
                             + groups);
        } else {
            String message = getNLS()
                            .getFormattedMessage("INVALID_USER_NAME_IN_PRINCIPAL_J2CA0670",
                                                 new Object[] { securityName },
                                                 "J2CA0670E: The WorkManager was unable to establish the security context for the Work instance, " +
                                                                 "because the resource adapter provided a caller identity " + securityName
                                                                 + ", which does not belong to the security " +
                                                                 "domain associated with the application.");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "addUniqueIdAndGroupsForUser");
            }
            throw new WSSecurityException(message);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addUniqueIdAndGroupsForUser");
        }
    }

    /**
     * Checks the user name and password against the user registry provided. If the user name and
     * password are valid it returns true.
     * 
     * @param userSecurityName The user security name.
     * @param password the password
     * @param registry The registry against which the user name and password should be validated
     * @param realmName The realm to which the user belongs
     * @param addedCred The custom hashtable that will be used for login
     * @param isCCInvoked Enum representing whether the CallerPrincipalCallback was invoked
     * @return boolean representing the success or failure of the password check.
     * @throws RemoteException This exception can be thrown by any operation on the UserRegistry as
     *             it extends java.rmi.Remote
     * @throws WSSecurityException Thrown when user registry operations fail.
     */
    private static boolean checkUserPassword(String userSecurityName, String password, UserRegistry registry, String realmName, Hashtable<String, Object> addedCred,
                                             Invocation isCCInvoked) { // 675924

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "checkUserPassword user: " + userSecurityName + ", realm: " + realmName);
        }
        if (userSecurityName == null || password == null) {
            Tr.error(tc, "INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674", userSecurityName); // 673415
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "checkUserPassword", new Object[] { userSecurityName, password });
            }
            return false; // 675924
        }
        boolean match = validateCallbackInformation(addedCred, userSecurityName, isCCInvoked); // 675924
        if (!match) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "checkUserPassword", " - invalid username and password.");
            }
            return false; // 675924
        }
        try {
            registry.checkPassword(userSecurityName, password);
            String uniqueId = registry.getUniqueUserId(userSecurityName);
            uniqueId = stripRealm(uniqueId, realmName); // 669627
            List<String> uniqueGroups = new ArrayList<String>();
            List<?> groupNames = registry.getGroupsForUser(userSecurityName);
            if (groupNames != null)
                for (Object group : groupNames) {
                    uniqueGroups.add(registry.getUniqueGroupId((String) group));
                }
            updateCustomHashtable(addedCred, realmName, uniqueId, userSecurityName, uniqueGroups);
        } catch (PasswordCheckFailedException e) {
            // This error is thrown when there is no entry corresponding to the username and password
            // in the registry.
            Tr.error(tc, "INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674", userSecurityName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "checkUserPassword", " - invalid username and password"); // 675924 
            }
            return false; // 675924 
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ejs.j2c.work.security.J2CSecurityHelper.checkUserPassword", "%C");
            Tr.error(tc, "VALIDATION_FAILED_INBOUND_J2CA0684", userSecurityName); // 675924
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "checkUserPassword", " - unable to validate password.");
            }
            return false; // 675924
        } // End 673415
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "checkUserPassword", " - password is valid.");
        }
        return true;
    }

    /**
     * This method constructs the cache key that is required by security for caching
     * the Subject.
     * 
     * @param uniqueId The unique Id of the user
     * @param appRealm The application realm that the user belongs to
     * @return the cache key
     */
    public static String getCacheKey(String uniqueId, String appRealm) {
        StringBuilder cacheKey = new StringBuilder();
        if (uniqueId == null || appRealm == null) {
            cacheKey.append(CACHE_KEY_PREFIX);
        } else {
            cacheKey.append(CACHE_KEY_PREFIX).append(uniqueId).append(CACHE_KEY_SEPARATOR).append(appRealm);
        }
        return cacheKey.toString();
    }

    // Begin 675924  
    /**
     * This method validates whether the user security name provided by the CallerPrincipalCallback and
     * the PasswordValidationCallback match. It does this check only in case the CallerPrincipalCallback
     * was invoked prior to the current invocation of the PasswordValidationCallback.
     * 
     * @param credData The custom hashtable that will be used for login
     * @param securityName The user security name provided by the callback
     * @param isInvoked An Enum that represents whether either the CallerPrincipalCallback was invoked prior to this invocation.
     * @return boolean indicating whether the validation succeeded or failed
     */
    private static boolean validateCallbackInformation(Hashtable<String, Object> credData, String securityName, Invocation isInvoked) {
        boolean status = true;
        if (isInvoked == Invocation.CALLERPRINCIPALCALLBACK) {
            String existingName = (String) credData.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
            if (existingName != null && !(existingName.equals(securityName))) {
                status = false;
                Tr.error(tc, "CALLBACK_SECURITY_NAME_MISMATCH_J2CA0675", new Object[] { securityName, existingName });
            }
        }
        return status;
    }

    // End 675924

    // begin 669627
    /**
     * Some registries return user:realm/uniqueId on the invocation of the getUniqueUserId
     * method but do not accept it as the uniqueUserId for other methods like getUniqueGroupIds.
     * This method is used to strip of the user:realm/ portion and get the uniqueId
     * 
     * @param uniqueUserId The uniqueId of the user
     * @param realm The realm that this uniqueId belongs to
     * @return the uniqueUserId with the realm stripped off.
     * 
     */
    private static String stripRealm(String uniqueUserId, String realm) {
        if (uniqueUserId == null || realm == null)
            return uniqueUserId;
        int index = uniqueUserId.indexOf(realm + REALM_SEPARATOR);
        if (index > -1) {
            uniqueUserId = uniqueUserId.substring(index + realm.length() + 1);
        }
        return uniqueUserId;
    }

    // end 669627

    /**
     * A string that identifies an object instance within WAS messages and heap dumps.
     * 
     * @param o The Object instance.
     * @return the class name of bean concatenated with its hashcode in hexadecimal.
     */
    public static String objectId(Object o)
    {
        return (o == null) ? "0x0" : o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }

    private static TraceNLS nls = TraceNLS.getTraceNLS(J2CSecurityHelper.class, "com.ibm.ws.jca.security.resources.J2CAMessages");

    private static String REALM_SEPARATOR = "/"; // 669627

    public static TraceNLS getNLS() {
        return nls;
    }

}
