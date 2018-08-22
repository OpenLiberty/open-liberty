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
package com.ibm.ws.security.credentials.wscred.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.CredentialExpiredException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Provides a mechanism to create WSCredentials, consumable by the CredentialsService.
 */
public class WSCredentialProvider implements CredentialProvider {
    private static final TraceComponent tc = Tr.register(WSCredentialProvider.class);//, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String KEY_USER_REGISTYR_SERVICE = "userRegistryService";
    public static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    private final AtomicServiceReference<UserRegistryService> userRegistryServiceRef = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTYR_SERVICE);
    private final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);

    protected void setUserRegistryService(ServiceReference<UserRegistryService> reference) {
        userRegistryServiceRef.setReference(reference);
    }

    protected void unsetUserRegistryService(ServiceReference<UserRegistryService> reference) {
        userRegistryServiceRef.unsetReference(reference);
    }

    public void setCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.setReference(ref);
    }

    public void unsetCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        userRegistryServiceRef.activate(cc);
        credentialsServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        userRegistryServiceRef.deactivate(cc);
        credentialsServiceRef.deactivate(cc);
    }

    /**
     * {@inheritDoc} Create a WSCredential for the WSPrincipal in the subject.
     * If WSPrincipal is found, take no action.
     */
    @Override
    public void setCredential(Subject subject) throws CredentialException {
        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        if (principals.isEmpty()) {
            return;
        }
        if (principals.size() != 1) {
            throw new CredentialException("Too many WSPrincipals in the subject");
        }
        WSPrincipal principal = principals.iterator().next();
        setCredential(subject, principal);
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    private Hashtable<String, ?> getUniqueIdAndSecurityNameHashtableFromSubject(final Subject subject) {
        final String[] properties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                      AttributeNameConstants.WSCREDENTIAL_SECURITYNAME };
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getHashtableFromSubject(subject, properties);
    }

    private Hashtable<String, ?> getUniqueIdHashtableFromSubject(final Subject subject) {
        final String[] properties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID };
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getHashtableFromSubject(subject, properties);
    }

    /**
     * Create a WSCredential for the specified accessId.
     * If this accessId came from the current UserRegistry, create a WsCredential.
     * If not, then do nothing.
     *
     * @throws CredentialException
     */
    private void setCredential(Subject subject, WSPrincipal principal) throws CredentialException {
        String securityName = principal.getName();
        Hashtable<String, ?> customProperties = getUniqueIdHashtableFromSubject(subject);
        if (customProperties == null || customProperties.isEmpty()) {
            UserRegistryService urService = userRegistryServiceRef.getService();
            if (urService != null) {
                String urType = urService.getUserRegistryType();
                if ("WIM".equalsIgnoreCase(urType) || "LDAP".equalsIgnoreCase(urType)) {
                    try {
                        securityName = urService.getUserRegistry().getUserDisplayName(securityName);
                    } catch (Exception e) {
                        //do nothing
                    }
                }
            }
        }
        if (securityName == null || securityName.length() == 0) {
            securityName = principal.getName();
        }
        String accessId = principal.getAccessId();

        String customRealm = null;
        String realm = null;
        String uniqueName = null;

        if (customProperties != null) {
            customRealm = (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM);
        }

        if (customRealm != null) {
            realm = customRealm;
            String[] parts = accessId.split(realm + "/");
            if (parts != null && parts.length == 2)
                uniqueName = parts[1];
        } else {
            realm = AccessIdUtil.getRealm(accessId);
            uniqueName = AccessIdUtil.getUniqueId(accessId);
        }
        if (AccessIdUtil.isServerAccessId(accessId)) {
            // Create a server WSCredential
            setCredential(null, subject, realm, securityName, uniqueName, null, accessId, null, null);
        } else {
            CredentialsService cs = credentialsServiceRef.getService();
            String unauthenticatedUserid = cs.getUnauthenticatedUserid();

            if (securityName != null && unauthenticatedUserid != null &&
                securityName.equals(unauthenticatedUserid)) {
                // Create an unauthenticated WSCredential
                setCredential(unauthenticatedUserid, subject, realm, securityName, uniqueName, null, null, null, null);
            } else if (AccessIdUtil.isUserAccessId(accessId)) {
                // Create a user WSCredential
                createUserWSCredential(subject, securityName, accessId, realm, uniqueName, unauthenticatedUserid);
            }
        }
    }

    /**
     * @param subject
     * @param securityName
     * @param accessId
     * @param realm
     * @param uniqueName
     * @param unauthenticatedUserid
     * @throws CredentialException
     */
    private void createUserWSCredential(Subject subject, String securityName, String accessId, String realm, String uniqueName,
                                        String unauthenticatedUserid) throws CredentialException {
        UserRegistryService userRegistryService = userRegistryServiceRef.getService();
        try {
            Hashtable<String, ?> customProperties = getUniqueIdAndSecurityNameHashtableFromSubject(subject);
            //hashtable login mapping credential
            if (customProperties != null && !customProperties.isEmpty()) {
                List<String> uniqueGroupAccessIds = getUniqueGroupAccessIds(customProperties, realm);
                setCredential(unauthenticatedUserid, subject, realm, securityName, uniqueName, null, accessId, null, uniqueGroupAccessIds);
            } else if (userRegistryService.isUserRegistryConfigured()) {
                // We do not always require a UserRegistry, especially in cases of trusted realms
                UserRegistry userRegistry = userRegistryService.getUserRegistry();
                if (userRegistry.getRealm().equals(realm)) {
                    List<String> uniqueGroupAccessIds = getUniqueGroupAccessIds(userRegistry, realm, uniqueName);
                    String primaryUniqueGroupAccessId = getPrimaryGroupId(uniqueGroupAccessIds);
                    setCredential(unauthenticatedUserid, subject, realm, securityName, uniqueName, primaryUniqueGroupAccessId, accessId, null, uniqueGroupAccessIds);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Requested creation of a WSCredential for an unknown realm", userRegistry.getRealm(), realm);
                    }
                    throw new CredentialException("Foreign realms are unsupported. The accessId's realm, " + accessId + ", doesn't match the current realm, "
                                                  + userRegistry.getRealm() + ".");
                }
            } else {
                /*
                 * Since there is no UserRegistry we may need to throw a CredentialException here. Right now there are no reported errors, so taking no action.
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected state in WSCredentialProvider, no UserRegistry");
                }
            }
        } catch (EntryNotFoundException e) {
            throw new CredentialException("Unable to find the user for this accessId: " + accessId + ". " + e.getMessage());
        } catch (RegistryException e) {
            throw new CredentialException("Unable to access the UserRegistry: " + e.getMessage());
        }
    }

    private void setCredential(String unauthenticatedId,
                               Subject subject,
                               String realm,
                               String securityName,
                               String uniqueName,
                               String primaryUniqueGroupAccessId,
                               String accessId,
                               List<String> roles,
                               List<String> uniqueGroupAccessIds) throws CredentialException {

        WSCredential cred = new WSCredentialImpl(realm, securityName, uniqueName, unauthenticatedId, primaryUniqueGroupAccessId, accessId, null, uniqueGroupAccessIds);
        subject.getPublicCredentials().add(cred);
    }

    /**
     * Get a list of all of the groups the user belongs to, formated as
     * group access IDs.
     *
     * @param userRegistry
     * @param realm
     * @param uniqueName
     * @return A list of all of the uniqueGroupAccessIds the user belongs to
     * @throws EntryNotFoundException
     * @throws RegistryException
     */
    private List<String> getUniqueGroupAccessIds(UserRegistry userRegistry,
                                                 String realm, String uniqueName) throws EntryNotFoundException, RegistryException {
        List<String> uniqueGroupAccessIds = new ArrayList<String>();
        List<String> uniqueGroupIds = userRegistry.getUniqueGroupIdsForUser(uniqueName);
        Iterator<String> groupIter = uniqueGroupIds.iterator();
        while (groupIter.hasNext()) {
            String groupAccessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, realm, groupIter.next());
            uniqueGroupAccessIds.add(groupAccessId);
        }
        return uniqueGroupAccessIds;
    }

    /**
     * Get the primary group ID. This is assumed to be the first group in the
     * group list.
     *
     * @param uniqueGroupIds
     * @return The first entry of the list
     */
    private String getPrimaryGroupId(List<String> uniqueGroupIds) {
        return uniqueGroupIds.isEmpty() ? null : uniqueGroupIds.get(0);
    }

    /**
     * @param customProperties
     * @param realm
     * @return
     */
    private List<String> getUniqueGroupAccessIds(Hashtable<String, ?> customProperties, String realm) {
        @SuppressWarnings("unchecked")
        ArrayList<String> oldGroupList = (ArrayList<String>) customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        ArrayList<String> uniqueGroupAccessIds = new ArrayList<String>();
        if (oldGroupList != null) {
            uniqueGroupAccessIds.addAll(oldGroupList);
            for (int j = 0; j < uniqueGroupAccessIds.size(); j++) {
                String old_group = uniqueGroupAccessIds.get(j);
                String new_group = null;
                // If the group entry does not start with group:, then add it
                if (!AccessIdUtil.isGroupAccessId(old_group)) {
                    new_group = AccessIdUtil.TYPE_GROUP +
                                AccessIdUtil.TYPE_SEPARATOR +
                                realm +
                                AccessIdUtil.REALM_SEPARATOR +
                                old_group;

                    uniqueGroupAccessIds.set(j, new_group);
                }
            }
        }

        return uniqueGroupAccessIds;
    }

    /**
     * Gets the WSCredential from the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    private WSCredential getWSCredential(Subject subject) {
        WSCredential wsCredential = null;
        Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
        Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
        if (wsCredentialsIterator.hasNext()) {
            wsCredential = wsCredentialsIterator.next();
        }
        return wsCredential;
    }

    /**
     * Checks if the subject is valid. Currently, a subject is REQUIRED to have
     * a WSCredential, and it is only valid if the WSCredential is not expired.
     *
     * @param subject The subject to validate, {@code null} is not supported.
     * @return <code>true</code> if the subject is valid.
     */
    @Override
    @FFDCIgnore({ CredentialDestroyedException.class, CredentialExpiredException.class })
    public boolean isSubjectValid(Subject subject) {
        boolean valid = false;
        try {
            WSCredential wsCredential = getWSCredential(subject);
            if (wsCredential != null) {
                long credentialExpirationInMillis = wsCredential.getExpiration();
                Date currentTime = new Date();
                Date expirationTime = new Date(credentialExpirationInMillis);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Current time = " + currentTime + ", expiration time = " + expirationTime);
                }
                if (credentialExpirationInMillis == 0 || credentialExpirationInMillis == -1 ||
                    currentTime.before(expirationTime)) {
                    valid = true;
                }
            }
        } catch (CredentialDestroyedException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CredentialDestroyedException while determining the validity of the subject.", e);
            }
        } catch (CredentialExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CredentialExpiredException while determining the validity of the subject.", e);
            }
        }
        return valid;
    }
}
