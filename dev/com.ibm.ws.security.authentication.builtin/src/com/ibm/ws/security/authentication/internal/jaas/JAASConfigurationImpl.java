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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.security.authentication.jaas.modules.WSLoginModuleImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.jaas.common.JAASConfiguration;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.kerberos.auth.Krb5LoginModuleWrapper;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * The method process the jaasConfiguration in the file. If there is no jaasConfiguration in
 * the file, we create all the default entries.
 *
 */
@Component(service = JAASConfiguration.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class JAASConfigurationImpl implements JAASConfiguration {
    static final TraceComponent tc = Tr.register(JAASConfigurationImpl.class);

    static final List<String> defaultEntryIds = Collections.unmodifiableList(Arrays.asList(new String[] { JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED,
                                                                                                          JaasLoginConfigConstants.SYSTEM_WEB_INBOUND,
                                                                                                          JaasLoginConfigConstants.SYSTEM_DEFAULT,
                                                                                                          JaasLoginConfigConstants.SYSTEM_DESERIALIZE_CONTEXT, //TODO: Revisit after security context support is more stable. Do we really need to add all the login modules to this config?
                                                                                                          JaasLoginConfigConstants.SYSTEM_RMI_INBOUND,
                                                                                                          JaasLoginConfigConstants.APPLICATION_WSLOGIN }));
    public static final Class<WSLoginModuleImpl> WSLOGIN_MODULE_IMPL_CLASS = WSLoginModuleImpl.class;
    private ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries;

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
     * Get all jaasLoginContextEntry in the server.xml and create any missing default entries.
     * If there are no jaas configuration, then create all the default entries system.DEFAULT,
     * system.WEB_INBOUND, system.DESERIALIZE_CONTEXT, system.UNAUTHENTICATED and WSLogin
     *
     * @return
     */
    @Override
    public Map<String, List<AppConfigurationEntry>> getEntries() {
        Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();
        Map<String, String> jaasConfigIDs = new HashMap<String, String>();

        if (jaasLoginContextEntries != null) {
            createJAASClientLoginContextEntry(jaasConfigurationEntries);

            Iterator<JAASLoginContextEntry> lcEntries = jaasLoginContextEntries.getServices();

            while (lcEntries.hasNext()) {
                JAASLoginContextEntry loginContextEntry = lcEntries.next();
                String entryName = loginContextEntry.getEntryName();

                List<JAASLoginModuleConfig> loginModules = loginContextEntry.getLoginModules();
                if (JaasLoginConfigConstants.SYSTEM_DEFAULT.equalsIgnoreCase(entryName)) {
                    ensureProxyIsNotSpecifyInSystemDefaultEntry(entryName, loginModules);
                }
                List<AppConfigurationEntry> appConfEntry = getLoginModules(loginModules);
                if (appConfEntry != null && !appConfEntry.isEmpty()) {
                    if (jaasConfigIDs.containsKey(entryName)) {
                        // if there is a duplicate name, log a warning message to indicate which id is being overwritten.
                        String id = jaasConfigIDs.get(entryName);
                        Tr.warning(tc, "JAAS_LOGIN_CONTEXT_ENTRY_HAS_DUPLICATE_NAME", new Object[] { entryName, id, loginContextEntry.getId() });
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "configure jaasContextLoginEntry id: " + loginContextEntry.getId());
                        Tr.debug(tc, "configure jaasContextLoginEntry: " + entryName + " has " + appConfEntry.size() + " loginModule(s)");
                        Tr.debug(tc, "appConfEntry: " + appConfEntry);
                    }
                    jaasConfigurationEntries.put(entryName, appConfEntry);
                    jaasConfigIDs.put(entryName, loginContextEntry.getId());
                }
            }
        }

        return jaasConfigurationEntries;
    }

    /**
     * The proxy (WSLoginModuleProxy) can not be configured for the system.DEFAULT.
     *
     * @param entryName
     * @param loginModules
     */
    private void ensureProxyIsNotSpecifyInSystemDefaultEntry(String entryName, List<JAASLoginModuleConfig> loginModules) {
        for (Iterator<JAASLoginModuleConfig> i = loginModules.iterator(); i.hasNext();) {
            JAASLoginModuleConfig loginModule = i.next();
            if (loginModule.getId().equalsIgnoreCase(JAASLoginModuleConfig.PROXY)) {
                Tr.warning(tc, "JAAS_PROXY_IS_NOT_SUPPORT_IN_SYSTEM_DEFAULT");
                i.remove();
            }
        }
    }

    List<AppConfigurationEntry> getLoginModules(List<JAASLoginModuleConfig> loginModules) {
        List<AppConfigurationEntry> loginModuleEntries = new ArrayList<AppConfigurationEntry>();
        for (JAASLoginModuleConfig loginModule : loginModules) {
            if (loginModule != null) {
                AppConfigurationEntry loginModuleEntry = createAppConfigurationEntry(loginModule);
                loginModuleEntries.add(loginModuleEntry);
            } else {
                throw new IllegalStateException("Missing login module: found: " + loginModules);
            }
        }
        return loginModuleEntries;
    }

    AppConfigurationEntry createAppConfigurationEntry(JAASLoginModuleConfig loginModule) throws IllegalArgumentException {
        String loginModuleClassName = loginModule.getClassName();
        LoginModuleControlFlag controlFlag = loginModule.getControlFlag();
        Map<String, ?> options = loginModule.getOptions();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "loginModuleClassName: " + loginModuleClassName + " options: " + options.toString() + " controlFlag: " + controlFlag.toString());
        }

        AppConfigurationEntry loginModuleEntry = new AppConfigurationEntry(loginModuleClassName, controlFlag, options);
        return loginModuleEntry;
    }

    private void createJAASClientLoginContextEntry(Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries) throws IllegalArgumentException {

        List<AppConfigurationEntry> loginModuleEntries;
        if (Krb5Common.isIBMJdk18) {
            loginModuleEntries = createIBMJdk8Krb5loginModuleAppConfigurationEntry();
            jaasConfigurationEntries.put(JaasLoginConfigConstants.JAASClient, loginModuleEntries);
            jaasConfigurationEntries.put(Krb5LoginModuleWrapper.COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE, loginModuleEntries);
        } else if (Krb5Common.isOtherSupportJDKs) {
            loginModuleEntries = createJdk11Krb5loginModuleAppConfigurationEntry(false, "true");
            jaasConfigurationEntries.put(JaasLoginConfigConstants.JAASClient, loginModuleEntries);
            jaasConfigurationEntries.put(Krb5LoginModuleWrapper.COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE, loginModuleEntries);
            jaasConfigurationEntries.put(Krb5LoginModuleWrapper.COM_SUN_SECURITY_JGSS_KRB5_INITIATE, loginModuleEntries);

            //TODO:
            loginModuleEntries = createJdk11Krb5loginModuleAppConfigurationEntry(true, "true");
            jaasConfigurationEntries.put(Krb5LoginModuleWrapper.COM_SUN_SECURITY_JGSS_KRB5_ACCEPT, loginModuleEntries);
        }
    }

    private List<AppConfigurationEntry> createIBMJdk8Krb5loginModuleAppConfigurationEntry() {
        List<AppConfigurationEntry> loginModuleEntries = new ArrayList<AppConfigurationEntry>();
        String loginModuleClassName = Krb5LoginModuleWrapper.COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        LoginModuleControlFlag controlFlag = LoginModuleControlFlag.REQUIRED;
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("credsType", "both");
        options.put("forwardable", "true");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
            Tr.debug(tc, "loginModuleClassName: " + loginModuleClassName + " options: " + options.toString() + " controlFlag: " + controlFlag.toString());
        }

        AppConfigurationEntry loginModuleEntry = new AppConfigurationEntry(loginModuleClassName, controlFlag, options);
        if (loginModuleEntry != null)
            loginModuleEntries.add(loginModuleEntry);
        return loginModuleEntries;
    }

    private List<AppConfigurationEntry> createJdk11Krb5loginModuleAppConfigurationEntry(boolean proxy, String initiatorValue) {
        List<AppConfigurationEntry> loginModuleEntries = new ArrayList<AppConfigurationEntry>();
        Map<String, Object> options = new HashMap<String, Object>();
        String loginModuleClassName = Krb5LoginModuleWrapper.class.getCanonicalName();
        if (proxy) {
            loginModuleClassName = JAASLoginModuleConfig.LOGIN_MODULE_PROXY;
            options.put(LoginModuleProxy.KERNEL_DELEGATE, Krb5LoginModuleWrapper.class);
        }

        LoginModuleControlFlag controlFlag = LoginModuleControlFlag.REQUIRED;

        options.put("useKeyTab", "true");
        options.put("refreshKrb5Config", "true");
        options.put("doNotPrompt", "true");
        options.put("storeKey", "true");
        options.put("isInitiator", initiatorValue);
        options.put("keyTab", Krb5Common.getSystemProperty("KRB5_KTNAME"));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
            Tr.debug(tc, "loginModuleClassName: " + loginModuleClassName + " options: " + options.toString() + " controlFlag: " + controlFlag.toString());
        }

        AppConfigurationEntry loginModuleEntry = new AppConfigurationEntry(loginModuleClassName, controlFlag, options);
        if (loginModuleEntry != null)
            loginModuleEntries.add(loginModuleEntry);
        return loginModuleEntries;
    }
}