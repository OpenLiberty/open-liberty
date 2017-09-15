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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.client.jaas.modules.WSClientLoginModuleImpl;
import com.ibm.ws.security.jaas.common.JAASConfiguration;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * The method processes the jaasConfiguration elements in the client.xml and defaultInstances.xml.
 * 
 * */
@Component(service = JAASConfiguration.class,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = "service.vendor=IBM")
public class JAASClientConfigurationImpl implements JAASConfiguration {
    static final TraceComponent tc = Tr.register(JAASClientConfigurationImpl.class);

    public static final Class<WSClientLoginModuleImpl> WSCLIENTLOGIN_MODULE_IMPL_CLASS = WSClientLoginModuleImpl.class;

    private ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries;

    public static final String WAS_IGNORE_CLIENT_CONTAINER_DD = "was.ignoreClientContainerDD";

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaas.common.JAASConfiguration#setJaasLoginContextEntries(com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap)
     */
    @Override
    public void setJaasLoginContextEntries(ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries) {
        this.jaasLoginContextEntries = jaasLoginContextEntries;

    }

    /**
     * Get all jaasLoginContextEntry in the client.xml and defaultInstances.xml
     * 
     * @return a map of JAAS login context entry names to their corresponding AppConfigurationEntry objects
     */
    @Override
    public Map<String, List<AppConfigurationEntry>> getEntries() {
        Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();
        Map<String, String> jaasConfigIDs = new HashMap<String, String>();

        if (jaasLoginContextEntries != null) {
            Iterator<JAASLoginContextEntry> lcEntries = jaasLoginContextEntries.getServices();

            while (lcEntries.hasNext()) {
                JAASLoginContextEntry loginContextEntry = lcEntries.next();
                String entryName = loginContextEntry.getEntryName();

                List<JAASLoginModuleConfig> loginModules = loginContextEntry.getLoginModules();
                List<AppConfigurationEntry> appConfEntries = getLoginModules(loginModules, entryName);
                if (appConfEntries != null && !appConfEntries.isEmpty()) {
                    if (jaasConfigIDs.containsKey(entryName)) {
                        // if there is a duplicate name, log a warning message to indicate which id is being overwritten.
                        String id = jaasConfigIDs.get(entryName);
                        Tr.warning(tc, "JAAS_LOGIN_CONTEXT_ENTRY_HAS_DUPLICATE_NAME", new Object[] { entryName, id, loginContextEntry.getId() });
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "configure jaasContextLoginEntry id: " + loginContextEntry.getId());
                        Tr.debug(tc, "configure jaasContextLoginEntry: " + entryName + " has " + appConfEntries.size() + " loginModule(s)");
                        Tr.debug(tc, "appConfEntry: " + appConfEntries);
                    }
                    jaasConfigurationEntries.put(entryName, appConfEntries);
                    jaasConfigIDs.put(entryName, loginContextEntry.getId());
                }
            }
        }

        return jaasConfigurationEntries;
    }

    public List<AppConfigurationEntry> getLoginModules(List<JAASLoginModuleConfig> loginModules, String loginContextEntryName) {
        List<AppConfigurationEntry> loginModuleEntries = new ArrayList<AppConfigurationEntry>();
        for (JAASLoginModuleConfig loginModule : loginModules) {
            if (loginModule != null) {
                AppConfigurationEntry loginModuleEntry = createAppConfigurationEntry(loginModule, loginContextEntryName);
                loginModuleEntries.add(loginModuleEntry);
            } else {
                throw new IllegalStateException("Missing login module: found: " + loginModules);
            }
        }
        return loginModuleEntries;
    }

    /**
     * Create an AppConfigurationEntry object for the given JAAS login module
     * 
     * @param loginModule the JAAS login module
     * @param loginContextEntryName the JAAS login context entry name referencing the login module
     * @return the AppConfigurationEntry object
     * @throws IllegalArgumentException if loginModuleName is null, if LoginModuleName has a length of 0, if controlFlag is not either REQUIRED, REQUISITE, SUFFICIENT or OPTIONAL,
     *             or if options is null.
     */
    public AppConfigurationEntry createAppConfigurationEntry(JAASLoginModuleConfig loginModule, String loginContextEntryName) {
        String loginModuleClassName = loginModule.getClassName();
        LoginModuleControlFlag controlFlag = loginModule.getControlFlag();
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(loginModule.getOptions());
        if (JaasLoginConfigConstants.APPLICATION_WSLOGIN.equals(loginContextEntryName)) {
            options.put(WAS_IGNORE_CLIENT_CONTAINER_DD, true);
        }
        else {
            options.put(WAS_IGNORE_CLIENT_CONTAINER_DD, false);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "loginModuleClassName: " + loginModuleClassName + " options: " + options.toString() + " controlFlag: " + controlFlag.toString());
        }

        AppConfigurationEntry loginModuleEntry = new AppConfigurationEntry(loginModuleClassName, controlFlag, options);
        return loginModuleEntry;
    }
}
