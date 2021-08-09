/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.client.internal.jaas;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.login.Configuration;

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
import com.ibm.ws.clientcontainer.metadata.CallbackHandlerProvider;
import com.ibm.ws.security.client.internal.authentication.ClientAuthenticationService;
import com.ibm.ws.security.jaas.common.JAASChangeNotifier;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.internal.JAASLoginContextEntryImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * Service to let the JDK know which JAAS login configuration file to use
 */

@Component(service = JAASClientService.class,
                immediate = true,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = "service.vendor=IBM")
public class JAASClientService {
    private static final TraceComponent tc = Tr.register(JAASClientService.class);

    protected static final String KEY_JAAS_LOGIN_CONTEXT_ENTRY = "jaasLoginContextEntry";
    protected static final String KEY_JAAS_LOGIN_MODULE_CONFIG = "jaasLoginModuleConfig";
    protected static final String KEY_CHANGE_SERVICE = "jaasChangeNotifier";
    public static final String KEY_JAAS_CONFIG_FACTORY = "jaasConfigurationFactory";
    private static final String KEY_ID = "id";
    private static final String KEY_SERVICE_PID = "service.pid";
    public static final String KEY_CALLBACK_PROVIDER = "callbackHandlerProvider";
    public static final String KEY_CLIENT_AUTHN_SERVICE = "clientAuthenticationService";

    private static final AtomicServiceReference<ClientAuthenticationService> clientauthenticationServiceRef = new AtomicServiceReference<ClientAuthenticationService>(KEY_CLIENT_AUTHN_SERVICE);
    private static final AtomicServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = new AtomicServiceReference<JAASConfigurationFactory>(KEY_JAAS_CONFIG_FACTORY);
    private static final AtomicServiceReference<CallbackHandlerProvider> callbackHandlerRef = new AtomicServiceReference<CallbackHandlerProvider>(KEY_CALLBACK_PROVIDER);
    /** Map of login context entries -- BY ID: login contexts are only in this list when all of their login modules are found */
    protected ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>(KEY_JAAS_LOGIN_CONTEXT_ENTRY);

    /** List of context entries waiting for required login modules */
    protected final HashSet<ServiceReference<JAASLoginContextEntry>> pendingContextEntryRefs = new HashSet<ServiceReference<JAASLoginContextEntry>>();

    /** List of context entries waiting for required login modules */
    protected final HashSet<String> reportedFailures = new HashSet<String>();

    /** Map of login module services -- BY service PID */
    protected ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>(KEY_JAAS_LOGIN_MODULE_CONFIG);
    private final AtomicServiceReference<JAASChangeNotifier> jaasChangeNotifierService = new AtomicServiceReference<JAASChangeNotifier>(KEY_CHANGE_SERVICE);

    protected ComponentContext cc;
    protected Map<String, Object> properties;
    private JAASConfigurationFactory jaasConfigurationFactory;

    @Reference(service = JAASLoginContextEntry.class,
                    target = "(id=*)",
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
        synchronized (pendingContextEntryRefs) {
            jaasLoginContextEntries.removeReference((String) ref.getProperty(KEY_ID), ref);
            pendingContextEntryRefs.remove(ref);
        }
        modified(properties);
    }

    @Reference(service = JAASLoginModuleConfig.class,
                    target = "(id=*)",
                    name = KEY_JAAS_LOGIN_MODULE_CONFIG,
                    cardinality = ReferenceCardinality.MULTIPLE,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setJaasLoginModuleConfig(ServiceReference<JAASLoginModuleConfig> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        jaasLoginModuleConfigs.putReference(pid, ref);
        addedLoginModule();
    }

    /**
     * @param pid
     * @return
     */
    private boolean isDefaultLoginModule(String pid) {
        JAASLoginModuleConfig loginModule = jaasLoginModuleConfigs.getService(pid);
        if (loginModule != null)
            return loginModule.isDefaultLoginModule();
        else
            return false; // JAASLoginModuleConfig service is not ready yet
    }

    protected void updatedJaasLoginModuleConfig(ServiceReference<JAASLoginModuleConfig> ref) {
        modified(properties);
    }

    protected void unsetJaasLoginModuleConfig(ServiceReference<JAASLoginModuleConfig> ref) {
        String pid = (String) ref.getProperty(KEY_SERVICE_PID);
        jaasLoginModuleConfigs.removeReference(pid, ref);
        removedLoginModule();
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
    protected void processContextEntry(ServiceReference<JAASLoginContextEntry> ref) {
        String id = (String) ref.getProperty(KEY_ID);
        String[] modulePids = (String[]) ref.getProperty(JAASLoginContextEntryImpl.CFG_KEY_LOGIN_MODULE_REF);

        boolean changedEntryContext = false;
        if (modulePids == null || modulePids.length == 0) {
            Tr.error(tc, "JAAS_LOGIN_CONTEXT_ENTRY_HAS_NO_LOGIN_MODULE", id);
            changedEntryContext |= jaasLoginContextEntries.removeReference(id, ref);
        } else {
            if (haveAllModules(modulePids)) {
                jaasLoginContextEntries.putReference(id, ref);
                changedEntryContext = true;
            } else {
                // make sure it was not previously registered, as it now has a missing module
                changedEntryContext |= jaasLoginContextEntries.removeReference(id, ref);
                synchronized (pendingContextEntryRefs) {
                    pendingContextEntryRefs.add(ref);
                }
            }
        }

        if (changedEntryContext) {
            modified(properties);
        }
    }

    /**
     * Iterate through pending context entries, find any entries
     * that are now satisfied, and move them to the main list.
     **/
    private synchronized void addedLoginModule() {
        boolean changedEntries = false;

        synchronized (pendingContextEntryRefs) {
            // Check for pending entries that are now satisfied.
            Iterator<ServiceReference<JAASLoginContextEntry>> i = pendingContextEntryRefs.iterator();
            while (i.hasNext()) {
                ServiceReference<JAASLoginContextEntry> contextEntryRef = i.next();
                String[] entryModulePids = (String[]) contextEntryRef.getProperty(JAASLoginContextEntryImpl.CFG_KEY_LOGIN_MODULE_REF);
                if (haveAllModules(entryModulePids)) {
                    i.remove(); // remove from pending list
                    jaasLoginContextEntries.putReference((String) contextEntryRef.getProperty(KEY_ID), contextEntryRef);
                    changedEntries = true;
                }
            }
        }

        if (changedEntries) {
            modified(properties);
        }
    }

    /**
     * Iterate through context entries, find context entries
     * that are now missing required login modules -- remove them
     * and add to the pending list.
     * 
     * @param moduleId login module config id
     */
    private synchronized void removedLoginModule() {
        boolean changedEntries = false;

        // Check for context entries that are now unsatisfied
        Iterator<ServiceReference<JAASLoginContextEntry>> i = jaasLoginContextEntries.references().iterator();
        while (i.hasNext()) {
            ServiceReference<JAASLoginContextEntry> contextEntryRef = i.next();
            String[] entryModulePids = (String[]) contextEntryRef.getProperty(JAASLoginContextEntryImpl.CFG_KEY_LOGIN_MODULE_REF);
            if (!haveAllModules(entryModulePids)) {
                i.remove(); // remove from context entry map
                pendingContextEntryRefs.add(contextEntryRef);
                changedEntries = true;
            }
        }

        if (changedEntries) {
            modified(properties);
        }
    }

    private boolean haveAllModules(String[] modulePids) {
        boolean allIsWell = true;
        if (modulePids != null) {
            // Ok. there are modules specified. Do we know about them all?
            for (String modulePid : modulePids) {
                if (jaasLoginModuleConfigs.getReference(modulePid) == null) {
                    allIsWell = false;
                    break;
                }
            }
        }
        return allIsWell;
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

    protected void unsetJaasConfigurationFactory(ServiceReference<JAASConfigurationFactory> ref) {
        jaasConfigurationFactoryRef.unsetReference(ref);
    }

    @Reference(service = CallbackHandlerProvider.class,
                    name = KEY_CALLBACK_PROVIDER)
    public void setCallbackHandlerProvider(ServiceReference<CallbackHandlerProvider> ref) {
        callbackHandlerRef.setReference(ref);
    }

    protected void unsetCallbackHandlerProvider(ServiceReference<CallbackHandlerProvider> ref) {
        callbackHandlerRef.unsetReference(ref);
    }

    @Reference(service = ClientAuthenticationService.class,
                    name = KEY_CLIENT_AUTHN_SERVICE)
    public void setClientAuthenticationService(ServiceReference<ClientAuthenticationService> ref) {
        clientauthenticationServiceRef.setReference(ref);
    }

    protected void unsetClientAuthenticationService(ServiceReference<ClientAuthenticationService> ref) {
        clientauthenticationServiceRef.unsetReference(ref);
    }

    @Activate
    public void activate(ComponentContext cc, Map<String, Object> properties) {
        jaasLoginModuleConfigs.activate(cc);
        jaasLoginContextEntries.activate(cc);
        jaasChangeNotifierService.activate(cc);
        callbackHandlerRef.activate(cc);
        clientauthenticationServiceRef.activate(cc);
        jaasConfigurationFactoryRef.activate(cc);

        modified(properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        this.properties = properties;
        jaasConfigurationFactory = jaasConfigurationFactoryRef.getService();
        if (jaasConfigurationFactory != null) {
            // Register all the default JAAS configuration entries 
            jaasConfigurationFactory.installJAASConfiguration(jaasLoginContextEntries);

            synchronized (pendingContextEntryRefs) {
                // If we have unsatisfied context entries... 
                if (!pendingContextEntryRefs.isEmpty()) {
                    HashSet<String> missingModules = new HashSet<String>();
                    // Issue warnings for entries with modules that can't be found
                    for (ServiceReference<JAASLoginContextEntry> contextEntryRef : pendingContextEntryRefs) {
                        String[] entryModulePids = (String[]) contextEntryRef.getProperty(JAASLoginContextEntryImpl.CFG_KEY_LOGIN_MODULE_REF);
                        for (String pid : entryModulePids) {
                            if (!!!isDefaultLoginModule(pid))
                                if (jaasLoginModuleConfigs.getReference(pid) == null)
                                    missingModules.add(pid);
                        }
                    }
                    // remove previously missing modules that are now just fine, lest they show up again later.
                    reportedFailures.retainAll(missingModules);

                    // Only report missing modules if we haven't already said it was missing
                    for (String missing : missingModules) {
                        if (!reportedFailures.contains(missing)) {
                            if (!jaasLoginModuleConfigs.isEmpty()) {
                                //Display the warning msg only when we already bound to the jaasLoginModuleConfig service
                                Tr.warning(tc, "JAAS_LOGIN_MODULE_NOT_FOUND_FOR_LOGIN_MODULE_REF", missing);
                            }
                            reportedFailures.add(missing);
                        }
                    }
                } else {
                    configReady();
                }
            }
        }
    }

    /**
     * Notify interested parties that the configuration was changed only if the authentication service
     * is already up and running.
     */
    public void configReady() {
        JAASChangeNotifier notifier = jaasChangeNotifierService.getService();
        if (notifier != null) {
            notifier.notifyListeners();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jaasLoginContextEntries.deactivate(cc);
        jaasLoginModuleConfigs.deactivate(cc);
        jaasChangeNotifierService.deactivate(cc);
        callbackHandlerRef.deactivate(cc);
        clientauthenticationServiceRef.deactivate(cc);
        jaasConfigurationFactoryRef.deactivate(cc);

        Configuration.setConfiguration(null);
    }

    /**
     * Get the service for getting the callback handler specified in the client
     * application's deployment descriptor
     * 
     * @return the CallbackHandlerProvider service
     */
    public static CallbackHandlerProvider getCallbackHandlerProvider() {
        return callbackHandlerRef.getService();
    }

    /**
     * Get the service for creating the basic auth subject on the client side
     * 
     * @return the ClientAuthenticationService service
     */
    public static ClientAuthenticationService getClientAuthenticationService() {
        return clientauthenticationServiceRef.getServiceWithException();
    }
}
