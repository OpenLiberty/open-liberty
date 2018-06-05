/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class Utils {

    private static final TraceComponent tc = Tr.register(Utils.class);
    private boolean logNoIDInfo = false;

    public Utils() {}

    @SuppressWarnings("rawtypes")
    protected AuthenticationStatus validateUserAndPassword(CDI cdi, String realmName, Subject clientSubject, @Sensitive UsernamePasswordCredential credential,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        return validateCredential(cdi, realmName, clientSubject, credential, httpMessageContext);
    }

    @SuppressWarnings("rawtypes")
    protected AuthenticationStatus validateCredential(CDI cdi, String realmName, Subject clientSubject, @Sensitive Credential credential,
                                                      HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        if (isIdentityStoreAvailable(cdi)) {
            IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler(cdi);
            if (identityStoreHandler != null) {
                status = validateWithIdentityStore(realmName, clientSubject, credential, identityStoreHandler);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdentityStoreHandler bean is not found. ");
                }
                throw new AuthenticationException("No IdentityStoreHandler found");
            }
        } else {
            if (!logNoIDInfo) {
                Tr.info(tc, "JAVAEESEC_CDI_INFO_NO_IDENTITY_STORE");
                logNoIDInfo = true;
            }
            if (isRegistryAvailable()) {
                // If an identity store is not available, fall back to the original user registry.
                status = validateWithUserRegistry(clientSubject, credential, httpMessageContext.getHandler());
            } else {
                // if there is no UR, return not done for let accesss the target if it is unprotected.
                status = AuthenticationStatus.NOT_DONE;
            }
        }
        return status;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected AuthenticationStatus handleAuthenticate(CDI cdi, String realmName, @Sensitive Credential credential, Subject clientSubject,
                                                      HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        status = validateCredential(cdi, realmName, clientSubject, credential, httpMessageContext);
        if (status == AuthenticationStatus.SUCCESS) {
            httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
        }
        return status;
    }

    public AuthenticationStatus validateWithIdentityStore(String realmName, Subject clientSubject, @Sensitive Credential credential, IdentityStoreHandler identityStoreHandler) {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        CredentialValidationResult result = identityStoreHandler.validate(credential);
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            setLoginHashtable(realmName, clientSubject, result);
            status = AuthenticationStatus.SUCCESS;
        } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
            status = AuthenticationStatus.NOT_DONE;
        }
        return status;
    }

    private AuthenticationStatus validateWithUserRegistry(Subject clientSubject, @Sensitive Credential credential,
                                                          CallbackHandler handler) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        if (handler != null) {
            if (isSupportedCredential(credential)) {
                PasswordValidationCallback pwcb = new PasswordValidationCallback(clientSubject, ((UsernamePasswordCredential) credential).getCaller(), ((UsernamePasswordCredential) credential).getPassword().getValue());
                try {
                    handler.handle(new Callback[] { pwcb });
                    boolean isValidPassword = pwcb.getResult();
                    if (isValidPassword) {
                        status = AuthenticationStatus.SUCCESS;
                    }
                } catch (Exception e) {
                    throw new AuthenticationException(e.toString());
                }
            } else {
                // This is an error condition.
                Tr.error(tc, "JAVAEESEC_CDI_ERROR_UNSUPPORTED_CRED", credential.getClass().getName());
                String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_UNSUPPORTED_CRED", credential.getClass().getName());
                throw new AuthenticationException(msg);
            }
        }
        return status;
    }

    private void setLoginHashtable(String realmName, Subject clientSubject, CredentialValidationResult result) {
        Hashtable<String, Object> subjectHashtable = getSubjectHashtable(clientSubject);
        String callerPrincipalName = result.getCallerPrincipal().getName();
        String callerUniqueId = result.getCallerUniqueId();
        String realm = result.getIdentityStoreId();
        realm = realm != null ? realm : realmName;
        if (realm == null || realm.isEmpty()) {
            realm = JavaEESecConstants.DEFAULT_REALM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The realm name is not defined, \"defaultRealm\" is used.");
            }
        }
        String uniqueId = callerUniqueId != null ? callerUniqueId : callerPrincipalName;

        setCommonAttributes(subjectHashtable, realm, callerPrincipalName);
        setUniqueId(subjectHashtable, realm, uniqueId);
        setGroups(subjectHashtable, result.getCallerGroups());
        // set cache key for the hashtable which is constructed by the identity store,
        // because the output from the id store might be different even if the same users are
        // validated multiple times. This is because some EL expression would be resolved in every invocation
        // and which will end up generating differernt ouput.
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, String.valueOf(subjectHashtable.hashCode()));
    }

    private void setCommonAttributes(Hashtable<String, Object> subjectHashtable, String realm, String callerPrincipalName) {
        subjectHashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, callerPrincipalName);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, callerPrincipalName);
    }

    private void setUniqueId(Hashtable<String, Object> subjectHashtable, String realm, String uniqueId) {
        String accessId = "user:" + realm + "/" + uniqueId;
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, accessId);
    }

    private void setGroups(Hashtable<String, Object> subjectHashtable, Set<String> groups) {
        if (groups != null && !groups.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding groups found in an identitystore", groups);
            }
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>(groups));
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No group  found in an identitystore");
            }
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>());
        }
    }

    private Hashtable<String, Object> getSubjectHashtable(final Subject clientSubject) {
        Hashtable<String, Object> subjectHashtable = getSubjectExistingHashtable(clientSubject);
        if (subjectHashtable == null) {
            subjectHashtable = createNewSubjectHashtable(clientSubject);
        }
        return subjectHashtable;
    }

    public Hashtable<String, Object> getSubjectExistingHashtable(final Subject clientSubject) {
        if (clientSubject == null) {
            return null;
        }

        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Hashtable<String, Object> run() {
                Set hashtables = clientSubject.getPrivateCredentials(Hashtable.class);
                if (hashtables == null || hashtables.isEmpty()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Subject has no Hashtable with custom credentials, return null.");
                    }
                    return null;
                } else {
                    Hashtable hashtable = (Hashtable) hashtables.iterator().next();
                    return hashtable;
                }
            }
        };
        Hashtable<String, Object> cred = AccessController.doPrivileged(action);
        return cred;
    }

    public Hashtable<String, Object> createNewSubjectHashtable(final Subject clientSubject) {
        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @Override
            public Hashtable<String, Object> run() {
                Hashtable<String, Object> newCred = new Hashtable<String, Object>();
                clientSubject.getPrivateCredentials().add(newCred);
                return newCred;
            }
        };
        return AccessController.doPrivileged(action);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public IdentityStoreHandler getIdentityStoreHandler(CDI cdi) {
        IdentityStoreHandler identityStoreHandler = null;
        Instance<IdentityStoreHandler> storeHandlerInstance = cdi.select(IdentityStoreHandler.class);
        if (storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        // If the ham is from the extension, then the identitystorehandler from the application need to be found using the app's bean manager.
        if (identityStoreHandler == null && cdi.getBeanManager().equals(CDIHelper.getBeanManager()) == false) {
            identityStoreHandler = (IdentityStoreHandler) CDIHelper.getBeanFromCurrentModule(IdentityStoreHandler.class);
        }
        return identityStoreHandler;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean isIdentityStoreAvailable(CDI cdi) {
        Instance<IdentityStore> identityStoreInstances = cdi.select(IdentityStore.class);
        if (identityStoreInstances != null && !identityStoreInstances.isUnsatisfied() && !identityStoreInstances.isAmbiguous()) {
            return true;
        }
        // If the mechanism is from the extension, then the identity stores from the application need to be found using the app's bean manager.
        if (cdi.getBeanManager().equals(CDIHelper.getBeanManager()) == false) {
            if (!CDIHelper.getBeansFromCurrentModule(IdentityStore.class).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSupportedCredential(@Sensitive Credential cred) {
        if (cred != null && (cred instanceof UsernamePasswordCredential || cred instanceof BasicAuthenticationCredential)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isRegistryAvailable() {
        try {
            return (RegistryHelper.getUserRegistry(null) != null);
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error getting the user registry", e);
            // in order to propagate the error, let the code follow through the authentication with UR.
            return true;
        }
    }
}
