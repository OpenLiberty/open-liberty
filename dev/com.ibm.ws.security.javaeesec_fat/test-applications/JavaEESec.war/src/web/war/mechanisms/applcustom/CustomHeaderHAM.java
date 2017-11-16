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
package web.war.mechanisms.applcustom;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
public class CustomHeaderHAM implements HttpAuthenticationMechanism {
    private static Logger log = Logger.getLogger(CustomHeaderHAM.class.getName());

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        log.info("validateRequest");
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        String authHeader = httpMessageContext.getRequest().getHeader("CustomHAM");

        if (httpMessageContext.isAuthenticationRequest()) {
            if (authHeader != null) {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        } else {
            if (authHeader == null) {
                if (httpMessageContext.isProtected() == false) {
                    log.info("isProtected returns false. returing NOT_DONE,");
                    status = AuthenticationStatus.NOT_DONE;
                }
            } else {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        }

        return status;
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request,
                                               HttpServletResponse response,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        log.info("secureResponse");
        return AuthenticationStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

    private AuthenticationStatus handleAuthorizationHeader(String authHeader, Subject clientSubject,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        // the format of CustomHAM is "userid:password"
        String[] values = authHeader.split(":");
        if (values.length == 2) {
            UsernamePasswordCredential cred = new UsernamePasswordCredential(values[0], values[1]);
            CredentialValidationResult result = validateUserAndPassword(cred);

            if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                setLoginHashtable(clientSubject, result);
                httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
                rspStatus = HttpServletResponse.SC_OK;
                status = AuthenticationStatus.SUCCESS;
            } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
                status = AuthenticationStatus.NOT_DONE;
            }
        } else {
            // TODO: Determine if serviceability message is needed
        }
        httpMessageContext.getResponse().setStatus(rspStatus);
        return status;
    }


    private CredentialValidationResult validateUserAndPassword(UsernamePasswordCredential credential) {
        CredentialValidationResult result = CredentialValidationResult.NOT_VALIDATED_RESULT;
        IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler();
        if (identityStoreHandler != null) {
            result = identityStoreHandler.validate(credential);
        }
        return result;
    }

    private IdentityStoreHandler getIdentityStoreHandler() {
        Instance<IdentityStoreHandler> storeHandlerInstance = CDI.current().select(IdentityStoreHandler.class);
        return storeHandlerInstance.get();
    }

    private void setLoginHashtable(Subject clientSubject, CredentialValidationResult result) {
        Hashtable<String, Object> subjectHashtable = getSubjectHashtable(clientSubject);
        String callerPrincipalName = result.getCallerPrincipal().getName();
        String callerUniqueId = result.getCallerUniqueId();
        String realm = result.getIdentityStoreId();
        realm = realm != null ? realm : "customHAM";
        String uniqueId = callerUniqueId != null ? callerUniqueId : callerPrincipalName;

        setCommonAttributes(subjectHashtable, realm, callerPrincipalName);
        setUniqueId(subjectHashtable, realm, uniqueId);
        setGroups(subjectHashtable, result.getCallerGroups());
    }

    private void setCommonAttributes(Hashtable<String, Object> subjectHashtable, String realm, String callerPrincipalName) {
        subjectHashtable.put("com.ibm.ws.authentication.internal.assertion", Boolean.TRUE);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, callerPrincipalName);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, callerPrincipalName);
    }

    private void setUniqueId(Hashtable<String, Object> subjectHashtable, String realm, String uniqueId) {
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, "user:" + realm + "/" + uniqueId);
    }

    private void setGroups(Hashtable<String, Object> subjectHashtable, Set<String> groups) {
        if (groups != null && !groups.isEmpty()) {
            log.info("Adding groups found in an identitystore : " + groups);
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>(groups));
        } else {
            log.info("No group  found in an identitystore");
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

    private Hashtable<String, Object> getSubjectExistingHashtable(final Subject clientSubject) {
        if (clientSubject == null) {
            return null;
        }

        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Hashtable<String, Object> run() {
                Set hashtables = clientSubject.getPrivateCredentials(Hashtable.class);
                if (hashtables == null || hashtables.isEmpty()) {
                    log.info("Subject has no Hashtable with custom credentials, return null.");
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

    private Hashtable<String, Object> createNewSubjectHashtable(final Subject clientSubject) {
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
}
