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
package web.war.mechanisms.appllogintocontinue.redirect;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped

@AutoApplySession
@LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=false, useForwardToLoginExpression="")
public class LoginToContinueMechanism implements HttpAuthenticationMechanism {
    private static Logger log = Logger.getLogger(LoginToContinueMechanism.class.getName());

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        @SuppressWarnings("unchecked")
        HttpServletRequest req = httpMessageContext.getRequest();
        HttpServletResponse rsp = httpMessageContext.getResponse();
        String username = null;
        String password = null;
        // in order to preserve the post parameter, unless the target url is j_security_check, do not read
        // j_username and j_password.
        String method = req.getMethod();
        String uri = req.getRequestURI();
        if ("POST".equalsIgnoreCase(method)) {
            if (uri.contains("/j_security_check")) {
                username = req.getParameter("j_username");
                password = req.getParameter("j_password");
            }
        }
        log.info("method : " + method + ", URI : " + uri + ", j_username : " + username);

        if (httpMessageContext.isAuthenticationRequest()) {
            if (username != null && password != null) {
                status = handleFormLogin(username, password, rsp, clientSubject, httpMessageContext);
            } else {
                status = AuthenticationStatus.SEND_CONTINUE;
            }
        } else {
            if (username == null || password == null) {
                if (httpMessageContext.isProtected() == false) {
                    log.info("both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    status = AuthenticationStatus.SEND_CONTINUE;
                }
            } else {
                status = handleFormLogin(username, password, rsp, clientSubject, httpMessageContext);
            }
        }

        log.info("validateRequest: status : " + status);
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
        log.info("cleanSubject");
    }

    private AuthenticationStatus handleFormLogin(String username, String password, HttpServletResponse rsp, Subject clientSubject,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        UsernamePasswordCredential credential = new UsernamePasswordCredential(username, password);
        status = validateUserAndPassword(getCDI(), "defaultRealm", clientSubject, credential, httpMessageContext);
        if (status == AuthenticationStatus.SUCCESS) {
            httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
            rspStatus = HttpServletResponse.SC_OK;
        }
        rsp.setStatus(rspStatus);
        return status;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

    protected AuthenticationStatus validateUserAndPassword(CDI cdi, String realmName, Subject clientSubject, UsernamePasswordCredential credential,
                                                         HttpMessageContext httpMessageContext) throws AuthenticationException {
        return validateCredential(cdi, realmName, clientSubject, credential, httpMessageContext);
    }

    protected AuthenticationStatus validateCredential(CDI cdi, String realmName, Subject clientSubject, Credential credential,
                                                         HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler(cdi);
        if (identityStoreHandler != null) {
            status = validateWithIdentityStore(realmName, clientSubject, credential, identityStoreHandler, httpMessageContext);
        } else {
            log.severe("IdentityStoreHandler object is not found.");
        }
        if (identityStoreHandler == null || status == AuthenticationStatus.NOT_DONE) {
            // If an identity store is not available, do nothing.
            log.severe("login is not completed.");
        }
        return status;
    }

    private AuthenticationStatus validateWithIdentityStore(String realmName, Subject clientSubject, Credential credential, IdentityStoreHandler identityStoreHandler,
                                                           HttpMessageContext httpMessageContext) {
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

    private void setLoginHashtable(String realmName, Subject clientSubject, CredentialValidationResult result) {
        Hashtable<String, Object> subjectHashtable = getSubjectHashtable(clientSubject);
        String callerPrincipalName = result.getCallerPrincipal().getName();
        String callerUniqueId = result.getCallerUniqueId();
        String realm = result.getIdentityStoreId();
        realm = realm != null ? realm : realmName;
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
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>(groups));
        } else {
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

    @SuppressWarnings("unchecked")
    private IdentityStoreHandler getIdentityStoreHandler(CDI cdi) {
        IdentityStoreHandler identityStoreHandler = null;
        Instance<IdentityStoreHandler> storeHandlerInstance = cdi.select(IdentityStoreHandler.class);
        if (storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        return identityStoreHandler;
    }
}
