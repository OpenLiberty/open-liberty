/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.modules.WSLoginModuleProxy;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
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

    public static final String IBM_KRB5_LOGIN_MODULE = "com.ibm.security.auth.module.Krb5LoginModule";

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

    /**
     * Application info for the application, if in started state, that provides the JAAS login module.
     * Always null if classProviderRef is not specified.
     */
    private ApplicationInfo classProviderAppInfo;

    /**
     * The shared library, if specified.
     */
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
            if (sharedLibrary == null && classProviderAppInfo == null) // nowhere to load the login module class from
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKS1147_JAAS_CUSTOM_LOGIN_MODULE_CP_LIB_MISSING",
                                                                    originalLoginModuleClassName,
                                                                    props.get("config.displayId")));
            else if (sharedLibrary != null && classProviderAppInfo != null) // conflicting locations to load from
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKS1146_JAAS_CUSTOM_LOGIN_MODULE_CP_LIB_CONFLICT",
                                                                    originalLoginModuleClassName,
                                                                    props.get("config.displayId")));

            options = processDelegateOptions(options, originalLoginModuleClassName, classProviderAppInfo, classLoadingService, sharedLibrary, false);
        }
        this.options = options;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    public static Map<String, Object> processDelegateOptions(Map<String, Object> inOptions, String originalLoginModuleClassName,
                                                             ApplicationInfo classProviderAppInfo, ClassLoadingService classLoadingService,
                                                             Library sharedLibrary, boolean jaasConfigFile) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(inOptions);
        String target = getTargetClassName(originalLoginModuleClassName, options);
        options.put(LoginModuleProxy.KERNEL_DELEGATE, WSLOGIN_MODULE_PROXY_CLASS);
        if (target != null) {
            // TODO error path if neither libraryRef nor classProviderRef are specified
            // For now, while this function is internal, this is unreachable because libraryRef is still mandatory

            ClassLoader loader;
            if (classProviderAppInfo == null)
                loader = classLoadingService == null ? null : classLoadingService.getSharedLibraryClassLoader(sharedLibrary);
            else if (classProviderAppInfo instanceof EARApplicationInfo)
                // load from an enterprise application (EAR) which might contain an embedded resource adapter
                loader = ((EARApplicationInfo) classProviderAppInfo).getApplicationClassLoader();
            else {
                NestedConfigHelper config = classProviderAppInfo.getConfigHelper();
                if ("rar".equals(config.get("type"))) { // the resourceAdapter metatype hard codes the type to "rar"
                    // load from a standalone resource adapter (RAR)
                    String classProviderId = (String) classProviderAppInfo.getConfigHelper().get("id");
                    String classProviderFilter = "(&"
                                                 + FilterUtils.createPropertyFilter("objectClass", ClassProvider.class.getName())
                                                 + FilterUtils.createPropertyFilter("id", classProviderId)
                                                 + ")";

                    BundleContext bundleContext = FrameworkUtil.getBundle(JAASLoginModuleConfigImpl.class).getBundleContext();
                    Collection<ServiceReference<ClassProvider>> classProviderRefs;
                    try {
                        classProviderRefs = bundleContext.getServiceReferences(ClassProvider.class, classProviderFilter);
                    } catch (InvalidSyntaxException x) {
                        throw new RuntimeException(x); // should be unreachable
                    }
                    ClassProvider classProvider = bundleContext.getService(classProviderRefs.iterator().next()); // TODO error checking
                    loader = classProvider.getDelegateLoader();
                } else {
                    // load from single-module applications such as web applications,
                    Container container = classProviderAppInfo.getContainer();
                    NonPersistentCache cache;
                    if (container == null)
                        cache = null;
                    else
                        try {
                            cache = container.adapt(NonPersistentCache.class);
                        } catch (UnableToAdaptException x) {
                            throw new IllegalStateException(classProviderAppInfo.getName(), x); // should be unreachable
                        }
                    ModuleInfo moduleInfo = cache == null ? null : (ModuleInfo) cache.getFromCache(ModuleInfo.class);
                    if (moduleInfo != null) {
                        loader = moduleInfo.getClassLoader();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "loading from standalone module " + moduleInfo.getName(), loader);
                    } else {
                        throw new IllegalArgumentException(); // TODO error message for unsupported application type, cannot load class
                    }
                }
            }

            Class<?> cl = null;
            try {
                //If the IBM Krb5LoginModule class is available then try to load the target class
                //OR, if it isn't available, only try to load the target class if it isn't the IBM Krb5LoginModule
                if (JavaInfo.isSystemClassAvailable(IBM_KRB5_LOGIN_MODULE) || !IBM_KRB5_LOGIN_MODULE.equalsIgnoreCase(target)) {
                    cl = Class.forName(target, false, loader);
                }
            } catch (ClassNotFoundException e) {
                if (classProviderAppInfo instanceof EARApplicationInfo)
                    cl = loadFromWebModules(target, (EARApplicationInfo) classProviderAppInfo);
                if (cl == null) {
                    if (classProviderAppInfo == null) {
                        //TODO consider different warning/error
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception performing class for name.", e);
                        }
                        if (jaasConfigFile) {
                            Tr.error(tc, "JAAS_CUSTOM_LOGIN_MODULE_CLASS_NOT_FOUND", originalLoginModuleClassName, e);
                        }
                    } else {
                        String displayId = (String) classProviderAppInfo.getConfigHelper().get("config.displayId");
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKS1148_JAAS_CUSTOM_LOGIN_MODULE_NOT_FOUND_BY_CP",
                                                                            originalLoginModuleClassName,
                                                                            classProviderAppInfo.getName(),
                                                                            displayId == null ? "application" : displayId));
                    }
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
     * Attempt to load a login module class from each of the application's web modules until it can be loaded,
     * or is found to not be loadable from any.
     *
     * @param className class name, including package, of the JAAS custom login module to load.
     * @param appInfo   information about the enterprise application.
     * @return the loaded class. Null if unable to load from any web module.
     */
    @FFDCIgnore(ClassNotFoundException.class)
    private static Class<?> loadFromWebModules(String className, EARApplicationInfo appInfo) {
        Container appContainer = appInfo.getContainer();
        NonPersistentCache cache;
        if (appContainer == null)
            cache = null;
        else
            try {
                cache = appContainer.adapt(NonPersistentCache.class);
            } catch (UnableToAdaptException x) {
                throw new IllegalStateException(appInfo.getName(), x); // should be unreachable
            }
        ApplicationClassesContainerInfo appClassesInfo = cache == null ? null : (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
        List<ModuleClassesContainerInfo> moduleClassesInfoList = appClassesInfo == null ? null : appClassesInfo.getModuleClassesContainerInfo();
        if (moduleClassesInfoList != null)
            for (ModuleClassesContainerInfo moduleClassesInfo : moduleClassesInfoList) {
                List<ContainerInfo> moduleContainerInfoList = moduleClassesInfo.getClassesContainerInfo();
                if (moduleContainerInfoList != null)
                    for (ContainerInfo moduleContainerInfo : moduleContainerInfoList) {
                        ContainerInfo.Type containerType = moduleContainerInfo.getType();
                        Container webContainer;
                        if (ContainerInfo.Type.WEB_MODULE == containerType) {
                            webContainer = moduleContainerInfo.getContainer();
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "skipping " + containerType);
                            webContainer = null;
                        }
                        if (webContainer == null)
                            cache = null;
                        else
                            try {
                                cache = webContainer.adapt(NonPersistentCache.class);
                            } catch (UnableToAdaptException x) {
                                throw new IllegalStateException(webContainer.getName(), x); // should be unreachable
                            }
                        ModuleInfo moduleInfo = cache == null ? null : (ModuleInfo) cache.getFromCache(ModuleInfo.class);
                        if (moduleInfo != null) {
                            ClassLoader loader = moduleInfo.getClassLoader();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "loading from web module " + moduleInfo.getName(), loader);
                            try {
                                return Class.forName(className, false, loader);
                            } catch (ClassNotFoundException x) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "not found in web module " + moduleInfo.getName());
                            }
                        }
                    }
            }

        return null;
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

    /** Required if classProviderRef is configured, in which case it will be called before activate */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setClassProvider(ApplicationInfo classProviderAppInfo) {
        this.classProviderAppInfo = classProviderAppInfo;
    }

    /** Required if libraryRef is configured, in which case it will be called before activate */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
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
}
