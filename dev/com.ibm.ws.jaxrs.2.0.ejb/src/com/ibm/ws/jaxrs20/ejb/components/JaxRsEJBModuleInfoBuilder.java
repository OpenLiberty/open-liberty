/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.ejb.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

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
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilder;
import com.ibm.ws.jaxrs20.ejb.EJBInJarEndpointInfo;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;
import com.ibm.ws.jaxrs20.metadata.builder.AbstractJaxRsModuleInfoBuilder;
import com.ibm.ws.jaxrs20.support.JaxRsMetaDataManager;
import com.ibm.ws.jaxrs20.support.JaxRsWebContainerManager;
import com.ibm.ws.jaxrs20.utils.UriEncoder;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * F138708: this supports the EJB jaxrs in EJB bundle.
 * This builder will scan all possible jaxrs EJB beans and create endpointInfo into the JaxRsModuleInfo
 */
@Component(name = "com.ibm.ws.jaxrs20.module.info.ejbbuilder", immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxRsEJBModuleInfoBuilder extends AbstractJaxRsModuleInfoBuilder implements JaxRsModuleInfoBuilder {

    private static final TraceComponent tc = Tr.register(JaxRsEJBModuleInfoBuilder.class);

    /**
     * this service will help to create web router module and fired the related events
     */
    private final AtomicServiceReference<JaxRsWebContainerManager> jaxRsWebContainerManagerRef = new AtomicServiceReference<JaxRsWebContainerManager>("jaxRsWebContainerManager");

    /**
     * @param supportType
     */
    public JaxRsEJBModuleInfoBuilder() {
        super(JaxRsModuleType.EJB);
    }

    @Override
    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, Container containerToAdapt, JaxRsModuleInfo jaxRsModuleInfo) throws UnableToAdaptException {

        /**
         * step 1: check if there is any EJB beans
         */
        EJBEndpoints ejbEndpoints = containerToAdapt.adapt(EJBEndpoints.class);
        if (ejbEndpoints == null || ejbEndpoints.getEJBEndpoints().size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No possible JAXRS EJB bean is found");
            }
            return null;
        }

        /**
         * step 2: check if the EJB beans are subclass of Application or @Path or @Provider
         */
        Set<String> AllPathProviderClassNames = new HashSet<String>();
        Set<Class<?>> AllApplicationClasses = new HashSet<Class<?>>();

        JaxRsModuleMetaData jaxRsModuleMetaData = JaxRsMetaDataManager.getJaxRsModuleMetaData(moduleMetaData);
        ClassLoader appClassloader = jaxRsModuleMetaData.getAppContextClassLoader();
        String ejbModuleName = jaxRsModuleMetaData.getModuleInfo().getName();

        boolean isShareEJBJarWithJAXWS = false;
        for (EJBEndpoint ejb : ejbEndpoints.getEJBEndpoints()) {

            if (ejb.isWebService()) {
                isShareEJBJarWithJAXWS = true;
            }

            EJBType type = ejb.getEJBType();
            String beanName = ejb.getName();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing bean " + beanName + " of type " + type);
            }

            if (type.isSession() && (type.equals(EJBType.SINGLETON_SESSION) || type.equals(EJBType.STATELESS_SESSION))) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Processing a singleton or stateless session bean.");
                }

                List<String> interfaces = ejb.getLocalBusinessInterfaceNames();
                if (interfaces.size() == 0) {

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "This is a no-interface bean. Checking the bean class directly for annotations");
                    }

                    // check if this a bean annotated directly
                    processEJB(ejb, null, appClassloader, AllApplicationClasses, AllPathProviderClassNames);

                } else {
                    for (String ifcName : interfaces) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Processing interface {0} of bean {1}", new Object[] { ifcName, beanName });
                        }
                        processEJB(ejb, ifcName, appClassloader, AllApplicationClasses, AllPathProviderClassNames);

                    }

                }

            }

        }

        if (AllApplicationClasses.size() == 0 && AllPathProviderClassNames.size() == 0) {
            return null;
        }

        //set the check if sharing the same EJB jar with JAXWS
        jaxRsModuleInfo.setIsShareEJBJarWithJAXWS(isShareEJBJarWithJAXWS);

        /**
         * step 3: build endpointInfo for each Application class
         */

        LinkedHashMap<String, EndpointInfo> endpointInfoMap = new LinkedHashMap<String, EndpointInfo>();
        try {
            /**
             * no Application class, then make a endpoint who registers all resources & providers to a default Application class
             */
            if (AllApplicationClasses.size() == 0) {
                try {
                    registerEndpointInfo(endpointInfoMap, "javax.ws.rs.core.Application", null, null, "javax.ws.rs.core.Application", "/*", AllPathProviderClassNames,
                                         ejbModuleName);
                } catch (Exception e) {
                    // This one has some problem, let's skip it and continue.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when register endpoint javax.ws.rs.core.Application", e);
                    }
                }
            }
            /**
             * make a endpoint for each valid Application class
             */
            else {

                for (Class<?> appClass : AllApplicationClasses) {

                    String appClassName = appClass.getName();
                    try {
                        String appPath = getApplicationPathValue(appClass);

                        if (appPath == null) {
                            appPath = "/*";
                        }

                        registerEndpointInfo(endpointInfoMap, appClassName, null, null, appClassName, appPath, AllPathProviderClassNames, ejbModuleName);
                    } catch (Exception e) {
                        // This one has some problem, let's skip it and continue.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception when register endpoint " + appClassName, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception when build endpoint info in jaxrs ejb module: ", e);
            }
        }

        // All endpoint info are collected and without exception so far.
        // Go through collection, bulid endpointInfo
        for (Entry<String, EndpointInfo> entry : endpointInfoMap.entrySet()) {
            EndpointInfo endpointInfo = entry.getValue();
            //endpointInfoBuilder.build(endpointInfoBuilderContext, endpointInfo);
            //rollback the change
            jaxRsModuleInfo.addEndpointInfo(endpointInfo.getAppClassName(), endpointInfo);
            // There could be different EndpointInfo objects with different values for 
            // @ApplicationPath, though pointing to the same javax.ws.rs.core.Application subclass
            //String key = (endpointInfo.getAppPath() != null) ? endpointInfo.getAppPath() : endpointInfo.getAppClassName();
            //jaxRsModuleInfo.addEndpointInfo(key, endpointInfo);
        }

        /**
         * step 4: create the router module for this ejb module, it helps to publish all endpoints
         */
        if (jaxRsModuleInfo.getEndpointInfos() != null && jaxRsModuleInfo.getEndpointInfos().size() > 0) {
            return startWebRouterModule(containerToAdapt);
        } else {
            return null;
        }
    }

    protected ExtendedModuleInfo startWebRouterModule(Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache overlayCache = containerToAdapt.adapt(NonPersistentCache.class);
        ExtendedModuleInfo ejbModuleInfo = (ExtendedModuleInfo) overlayCache.getFromCache(EJBModuleInfo.class);
        JaxRsWebContainerManager jaxRsWebContainerManager = jaxRsWebContainerManagerRef.getService();
        if (jaxRsWebContainerManager == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No JaxRsWebContainerManager service is located, unable to publish the EJB jaxrs endpoints to web container");
            }
            return null;
        }
        return jaxRsWebContainerManager.createWebModuleInfo(ejbModuleInfo);
    }

    static private void registerEndpointInfo(LinkedHashMap<String, EndpointInfo> endpointInfoMap, String servletName, String servletClassName, String servletMappingUrl,
                                             String appClassName, String appPath, Set<String> providerAndPathClassNames, String ejbModuleName) throws Exception {

        String key = servletMappingUrl;
        appPath = UriEncoder.decodeString(appPath);
        if (key == null) {
            key = appPath;
        }
        if (key == null) {
            // Both servlet mapping url and application path are null
            throw new Exception("Both servlet mapping url and application path are null.");
        }

        if (endpointInfoMap.containsKey(key)) {
            // Found duplicated servlet mapping url, throw exception to fail application starting.           
            throw new Exception("Found duplicated servlet mapping url, throw exception to fail application starting.");
        }

        if ((servletName == null) || (appClassName == null) || (providerAndPathClassNames == null)) {
            // These values should not be null.
            throw new Exception("invalid values for servletName or appClassName or providerAndPathClassNames");
        }

        EJBInJarEndpointInfo endpointInfo = new EJBInJarEndpointInfo(servletName, servletClassName, servletMappingUrl, appClassName, appPath, providerAndPathClassNames);

        endpointInfo.setEJBModuleName(ejbModuleName);
        endpointInfoMap.put(key, endpointInfo);

        // If the application path has encoded characters, we must also create an EndpointInfo for 
        // the decoded URI that should correspond to the encoded URI. A URL pattern will then be added 
        // to the servlet mapping; otherwise, web container won't know what to do with the request
        // and will send back a 404
//        if (appPath != null && appPath.indexOf('%') > -1) {
//            String decodedURI = UriEncoder.decodeString(appPath);
//            EJBInJarEndpointInfo endpointInfo2 = new EJBInJarEndpointInfo(servletName, servletClassName, servletMappingUrl, appClassName, decodedURI, providerAndPathClassNames);
//            endpointInfoMap.put(decodedURI, endpointInfo2);
//        }
    }

    /**
     * processEJB
     * 
     * @param ejb
     * @param ejbInterfaceName
     * @param appClassloader
     * @param AllApplicationClasses
     * @param AllPathProviderClassNames
     */
    public void processEJB(EJBEndpoint ejb, String ejbInterfaceName, ClassLoader appClassloader, Set<Class<?>> AllApplicationClasses, Set<String> AllPathProviderClassNames) {

        getEJBApplicationSubclasses(AllApplicationClasses, ejb, appClassloader);
        getEJBWithProviderClasses(AllPathProviderClassNames, ejb, ejbInterfaceName, appClassloader);
        getEJBWithPathClasses(AllPathProviderClassNames, ejb, ejbInterfaceName, appClassloader);

    }

    /**
     * getApplicationSubclasses
     * 
     * @param classes
     * @param ejb
     * @param appClassloader
     */
    private void getEJBApplicationSubclasses(Set<Class<?>> classes, EJBEndpoint ejb, ClassLoader appClassloader) {
        final String methodName = "getEJBApplicationSubclasses";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        if (classes == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, methodName, Collections.emptySet());
            return;
        }

        Class<Application> appClass = Application.class;

        final String ejbClassName = ejb.getClassName();
        Class<?> c = null;
        try {
            c = appClassloader.loadClass(ejbClassName);
        } catch (ClassNotFoundException e) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " exit - due to Class Not Found for " + ejbClassName + ": " + e);
            }

        }

        if (c != null && appClass.isAssignableFrom(c)) {
            classes.add(c);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName, classes);

    }

    /**
     * getEJBWithProviderClasses
     * 
     * @param classes
     * @param ejb
     * @param ejbInterfaceName
     * @param appClassloader
     */
    private void getEJBWithProviderClasses(Set<String> classeNames, EJBEndpoint ejb, String ejbInterfaceName, ClassLoader appClassloader) {
        final String methodName = "getEJBWithProviderClasses";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, classeNames, ejb, ejbInterfaceName);

        if (classeNames == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, methodName, Collections.emptySet());
            return;
        }

        Class<Provider> providerClass = Provider.class;
        final String ejbClassName = (ejbInterfaceName == null) ? ejb.getClassName() : ejbInterfaceName;
        Class<?> c = null;
        try {
            c = appClassloader.loadClass(ejbClassName);
        } catch (ClassNotFoundException e) {

//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "getEJBWithProviderClasses() exit - due to Class Not Found for " + ejbClassName + ": " + e);
//            }
            Tr.error(tc, "error.failed.toloadejbclass", ejbClassName);
        }

        if (c != null && c.getAnnotation(providerClass) != null) {
            classeNames.add(ejbClassName);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName, classeNames);
    }

    /**
     * getEJBWithPathClasses
     * 
     * @param classes
     * @param ejb
     * @param ejbInterfaceName
     * @param appClassloader
     */
    private void getEJBWithPathClasses(Set<String> classeNames, EJBEndpoint ejb, String ejbInterfaceName, ClassLoader appClassloader) {
        final String methodName = "getEJBWithPathClasses";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, classeNames, ejb, ejbInterfaceName);
        if (classeNames == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, methodName, Collections.emptySet());
        }

        Class<Path> pathClass = Path.class;
        final String ejbClassName = (ejbInterfaceName == null) ? ejb.getClassName() : ejbInterfaceName;
        Class<?> c = null;
        try {
            c = appClassloader.loadClass(ejbClassName);
        } catch (ClassNotFoundException e) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getEJBWithPathClasses() exit - due to Class Not Found for " + ejbClassName + ": " + e);
            }
        }

        if (c != null && c.getAnnotation(pathClass) != null) {
            classeNames.add(ejbClassName);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName, classeNames);
    }

    private static String getApplicationPathValue(Class<?> applicationClassClass) {

        if (applicationClassClass == null) {
            return null;
        }
        ApplicationPath appPath = applicationClassClass.getAnnotation(ApplicationPath.class);
        if (appPath == null) {
            return null;
        }

        String value = appPath.value();
        value = UriEncoder.encodePath(value, true);

        if (!value.endsWith("/*")) {
            if (!value.endsWith("/")) {
                value = value + "/";
            }

            if (!value.endsWith("*")) {
                value = value + "*";
            }
        }

        return value;
    }

    @Override
    @Activate
    protected void activate(ComponentContext cc) {
        super.activate(cc);
        jaxRsWebContainerManagerRef.activate(cc);
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        super.deactivate(cc);
        jaxRsWebContainerManagerRef.deactivate(cc);
    }

    @Reference(name = "jaxRsWebContainerManager", service = JaxRsWebContainerManager.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setJaxRsWebContainerManager(ServiceReference<JaxRsWebContainerManager> ref) {
        jaxRsWebContainerManagerRef.setReference(ref);
    }

    protected void unsetJaxRsWebContainerManager(ServiceReference<JaxRsWebContainerManager> ref) {
        jaxRsWebContainerManagerRef.unsetReference(ref);
    }

}
