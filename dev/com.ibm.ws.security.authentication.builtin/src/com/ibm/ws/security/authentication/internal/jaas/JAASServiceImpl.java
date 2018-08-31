/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.jaas;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin;
import com.ibm.ws.security.authentication.internal.JAASService;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.jaas.common.JAASChangeNotifier;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.callback.AuthenticationDataCallbackHandler;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@Component(immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class JAASServiceImpl implements JAASService {
    static final TraceComponent tc = Tr.register(JAASServiceImpl.class);
    public static final String KEY_USER_REGISTRY_SERVICE = "userRegistryService";
    public static final String KEY_CREDENTIALS_SERVICE = "credentialsService";
    public static final String KEY_TOKEN_MANAGER = "tokenManager";
    public static final String KEY_JAAS_LOGIN_CONTEXT_ENTRY = "JaasLoginContextEntry";
    public static final String KEY_JAAS_LOGIN_MODULE_CONFIG = "jaasLoginModuleConfig";
    public static final String KEY_JAAS_CONFIG_FACTORY = "jaasConfigurationFactory";
    public static final String KEY_COLLECTIVE_AUTHENTICATON_PLUGIN = "collectiveAuthenticationPlugin";
    static final String KEY_CHANGE_SERVICE = "jaasChangeNotifier";
    static final String KEY_ID = "id";
    static final String KEY_COMPONENT_NAME = "component.name";
    public static final String KEY_CERT_AUTHENTICATOR = "certificateAuthenticator";

    private static final AtomicServiceReference<TokenManager> tokenManager = new AtomicServiceReference<TokenManager>(KEY_TOKEN_MANAGER);
    private static final AtomicServiceReference<CredentialsService> credentialService = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);
    private static final AtomicServiceReference<UserRegistryService> userRegistryService = new AtomicServiceReference<UserRegistryService>(KEY_USER_REGISTRY_SERVICE);
    private static final AtomicServiceReference<CollectiveAuthenticationPlugin> collectiveAuthenticationPlugin = new AtomicServiceReference<CollectiveAuthenticationPlugin>(KEY_COLLECTIVE_AUTHENTICATON_PLUGIN);
    private static final AtomicServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = new AtomicServiceReference<JAASConfigurationFactory>(KEY_JAAS_CONFIG_FACTORY);

    /** Map of login context entries -- BY ID: login contexts are only in this list when all of their login modules are found */
    public ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>(KEY_JAAS_LOGIN_CONTEXT_ENTRY);

    private final AtomicServiceReference<JAASChangeNotifier> jaasChangeNotifierService = new AtomicServiceReference<JAASChangeNotifier>(KEY_CHANGE_SERVICE);

    public static ConcurrentServiceReferenceMap<String, CertificateAuthenticator> certificateAuthenticators = new ConcurrentServiceReferenceMap<String, CertificateAuthenticator>(KEY_CERT_AUTHENTICATOR);

    protected ComponentContext cc;
    protected Map<String, Object> properties;
    private JAASConfigurationFactory jaasConfigurationFactory;
    private static CollectiveAuthenticationPlugin cap = null;
    private static AuthenticationService authenticationService;

    @Reference(service = CollectiveAuthenticationPlugin.class,
               name = KEY_COLLECTIVE_AUTHENTICATON_PLUGIN,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    public void setCollectiveAuthenticationPlugin(CollectiveAuthenticationPlugin cap) {
        this.cap = cap;
        Tr.info(tc, "JAAS_LOGIN_COLLECTIVE_PLUGIN_AVAILABLE", new Object[] { cap.getClass().getSimpleName() });
    }

    public void unsetCollectiveAuthenticationPlugin(CollectiveAuthenticationPlugin cap) {
        if (cap == this.cap) {
            Tr.info(tc, "JAAS_LOGIN_COLLECTIVE_PLUGIN_UNAVAILABLE", new Object[] { cap.getClass().getSimpleName() });
            this.cap = null;
        }
    }

    public static CollectiveAuthenticationPlugin getCollectiveAuthenticationPlugin() {
        return cap;
    }

    @Reference(service = UserRegistryService.class, name = KEY_USER_REGISTRY_SERVICE)
    public void setUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryService.setReference(ref);
    }

    public void unsetUserRegistryService(ServiceReference<UserRegistryService> ref) {
        userRegistryService.unsetReference(ref);
    }

    public static UserRegistry getUserRegistry() throws RegistryException {
        UserRegistryService urs = userRegistryService.getService();
        return urs.getUserRegistry();
    }

    @Reference(service = TokenManager.class, name = KEY_TOKEN_MANAGER)
    public void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    public static TokenManager getTokenManager() {
        return tokenManager.getService();
    }

    public void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.unsetReference(ref);
    }

    @Reference(service = CredentialsService.class, name = KEY_CREDENTIALS_SERVICE)
    public void setCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialService.setReference(ref);
    }

    public void unsetCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialService.unsetReference(ref);
    }

    public static CredentialsService getCredentialsService() {
        return credentialService.getService();
    }

    // THIS IS NOT A DS-CALLED METHOD
    public static void setAuthenticationService(AuthenticationService authService) {
        authenticationService = authService;
    }

    public static AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public static void unsetAuthenticationService(AuthenticationService authService) {
        if (authenticationService == authService) {
            authenticationService = null;
        }
    }

    @Reference(service = JAASLoginContextEntry.class,
               name = KEY_JAAS_LOGIN_CONTEXT_ENTRY,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJaasLoginContextEntry(ServiceReference<JAASLoginContextEntry> ref) {
        processContextEntry(ref);
    }

    protected void updatedJaasLoginContextEntry(ServiceReference<JAASLoginContextEntry> ref) {
        processContextEntry(ref);
    }

    protected void unsetJaasLoginContextEntry(ServiceReference<JAASLoginContextEntry> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        if (jaasLoginContextEntries.removeReference(id, ref)) {
            modified(properties);
        }
    }

    @Reference(service = CertificateAuthenticator.class,
               name = KEY_CERT_AUTHENTICATOR,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setCertificateAuthenticator(ServiceReference<CertificateAuthenticator> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CertificateAuthenticator key: " + ref.getProperty(KEY_COMPONENT_NAME));
        }
        certificateAuthenticators.putReference(ref.getProperty(KEY_COMPONENT_NAME).toString(), ref);
    }

    protected void unsetCertificateAuthenticator(ServiceReference<CertificateAuthenticator> ref) {
        certificateAuthenticators.removeReference(ref.getProperty(KEY_COMPONENT_NAME).toString(), ref);
    }

    public static ConcurrentServiceReferenceMap<String, CertificateAuthenticator> getCertificateAuthenticators() {
        return certificateAuthenticators;
    }

    /**
     * Look at the login module pids required by the context entry:
     * <ul>
     * <li>If there are no module pids, and the entry id is not one of the defaults, issue a warning.
     * <li>If there are module pids, and some can't be found, add the entry to the pending list.
     * <li>If all is well, add it to the map.
     * </ul>
     * 
     * @param ref JAASLoginContextEntry service reference
     */
    private void processContextEntry(ServiceReference<JAASLoginContextEntry> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        String[] modulePids = (String[]) ref.getProperty(JAASLoginContextEntry.CFG_KEY_LOGIN_MODULE_REF);

        boolean changedEntryContext = false;
        if (modulePids == null || modulePids.length == 0) {
            // There are no modules specified
            if (JAASConfigurationImpl.defaultEntryIds.contains(id)) {
                // OK for default context entries to have no modules
                jaasLoginContextEntries.putReference(id, ref);
                changedEntryContext = true;
            } else {
                // make sure it was not previously registered
                Tr.error(tc, "JAAS_LOGIN_CONTEXT_ENTRY_HAS_NO_LOGIN_MODULE", id);
                changedEntryContext |= jaasLoginContextEntries.removeReference(id, ref);
            }
        } else {
            jaasLoginContextEntries.putReference(id, ref);
            changedEntryContext = true;
        }

        if (changedEntryContext) {
            modified(properties);
        }
    }

    @Reference(service = JAASChangeNotifier.class, name = KEY_CHANGE_SERVICE)
    protected void setJaasChangeNotifier(ServiceReference<JAASChangeNotifier> jaasChangeNotifierRef) {
        jaasChangeNotifierService.setReference(jaasChangeNotifierRef);
    }

    protected void unsetJaasChangeNotifier(ServiceReference<JAASChangeNotifier> jaasChangeNotifierRef) {
        jaasChangeNotifierService.unsetReference(jaasChangeNotifierRef);
    }

    @Reference(service = JAASConfigurationFactory.class, name = KEY_JAAS_CONFIG_FACTORY)
    public void setJaasConfigurationFactory(ServiceReference<JAASConfigurationFactory> ref) {
        jaasConfigurationFactoryRef.setReference(ref);
    }

    public void unsetJaasConfigurationFactory(ServiceReference<JAASConfigurationFactory> ref) {
        jaasConfigurationFactoryRef.unsetReference(ref);
    }

    /**
     * @param cc
     * @param props
     */
    @Activate
    public void activate(ComponentContext cc, Map<String, Object> properties) {
        jaasLoginContextEntries.activate(cc);
        tokenManager.activate(cc);
        credentialService.activate(cc);
        userRegistryService.activate(cc);
        jaasChangeNotifierService.activate(cc);
        collectiveAuthenticationPlugin.activate(cc);
        jaasConfigurationFactoryRef.activate(cc);
        certificateAuthenticators.activate(cc);

        modified(properties);
    }

    /**
     * @param newProperties
     */
    @Modified
    protected void modified(Map<String, Object> properties) {
        this.properties = properties;
        jaasConfigurationFactory = jaasConfigurationFactoryRef.getService();
        if (jaasConfigurationFactory != null) {
            // Register all the default JAAS configuration entries
            jaasConfigurationFactory.installJAASConfiguration(jaasLoginContextEntries);

            configReady();
        }
    }

    /**
     * @param cc
     */
    @Deactivate
    public void deactivate(ComponentContext cc) {
        tokenManager.deactivate(cc);
        credentialService.deactivate(cc);
        userRegistryService.deactivate(cc);
        jaasLoginContextEntries.deactivate(cc);
        jaasChangeNotifierService.deactivate(cc);
        collectiveAuthenticationPlugin.deactivate(cc);

        jaasConfigurationFactoryRef.deactivate(cc);
        certificateAuthenticators.deactivate(cc);
        Configuration.setConfiguration(null);
    }

    /** {@inheritDoc} */
    @Override
    public Subject performLogin(String jaasEntryName, AuthenticationData authenticationData, Subject partialSubject) throws LoginException {
        CallbackHandler callbackHandler = createCallbackHandlerForAuthenticationData(authenticationData);
        return performLogin(jaasEntryName, callbackHandler, partialSubject);
    }

    /**
     * Performs a JAAS login.
     * 
     * @param jaasEntryName
     * @param callbackHandler
     * @param partialSubject
     * @return the authenticated subject.
     * @throws javax.security.auth.login.LoginException
     */
    @Override
    public Subject performLogin(String jaasEntryName, CallbackHandler callbackHandler, Subject partialSubject) throws LoginException {
        LoginContext loginContext = null;
        loginContext = doLoginContext(jaasEntryName, callbackHandler, partialSubject);
        return (loginContext == null ? null : loginContext.getSubject());
    }

    /**
     * @param jaasEntryName
     * @param callbackHandler
     * @param partialSubject
     * @return
     * @throws LoginException
     */
    private LoginContext doLoginContext(String jaasEntryName, CallbackHandler callbackHandler, Subject partialSubject) throws LoginException {
        LoginContext loginContext;
        loginContext = createLoginContext(jaasEntryName, callbackHandler, partialSubject);
        loginContext.login();
        return loginContext;
    }

    public CallbackHandler createCallbackHandlerForAuthenticationData(final AuthenticationData authenticationData) {
        return new AuthenticationDataCallbackHandler(authenticationData);
    }

    public LoginContext createLoginContext(String jaasEntryName, CallbackHandler callbackHandler, Subject partialSubject) throws LoginException {
        LoginContext loginContext;
        if (partialSubject != null) {
            loginContext = new LoginContext(jaasEntryName, partialSubject, callbackHandler);
        } else {
            loginContext = new LoginContext(jaasEntryName, callbackHandler);
        }
        return loginContext;
    }

    /**
     * Notify interested parties that the configuration was changed only if the authentication service
     * is already up and running.
     */
    public void configReady() {
        if (authenticationService != null) {
            JAASChangeNotifier notifier = jaasChangeNotifierService.getService();
            if (notifier != null) {
                notifier.notifyListeners();
            }
        }
    }
}