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
package com.ibm.ws.security.jaas.common.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.modules.WSLoginModuleProxy;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

@Component(configurationPid = "com.ibm.ws.security.authentication.internal.jaas.jaasLoginModuleConfig", configurationPolicy = ConfigurationPolicy.REQUIRE, property = "service.vendor=IBM")
public class JAASLoginModuleConfigImpl implements JAASLoginModuleConfig {
    private static final TraceComponent tc = Tr.register(JAASLoginModuleConfigImpl.class);

    public static final String CERTIFICATE = "certificate";
    public static final String HASHTABLE = "hashtable";
    public static final String IDENTITY_ASSERTION = "identityAssertion";
    public static final String TOKEN = "token";
    public static final String USERNAME_AND_PASSWORD = "userNameAndPassword";

    public static final String DELEGATE = "delegate";

    public static final Class<WSLoginModuleProxy> WSLOGIN_MODULE_PROXY_CLASS = com.ibm.ws.security.jaas.common.modules.WSLoginModuleProxy.class;

    public static final String WSLOGIN_MODULE_PROXY = WSLOGIN_MODULE_PROXY_CLASS.getName();

    public static final List<String> defaultLoginModuleIds = Collections.unmodifiableList(Arrays.asList(new String[] { HASHTABLE, USERNAME_AND_PASSWORD, CERTIFICATE, TOKEN,
                                                                                                                       HASHTABLE, PROXY,
                                                                                                                       IDENTITY_ASSERTION }));

    static final String CFG_KEY_ID = "id";
    static final String CFG_KEY_CLASSNAME = "className";
    static final String CFG_KEY_CONTROL_FLAG = "controlFlag";
    static final String CFG_KEY_OPTION_PID = "optionsRef";
    public final static String WAS_LM_SHARED_LIB = "WAS_LM_SHAREDLIB";

    private ModuleConfig moduleConfig;
    private LoginModuleControlFlag controlFlag = null;
    private Map<String, Object> options = Collections.emptyMap();

    /** The required shared library */
    private Library sharedLibrary;

    private ClassLoadingService classLoadingService;

    @Activate
    protected void activate(ModuleConfig moduleConfig, Map<String, Object> props) {
        this.moduleConfig = moduleConfig;
        processConfigProps(props);
    }

    /**
     * Process the properties from the server.xml/client.xml
     *
     * @param props
     */
    private void processConfigProps(Map<String, Object> props) {
        controlFlag = setControlFlag(moduleConfig.controlFlag());
        Map<String, Object> options = extractOptions(props);

        String originalLoginModuleClassName = moduleConfig.className();
        if (isDefaultLoginModule()) {
            String target = getTargetClassName(originalLoginModuleClassName, options);
            Class<?> cl = getTargetClassForName(target);
            options.put(LoginModuleProxy.KERNEL_DELEGATE, cl);
        } else {
            options = processDelegateOptions(options, originalLoginModuleClassName, classLoadingService, sharedLibrary, false);
        }
        this.options = options;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    public static Map<String, Object> processDelegateOptions(Map<String, Object> inOptions, String originalLoginModuleClassName, ClassLoadingService classLoadingService,
                                                             Library sharedLibrary, boolean jaasConfigFile) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(inOptions);
        String target = getTargetClassName(originalLoginModuleClassName, options);
        options.put(LoginModuleProxy.KERNEL_DELEGATE, WSLOGIN_MODULE_PROXY_CLASS);
        if (target != null) {
            ClassLoader loader = classLoadingService == null ? null : classLoadingService.getSharedLibraryClassLoader(sharedLibrary);
            Class<?> cl = null;
            try {
                if (isIBMJdk18Lower() || !"com.ibm.security.auth.module.Krb5LoginModule".equalsIgnoreCase(target)) {
                    //Do not initialize the IBM Krb5LoginModule if we are running with IBM JDK 18 or lower
                    cl = Class.forName(target, false, loader);
                }
            } catch (ClassNotFoundException e) {
                //TODO consider different warning/error
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception performing class for name.", e);
                }
                if (jaasConfigFile) {
                    Tr.error(tc, "JAAS_CUSTOM_LOGIN_MODULE_CLASS_NOT_FOUND", originalLoginModuleClassName, e);
                }
            }
            options.put(DELEGATE, cl);
        }
        return options;
    }

    private static String getTargetClassName(String className, Map<String, Object> options) {
        String target;
        if (WSLOGIN_MODULE_PROXY.equals(className)) {
            target = (String) options.get(DELEGATE);
            if (target == null || target.length() == 0) {
                Tr.error(tc, "JAAS_WSLOGIN_MODULE_PROXY_DELEGATE_NOT_SET");
            }
        } else {
            target = className;
        }
        return target;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private Class<?> getTargetClassForName(String tg) {
        Class<?> cl = null;
        ClassLoader contextClassLoader = null;
        try {
            ClassLoader bundleClassLoader = JAASLoginModuleConfigImpl.class.getClassLoader();
            contextClassLoader = classLoadingService.createThreadContextClassLoader(bundleClassLoader);
            cl = Class.forName(tg, true, contextClassLoader);
        } catch (ClassNotFoundException e) {
            //TODO consider different warning/error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception performing class for name.", e);
            }
        } finally {
            classLoadingService.destroyThreadContextClassLoader(contextClassLoader);
        }
        return cl;
    }

    /**
     * Process the option child element, store config attributes
     * in the options map
     *
     * @param pid retrieved from the map
     * @return
     */
    private Map<String, Object> extractOptions(Map<String, Object> props) {
        List<Map<String, Object>> optionsList = Nester.nest("options", props);
        if (!optionsList.isEmpty()) {
            Map<String, Object> options = new HashMap<String, Object>(optionsList.get(0).size());
            for (Map.Entry<String, Object> option : optionsList.get(0).entrySet()) {
                String key = option.getKey();
                if (key.startsWith(".")
                    || key.startsWith("config.")
                    || key.startsWith("service.")
                    || key.equals("id")) {
                    continue;
                }
                options.put(key, option.getValue());

            }
            return options;
        }
        return new HashMap<String, Object>(2);

    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return moduleConfig.id();
    }

    /** {@inheritDoc} */
    @Override
    public String getClassName() {
        return LOGIN_MODULE_PROXY;
    }

    static LoginModuleControlFlag setControlFlag(String flag) {
        if ("REQUISITE".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.REQUISITE;
        else if ("SUFFICIENT".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.SUFFICIENT;
        else if ("OPTIONAL".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.OPTIONAL;
        else
            return LoginModuleControlFlag.REQUIRED;
    }

    /** {@inheritDoc} */
    @Override
    public LoginModuleControlFlag getControlFlag() {
        return controlFlag;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, ?> getOptions() {
        return options;
    }

    /** Set required service, will be called before activate */
    @Reference
    protected void setSharedLib(Library svc) {
        sharedLibrary = svc;
    }

    @Override
    public boolean isDefaultLoginModule() {
        if (defaultLoginModuleIds.contains(moduleConfig.id()))
            return true;
        else
            return false;

    }

    @Reference
    protected void setClassLoadingSvc(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    private static boolean isIBMJdk18Lower() {
        return (JavaInfo.vendor() == Vendor.IBM && JavaInfo.majorVersion() <= 8);
    }

}
