/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.SecurityContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import com.ibm.adapter.message.WorkInformation;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This class defines a security context as specified by the JCA 1.6
 * specification. It has a method setupSecurityContext which will be invoked by
 * the WorkManager of the Application Server to setup the security context.
 * During the invocation the Application server passes in an executionSubject, a
 * JASPIC CallbackHandler and the serverSubject. This method will either
 * populate the executionSubject with the identities it needs the WorkManager to
 * establish or provide JASPIC callbacks to the handle method of the JASPIC
 * callbackhandler.
 */
public class FVTSecurityContext extends SecurityContext {

    private static final TraceComponent tc = Tr.register(
                                                         FVTSecurityContext.class, "adapter");

    // The workinfo provides information about the behaviour this Security
    // context should
    // exhibit
    private final WorkInformation workInfo;

    public FVTSecurityContext(WorkInformation info) {
        workInfo = info;
    }

    /**
     *
     * This method is invoked by the WorkManager of the Application Server to
     * setup the security context.
     *
     * @param handler
     *            The JASPIC Callbackhandler
     * @param executionSubject
     *            The subject under which the work should execute
     * @param serverSubject
     *            The subject of the server for mutual auth scenarios
     */
    @Override
    public void setupSecurityContext(CallbackHandler handler,
                                     Subject executionSubject, Subject serverSubject) {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "setupSecurityContext", new Object[] { handler,
                                                                executionSubject, serverSubject });
        }
        if (workInfo.isThrowsUnexpectedException())
            throw new IllegalStateException("Unexpected error occurred.");
        String[] callbacks = workInfo.getCallbacks();
        List<Callback> cbList = new ArrayList<Callback>();
        if (callbacks != null && callbacks.length > 0
            && !workInfo.isPassIdentityInSubject()) {
            for (String callback : callbacks) {
                if (callback.equals(WorkInformation.CALLERPRINCIPALCALLBACK)) {
                    String callerIdentity = workInfo.getCalleridentity();
                    Principal identity = workInfo.getIdentity();
                    CallerPrincipalCallback cpc = null;
                    if (workInfo.isSameSubject()) {
                        if (workInfo.getIdentity() != null) {
                            cpc = new CallerPrincipalCallback(executionSubject, identity);
                        } else {
                            cpc = new CallerPrincipalCallback(executionSubject, callerIdentity);
                        }
                    } else if (workInfo.isNullSubject()) {
                        cpc = new CallerPrincipalCallback(null, callerIdentity);
                    } else {
                        Set<Principal> set = new HashSet<Principal>();
                        Set<?> publicCred = new HashSet();
                        Set<?> privateCred = new HashSet();
                        set.add(new Principal() {
                            @Override
                            public String getName() {
                                return "Unknown";
                            }
                        });
                        Subject subject = new Subject(false, set, publicCred, privateCred);
                        cpc = new CallerPrincipalCallback(subject, callerIdentity);
                    }
                    cbList.add(cpc);
                } else if (callback
                                .equals(WorkInformation.GROUPPRINCIPALCALLBACK)) {
                    String[] groupNames = workInfo.getGroups();
                    GroupPrincipalCallback gpc = null;
                    if (workInfo.isSameSubject()) {
                        gpc = new GroupPrincipalCallback(executionSubject, groupNames);
                    } else if (workInfo.isNullSubject()) {
                        gpc = new GroupPrincipalCallback(null, groupNames);
                    } else {
                        Set<Principal> set = new HashSet<Principal>();
                        Set<?> publicCred = new HashSet();
                        Set<?> privateCred = new HashSet();
                        set.add(new Principal() {
                            @Override
                            public String getName() {
                                return "Unknown";
                            }
                        });
                        Subject subject = new Subject(false, set, publicCred, privateCred);
                        gpc = new GroupPrincipalCallback(subject, groupNames);
                    }
                    cbList.add(gpc);
                } else if (callback
                                .equals(WorkInformation.PASSWORDVALIDATIONCALLBACK)) {
                    String userName = workInfo.getUsername();
                    String password = workInfo.getPassword();
                    PasswordValidationCallback pvc = null;
                    if (workInfo.isSameSubject()) {
                        if (password != null) {
                            pvc = new PasswordValidationCallback(executionSubject, userName, password.toCharArray());
                        } else {
                            pvc = new PasswordValidationCallback(executionSubject, userName, null);
                        }
                    } else if (workInfo.isNullSubject()) {
                        pvc = new PasswordValidationCallback(null, userName, password.toCharArray());
                    } else {
                        Set<Principal> set = new HashSet<Principal>();
                        Set<?> publicCred = new HashSet();
                        Set<?> privateCred = new HashSet();
                        set.add(new WSPrincipalImpl("Unknown"));
                        Subject subject = new Subject(false, set, publicCred, privateCred);
                        pvc = new PasswordValidationCallback(subject, userName, password.toCharArray());
                    }
                    cbList.add(pvc);
                }
            }
        }
        if (workInfo.isPassIdentityInSubject()) {
            String[] identities = workInfo.getSubjectIdentities();
            if (identities == null || identities.length == 0) {
                Principal p = new WSPrincipalImpl(null);
                executionSubject.getPrincipals().add(p);
            }
            for (String identity : identities) {
                Principal p = new WSPrincipalImpl(identity);
                executionSubject.getPrincipals().add(p);
            }
        }
        if (workInfo.isAuthenticated()) {
            try {
                if (workInfo.isPassIdentityInSubject()) {
                    String[] identities = workInfo.getSubjectIdentities();
                    if (identities.length > 0)
                        login(executionSubject, identities[0]);
                }
                if (workInfo.getCalleridentity() != null) {
                    login(executionSubject, workInfo.getCalleridentity(),
                          workInfo.getPassword());
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        try {
            handler.handle(cbList.toArray(new Callback[] {}));
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (UnsupportedCallbackException e) {
            e.printStackTrace(System.out);
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "setupSecurityContext", new Object[] { handler,
                                                               executionSubject, serverSubject });
        }

    }

    /**
     *
     * A method to create an authenticated subject
     *
     * @param executionSubject
     * @param user
     */
    private void login(final Subject executionSubject, final String user) {
        try {
            PrivilegedExceptionAction<Object> pAction = new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    LoginContext loginCtx = new LoginContext("system.DEFAULT", executionSubject, new IdentityCallbackHandler(user));
                    // ContextManager cm = ContextManagerFactory.getInstance();
                    loginCtx.login();

                    // Subject authSubject = cm.login(cm.getAppRealm(), user,
                    // "system.DEFAULT", null, null, null,
                    // executionSubject);
                    Subject authSubject = loginCtx.getSubject();
                    if (executionSubject.getPrivateCredentials().isEmpty()) {
                        executionSubject.getPrincipals().addAll(
                                                                authSubject.getPrincipals());
                        executionSubject.getPrivateCredentials().addAll(
                                                                        authSubject.getPrivateCredentials());
                        executionSubject.getPublicCredentials().addAll(
                                                                       authSubject.getPublicCredentials());
                    }
                    return null;
                }
            };
            AccessController.doPrivileged(pAction);
        } catch (Exception e) {
            Tr.error(tc, "Login Failed in the Resource Adapter", e);
            e.printStackTrace(System.out);
        }
        // do a login using the execution subject

    }

    /**
     *
     * A method to create an authenticated subject
     *
     * @param executionSubject
     * @param user
     */
    private void login(final Subject executionSubject, final String user,
                       final String password) {

        try {

            PrivilegedExceptionAction<Object> pAction = new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    LoginContext loginCtx = new LoginContext("system.DEFAULT", executionSubject, new UserPasswordCallbackHandler(user, password));
                    // ContextManager cm = ContextManagerFactory.getInstance();
                    loginCtx.login();

                    // Subject authSubject = cm.login(cm.getAppRealm(), user,
                    // "system.DEFAULT", null, null, null,
                    // executionSubject);
                    Subject authSubject = loginCtx.getSubject();
                    if (executionSubject.getPrivateCredentials().isEmpty()) {
                        executionSubject.getPrincipals().addAll(
                                                                authSubject.getPrincipals());
                        executionSubject.getPrivateCredentials().addAll(
                                                                        authSubject.getPrivateCredentials());
                        executionSubject.getPublicCredentials().addAll(
                                                                       authSubject.getPublicCredentials());
                    }
                    return null;
                }
            };
            AccessController.doPrivileged(pAction);
        } catch (Exception e) {
            Tr.error(tc, "Login Failed in the Resource Adapter", e);
            e.printStackTrace(System.out);
        }
        // do a login using the execution subject

    }

    class WSPrincipalImpl implements Principal {
        String name;

        public WSPrincipalImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    class UserPasswordCallbackHandler implements CallbackHandler {
        private String username = null;
        private String password = null;

        public UserPasswordCallbackHandler(String pUsername, String pPassword) {
            username = pUsername;
            password = pPassword;
        }

        @Override
        public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(username);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
                }
            }
        }
    }

    class IdentityCallbackHandler implements CallbackHandler {
        private String username = null;
        private String password = null;
        Map<String, String> identity = new HashMap<String, String>();

        public IdentityCallbackHandler(String pUsername) {
            identity.put("Joseph", "p@ssw0rd");
            identity.put("Nitya", "pa$$w0rd");
            identity.put("Susan", "bistro");
            username = pUsername;
            password = identity.get(username);
        }

        @Override
        public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(username);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
                }
            }
        }
    }
}
