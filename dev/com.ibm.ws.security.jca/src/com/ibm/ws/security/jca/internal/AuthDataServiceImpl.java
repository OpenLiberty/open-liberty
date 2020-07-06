/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jca.internal;

import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSCredential;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.ws.security.jca.AuthDataService;
import com.ibm.ws.security.kerberos.auth.KerberosService;
import com.ibm.ws.security.kerberos.auth.Krb5LoginModuleWrapper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.auth.callback.WSMappingCallbackHandler;

/**
 * The AuthDataService is the interface to obtain the subject for an auth data alias.
 * The subject is optimized.
 * The concurrent access to the auth data config objects is protected by read and write locks for efficiency.
 *
 * NOTE: Caching of subjects was removed under defect 63520, due to the following two reasons
 * which we could not address in the GA timeframe:
 * 1) ManagedConnectionFactory on the PasswordCredential is used to compare which PasswordCredentials are relevant to
 * which ManagedConnectionFactory, so when multiple dataSources (means multiple ManagedConnectionFactories)
 * use a single authData, then when a ManagedConnectionFactory is given a subject where the PasswordCredential
 * constains the other ManagedConnectionFactory, then the PasswordCredential gets skipped, and the user/password
 * are not used in attempting the database connection, which then fails.
 * 2) ManagedConnectionFactory holds references to the JDBC driver classloader, and when configuration changes are made
 * since the subject cache keeps a reference to the old ManagedConnectionFactory, it causes a classloader leak.
 */
public class AuthDataServiceImpl implements AuthDataService {

    private static final TraceComponent tc = Tr.register(AuthDataServiceImpl.class);

    protected static final String CFG_KEY_ID = "id";
    protected static final String CFG_KEY_DISPLAY_ID = "config.displayId";
    protected static final String CFG_KEY_USER = "user";
    protected static final String CFG_KEY_PASSWORD = "password";
    protected static final String KEY_SECURITY_SERVICE = "securityService";

    private static final String KEY_AUTH_DATA_ALIAS = "com.ibm.mapping.authDataAlias";

    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    private final AtomicServiceReference<KerberosService> krb5ServiceRef = new AtomicServiceReference<KerberosService>("krb5Service");
    private final AtomicServiceReference<AuthDataProvider> authDataProviderRef = new AtomicServiceReference<AuthDataProvider>("authDataProvider");
    private final AtomicServiceReference<SubjectManagerService> smServiceRef = new AtomicServiceReference<SubjectManagerService>(SubjectManagerService.KEY_SUBJECT_MANAGER_SERVICE);

    @Reference(service = SecurityService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected void setKrb5Service(ServiceReference<KerberosService> ref) {
        krb5ServiceRef.setReference(ref);
    }

    protected void unsetKrb5Service(ServiceReference<KerberosService> ref) {
        krb5ServiceRef.unsetReference(ref);
    }

    protected void setAuthDataProvider(ServiceReference<AuthDataProvider> reference) {
        authDataProviderRef.setReference(reference);
    }

    protected void unsetAuthDataProvider(ServiceReference<AuthDataProvider> reference) {
        authDataProviderRef.unsetReference(reference);
    }

    protected void setSubjectManagerService(ServiceReference<SubjectManagerService> reference) {
        smServiceRef.setReference(reference);
    }

    protected void unsetSubjectManagerService(ServiceReference<SubjectManagerService> reference) {
        smServiceRef.unsetReference(reference);
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        krb5ServiceRef.activate(cc);
        authDataProviderRef.activate(cc);
        securityServiceRef.activate(cc);
        smServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        krb5ServiceRef.deactivate(cc);
        authDataProviderRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
        smServiceRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public Subject getSubject(ManagedConnectionFactory managedConnectionFactory, String jaasEntryName, Map<String, Object> loginData) throws LoginException {
        if (jaasEntryName != null) {
            return createSubjectUsingJAAS(jaasEntryName, managedConnectionFactory, loginData);
        } else {
            return createSubjectUsingAuthData(managedConnectionFactory, loginData);
        }
    }

    private Subject createSubjectUsingJAAS(String jaasEntryName, ManagedConnectionFactory managedConnectionFactory, Map<String, Object> loginData) throws LoginException {
        CallbackHandler callbackHandler = new WSMappingCallbackHandler(loginData, managedConnectionFactory);
        // NOTE: Do NOT add a doPriv here -- users must explicitly grant authority to user-defined login modules
        LoginContext loginContext = new LoginContext(jaasEntryName, callbackHandler);
        loginContext.login();
        Subject subject = loginContext.getSubject();
        addInvocationSubjectPrincipal(subject);
        return subject;
    }

    private Subject createSubjectUsingAuthData(ManagedConnectionFactory managedConnectionFactory, Map<String, Object> loginData) throws LoginException {
        String authDataAlias = getAuthDataAlias(loginData);
        AuthData authData = getAuthData(authDataAlias);
        return obtainSubject(managedConnectionFactory, authData);
    }

    private String getAuthDataAlias(Map<String, Object> loginData) {
        return loginData != null ? (String) loginData.get(KEY_AUTH_DATA_ALIAS) : null;
    }

    /**
     * Gets the auth data for the specified auth data alias.
     *
     * @param authDataAlias the auth data alias representing the auth data entry in the configuration.
     * @return the auth data.
     */
    @SuppressWarnings("static-access")
    private AuthData getAuthData(String authDataAlias) throws LoginException {
        return authDataProviderRef.getService().getAuthData(authDataAlias);
    }

    private Subject obtainSubject(ManagedConnectionFactory managedConnectionFactory, AuthData authData) throws LoginException {
        if (authData.getKrb5Principal() != null) {
            return doKerberosLogin(authData.getKrb5Principal());
        } else {
            Subject subject = createSubject(managedConnectionFactory, authData);
            addInvocationSubjectPrincipal(subject);
            optimize(subject);
            return subject;
        }
    }

    private Subject doKerberosLogin(String principal) throws LoginException {
        Subject subject = new Subject();
        Krb5LoginModuleWrapper krb5 = new Krb5LoginModuleWrapper();
        Map<String, String> options = new HashMap<String, String>();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Path keytab = krb5ServiceRef.getService().getKeytab();

        options.put("isInitiator", "true");
        options.put("refreshKrb5Config", "true");
        options.put("doNotPrompt", "true");
        options.put("useKeyTab", "true");
        // If no keytab path specified, still set useKeyTab=true because then the
        // default JDK or default OS locations will be checked
        if (keytab != null) {
            options.put("keyTab", keytab.toAbsolutePath().toString());
        }
        options.put("principal", principal);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
            Tr.debug(tc, "All kerberos config properties are: " + options);
        }

        krb5.initialize(subject, null, sharedState, options);
        krb5.login();
        krb5.commit();

        // If the created Subject does not have a GSSCredential, then create one and
        // associate it with the Subject
        Set<GSSCredential> gssCreds = subject.getPrivateCredentials(GSSCredential.class);
        if (gssCreds == null || gssCreds.size() == 0) {
            GSSCredential gssCred = SubjectHelper.createGSSCredential(subject);
            if (System.getSecurityManager() == null) {
                subject.getPrivateCredentials().add(gssCred);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        subject.getPrivateCredentials().add(gssCred);
                        return null;
                    }
                });
            }
        }

        return subject;
    }

    private Subject createSubject(ManagedConnectionFactory managedConnectionFactory, AuthData authData) {
        Subject subject = new Subject();
        String userName = authData.getUserName();
        char[] password = authData.getPassword();
        final PasswordCredential passwordCredential = new PasswordCredential(userName, password);
        passwordCredential.setManagedConnectionFactory(managedConnectionFactory);
        final Set<Object> creds = subject.getPrivateCredentials();
        if (System.getSecurityManager() == null)
            creds.add(passwordCredential);
        else
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    creds.add(passwordCredential);
                    return null;
                }
            });
        return subject;
    }

    private void addInvocationSubjectPrincipal(final Subject subject) {
        final WSPrincipal principal = getInvocationSubjectPrincipal();
        if (principal != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    subject.getPrincipals().add(principal);
                    return null;
                }
            });
        }
    }

    @FFDCIgnore(Exception.class)
    private WSPrincipal getInvocationSubjectPrincipal() {
        WSPrincipal principal = null;
        SubjectManagerService sms = smServiceRef.getService();
        if (sms != null) {
            final Subject finalInvocationSubject = sms.getInvocationSubject();
            if (finalInvocationSubject != null) {
                try {
                    principal = AccessController.doPrivileged(new GetInvocationSubjectAction(finalInvocationSubject));
                } catch (Exception e) {
                }
            }
        }
        return principal;
    }

    /**
     * To optimize the subject the subject must be marked as read only and be placed in the cache.
     */
    private void optimize(final Subject subject) {
        if (System.getSecurityManager() == null)
            subject.setReadOnly();
        else
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    subject.setReadOnly();
                    return null;
                }
            });
    }

    @Trivial
    private static class GetInvocationSubjectAction implements PrivilegedAction<WSPrincipal> {

        final Subject subj;

        public GetInvocationSubjectAction(Subject subject) {
            this.subj = subject;
        }

        @Override
        public WSPrincipal run() {
            WSPrincipal invocationSubjectPrincipal = subj.getPrincipals(com.ibm.ws.security.authentication.principals.WSPrincipal.class).iterator().next();
            return new WSPrincipal(invocationSubjectPrincipal.getName(), invocationSubjectPrincipal.getAccessId(), invocationSubjectPrincipal.getAuthenticationMethod());
        }
    }
}
