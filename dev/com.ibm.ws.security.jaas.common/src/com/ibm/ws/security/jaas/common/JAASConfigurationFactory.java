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
package com.ibm.ws.security.jaas.common;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jaas.common.internal.JAASSecurityConfiguration;
import com.ibm.ws.security.jaas.config.JAASLoginConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@Component(service = JAASConfigurationFactory.class,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = "service.vendor=IBM")
public class JAASConfigurationFactory {

    private static TraceComponent tc = Tr.register(JAASConfigurationFactory.class);

    public static final String KEY_JAAS_CONFIGURATION = "jaasConfiguration";

    public static final String KEY_JAAS_LOGIN_CONFIG = "JAASLoginConfig";

    private static final AtomicServiceReference<JAASConfiguration> jaasConfigurationRef = new AtomicServiceReference<JAASConfiguration>(KEY_JAAS_CONFIGURATION);

    // jaas.conf file 
    private static final AtomicServiceReference<JAASLoginConfig> jaasLoginConfigRef = new AtomicServiceReference<JAASLoginConfig>(KEY_JAAS_LOGIN_CONFIG);

    private JAASSecurityConfiguration jaasSecurityConfiguration = null;
    private JAASConfiguration jaasConfiguration = null;

    Map<String, List<AppConfigurationEntry>> jaasConfigurationEntriesFromJaasConfig = null;

    @Reference(service = JAASLoginConfig.class,
                    name = KEY_JAAS_LOGIN_CONFIG,
                    cardinality = ReferenceCardinality.MANDATORY)
    public void setJAASLoginConfig(ServiceReference<JAASLoginConfig> ref) {
        jaasLoginConfigRef.setReference(ref);
    }

    protected void unsetJAASLoginConfig(ServiceReference<JAASLoginConfig> ref) {
        jaasLoginConfigRef.unsetReference(ref);
    }

    @Reference(service = JAASConfiguration.class,
                    name = KEY_JAAS_CONFIGURATION)
    public void setJAASConfiguration(ServiceReference<JAASConfiguration> ref) {
        jaasConfigurationRef.setReference(ref);
    }

    protected void unsetJAASConfiguration(ServiceReference<JAASConfiguration> ref) {
        jaasConfigurationRef.unsetReference(ref);
    }

    @Activate
    public void activate(ComponentContext cc) {
        jaasConfigurationRef.activate(cc);
        jaasLoginConfigRef.activate(cc);
        installJAASConfigurationFromJAASConfigFile();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jaasConfigurationRef.deactivate(cc);
    }

    public JAASConfigurationFactory() {}

    /**
     * This method install the JAAS configuration that specified in the server.xml/client.xml file
     */
    public synchronized void installJAASConfiguration(ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries) {
        jaasConfiguration = jaasConfigurationRef.getServiceWithException();
        jaasConfiguration.setJaasLoginContextEntries(jaasLoginContextEntries);
        Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = jaasConfiguration.getEntries();

        if (jaasSecurityConfiguration == null) {
            jaasSecurityConfiguration = new JAASSecurityConfiguration();
            Configuration.setConfiguration(jaasSecurityConfiguration);
        }
        if (jaasConfigurationEntriesFromJaasConfig != null) {
            checkForDuplicateEntries(jaasConfigurationEntries);
        }
        jaasSecurityConfiguration.setAppConfigurationEntries(jaasConfigurationEntries);
    }

    /**
     * This method optional install the JAAS configuration that specified in the jaas.conf file
     */
    protected synchronized void installJAASConfigurationFromJAASConfigFile() {
        JAASLoginConfig jaasLoginConfig = jaasLoginConfigRef.getService();
        if (jaasLoginConfig != null) {
            jaasConfigurationEntriesFromJaasConfig = jaasLoginConfig.getEntries();
            if (jaasConfigurationEntriesFromJaasConfig != null) {
                if (jaasSecurityConfiguration == null) {
                    jaasSecurityConfiguration = new JAASSecurityConfiguration();
                    Configuration.setConfiguration(jaasSecurityConfiguration);
                }
                jaasSecurityConfiguration.setAppConfigurationEntries(jaasConfigurationEntriesFromJaasConfig);
            }
        }
    }

    private void checkForDuplicateEntries(Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries) {
        for (Entry<String, List<AppConfigurationEntry>> entry : jaasConfigurationEntries.entrySet()) {
            String jaasLoginContextEnrty = entry.getKey();
            if (jaasConfigurationEntriesFromJaasConfig.containsKey(jaasLoginContextEnrty)) {
                Tr.warning(tc, "JAAS_DUPLICATE_ENTRY_NAME", jaasLoginContextEnrty);
            }
        }
    }

    public JAASSecurityConfiguration getJaasConfiguration() {
        return jaasSecurityConfiguration;
    }
}
