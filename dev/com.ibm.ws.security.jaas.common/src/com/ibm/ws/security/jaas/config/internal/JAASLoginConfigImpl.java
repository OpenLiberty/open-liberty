/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.config.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

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
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.internal.JAASLoginModuleConfigImpl;
import com.ibm.ws.security.jaas.config.JAASLoginConfig;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.library.Library;

@Component(service = { JAASLoginConfig.class }, name = "JAASLoginConfig", configurationPid = "com.ibm.ws.security.jaas.config.JAASLoginConfig", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class JAASLoginConfigImpl extends Parser implements JAASLoginConfig {
    private static TraceComponent tc = Tr.register(JAASLoginConfigImpl.class);

    private final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private static final String AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

    private static final String SKIP_WAS_PROXY_FOR_JAAS_CONFIG_FILE = "com.ibm.websphere.security.skipWASProxyForJaasConfigFile";

    private boolean skipProxy = false; //Default is inserting ProxyLoginModule for custom JAAS login module in jaas.conf file
    public static final List<String> defaultJaasLoginContextEntries = Collections.unmodifiableList(Arrays.asList(new String[] {
                                                                                                                                "system.UNAUTHENTICATED",
                                                                                                                                "system.WEB_INBOUND",
                                                                                                                                "system.DEFAULT",
                                                                                                                                "system.DESERIALIZE_CONTEXT",
                                                                                                                                "system.RMI_INBOUND",
                                                                                                                                "DefaultPrincipalMapping",
                                                                                                                                "WSLogin",
                                                                                                                                "ClientContainer" }));
    private String fileName;
    private volatile Library sharedLibrary = null;

    private ConfigFile configFile = null;
    private ClassLoadingService classLoadingService;

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    @Reference
    protected void setClassLoadingSvc(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    @Reference(service = Library.class, name = "sharedLibrary", target = "(id=jaasDefaultSharedLib)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSharedLib(Library svc) {
        sharedLibrary = svc;
    }

    protected void unsetSharedLib(Library svc) {
        sharedLibrary = svc;
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        locationAdminRef.activate(cc);
        modified(props);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        String fn = getSystemProperty(AUTH_LOGIN_CONFIG);
        if (fn != null) {
            fileName = resolveVariblePath(fn);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, (fileName == null ? "There is no JAAS login configuration file" : "JAAS login configuration file: " + fileName));
        }

        if (fileName != null) {
            configFile = new ConfigFile(fileName);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        locationAdminRef.deactivate(cc);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String getSystemProperty(final String propName) {
        String value = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
            }
        });
        return value;
    }

    private String resolveVariblePath(String fn) {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        if (locationAdmin != null) {
            return locationAdmin.resolveString(fn);
        } else {
            Tr.error(tc, "OSGI_SERVICE_ERROR", "WsLocationAdmin");
        }

        return fn;
    }

    @Override
    public Map<String, List<AppConfigurationEntry>> getEntries() {
        if (configFile != null) {
            return updateDelegateOptions(configFile.getFileMap());
        }
        return null;
    }

    /**
     *
     */
    private void isSkipProxy() {
        skipProxy = "true".equalsIgnoreCase(getSystemProperty(SKIP_WAS_PROXY_FOR_JAAS_CONFIG_FILE));
    }

    private Map<String, List<AppConfigurationEntry>> updateDelegateOptions(Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries) {
        Map<String, List<AppConfigurationEntry>> result = new HashMap<String, List<AppConfigurationEntry>>();
        List<String> skipDefaultJaasLoginContextEntries = new ArrayList<String>();
        isSkipProxy();
        for (Entry<String, List<AppConfigurationEntry>> entry : jaasConfigurationEntries.entrySet()) {
            String jaasLoginContextEnrty = entry.getKey();
            if (!defaultJaasLoginContextEntries.contains(jaasLoginContextEnrty)) {
                if (skipProxy) {
                    result.put(jaasLoginContextEnrty, entry.getValue());
                } else {
                    result.put(jaasLoginContextEnrty, entry.getValue());
                    List<AppConfigurationEntry> updateAppConfiguationEntries = updateAppConfiguration(entry);
                    result.put(jaasLoginContextEnrty, updateAppConfiguationEntries);
                }
            } else {
                skipDefaultJaasLoginContextEntries.add(jaasLoginContextEnrty);
            }
        }
        if (!skipDefaultJaasLoginContextEntries.isEmpty()) {
            Tr.warning(tc, "DEFAULT_JAAS_LOGIN_CONTEXT_ENTRY_SKIP", skipDefaultJaasLoginContextEntries.toString(), fileName);
            //TODO: ClientContainer and WSlogin are default loginModule for client
            //do we need to check for client and server and provide the message accordingly
        }
        return result;
    }

    private List<AppConfigurationEntry> updateAppConfiguration(Entry<String, List<AppConfigurationEntry>> entry) {
        List<AppConfigurationEntry> updateAppConfiguationEntries = new ArrayList<AppConfigurationEntry>();
        List<AppConfigurationEntry> appConfigurationEntries = entry.getValue();

        for (AppConfigurationEntry appConfigurationEntry : appConfigurationEntries) {
            Map<String, Object> options = (Map<String, Object>) appConfigurationEntry.getOptions();
            options = JAASLoginModuleConfigImpl.processDelegateOptions(options, appConfigurationEntry.getLoginModuleName(), null, classLoadingService, sharedLibrary, true);
            LoginModuleControlFlag controlFlag = appConfigurationEntry.getControlFlag();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loginModuleClassName: " + JAASLoginModuleConfig.LOGIN_MODULE_PROXY + " options: " + options.toString() + " controlFlag: " + controlFlag.toString());
            }
            updateAppConfiguationEntries.add(new AppConfigurationEntry(JAASLoginModuleConfig.LOGIN_MODULE_PROXY, controlFlag, options));
        }
        return updateAppConfiguationEntries;
    }
}
