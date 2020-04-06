/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server.component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilder;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;
import com.ibm.ws.jaxrs20.server.internal.JaxRsServerConstants;
import com.ibm.ws.jaxrs20.utils.UriEncoder;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Build JaxRsModuleInfo for web services in web applications.
 *
 * EndpointInfo building order: if contains ejb in the war, then 1. ejb based
 * webservices which are not configured as servlet 2. ejb based webservices
 * which are also configured as servlet And then 1. pojo webservices which are
 * not configured as servlet(if not metadata complete) 2. pojo webservices which
 * are also configured as servlet
 */
@Component(name = "com.ibm.ws.jaxrs20.module.info.builder", immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxRsWebModuleInfoBuilder implements JaxRsModuleInfoBuilder {
    private final static TraceComponent tc = Tr.register(JaxRsWebModuleInfoBuilder.class);

    public JaxRsWebModuleInfoBuilder() {
    }

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData,
                                    Container containerToAdapt, JaxRsModuleInfo jaxRsModuleInfo) throws UnableToAdaptException {

        /**
         * if this JaxRsModuleInfo type is EJB, that means the EJBModuleInfoBuilder already process it
         * it must from EJB module, there is no reason for WebModuleInfoBuilder to handle it
         */
        if (jaxRsModuleInfo.getModuleType() == JaxRsModuleType.EJB) {
            return null;
        }

        WebAnnotations webAnnotations = AnnotationsBetaHelper.getWebAnnotations(containerToAdapt);

        try {
            WebAppConfig webAppConfig = containerToAdapt.adapt(WebAppConfig.class);

            AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();

            Set<String> allAppPathClassNames = new HashSet<String>();
            Set<String> allProviderAndPathClassNames = new HashSet<String>();

            // Scan annotation for @Provider, @Path, @ApplicationPath
            allProviderAndPathClassNames.addAll(annotationTargets.getAnnotatedClasses(Provider.class.getName()));
            allProviderAndPathClassNames.addAll(annotationTargets.getAnnotatedClasses(Path.class.getName()));
            allAppPathClassNames.addAll(annotationTargets.getAnnotatedClasses(ApplicationPath.class.getName()));

            // Process web.xml file firstly.
            // This is because for subclasses of javax.ws.rs.core.Application we
            // do not want to scan all classes to find them.
            // We will follow this logic:
            // First we analyze web.xml and found all subclasses. Besides that,
            // we will plus all subclasses which have @ApplicationPath
            // annotation.
            //
            // We check in this way is because, for each valid subclass, either
            // servlet mapping is defined, or @ApplicationPath annotation
            // presents.
            // For those subclasses have neither, we will treat them as invalid
            // and ignore them (cannot found at all).

            // Empty collection at the beginning.
            LinkedHashMap<String, EndpointInfo> endpointInfoMap = new LinkedHashMap<String, EndpointInfo>();

            Iterator<IServletConfig> cfgIter = webAppConfig.getServletInfos();
            while (cfgIter.hasNext()) {

                // Check servlet configuration one by one
                IServletConfig servletCfg = cfgIter.next();

                // All necessary information to register one endpoint
                String servletName = servletCfg.getServletName();
                String servletClassName = servletCfg.getClassName();
                String servletMappingUrl = null;
                String appClassName = null;
                String appPath = null;
                Set<String> thisAppProviderAndPathClassNames = null;

                // Initial these necessary information accordingly
                // If find anything wrong, just skip it and continue the next one.
                if (servletClassName == null) {
                    // Could be scenario #1 or #3
                    if (servletName.equals(JaxRsServerConstants.APPLICATION_ROOT_CLASS_NAME)) {
                        // Scenario #1
                        // E.g.
                        // <servlet>
                        //   <servlet-name>javax.ws.rs.core.Application</servlet-name>
                        // </servlet>
                        // <servlet-mapping>
                        //   <servlet-name>javax.ws.rs.core.Application</servlet-name>
                        //   <url-pattern>/sample1/*</url-pattern>
                        // </servlet-mapping>
                        //
                        // Normally this scenario means no sub-class of Application exists,
                        // and servlet mapping is required.

                        // Register endpoint info
                        appClassName = servletName;
                        servletMappingUrl = getServletMappingUrl(webAppConfig, servletName);
                        thisAppProviderAndPathClassNames = allProviderAndPathClassNames;
                    } else {
                        // Could be scenario #3
                        // E.g.
                        // <servlet>
                        //   <servlet-name>com.ibm.sample.jaxrs.DemoApplication</servlet-name>
                        // </servlet>
                        // <servlet-mapping>
                        //   <servlet-name>com.ibm.sample.jaxrs.DemoApplication</servlet-name>
                        //   <url-pattern>/sample4/*</url-pattern>
                        // </servlet-mapping>
                        //
                        // Now check if the servlet name is valid Application sub-class
                        // If it not found we will just ignore it.
                        appClassName = servletName;
                        Class<?> appClass = null;
                        Class<?> appBaseClass = null;
                        try {
                            appClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(appClassName);
                            appBaseClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(Application.class.getName());
                        } catch (ClassNotFoundException e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "ServletName could not be loaded.  This may be expected.", e);
                            }
                            continue;
                        }
                        if (appBaseClass.isAssignableFrom(appClass)) {
                            // This is scenario #3
                            try {
                                servletMappingUrl = getServletMappingUrl(webAppConfig, servletName);
                                appPath = getApplicationPathValue(appClass);
                                thisAppProviderAndPathClassNames = allProviderAndPathClassNames;
                            } catch (Exception e) {
                                // Skip the current application and continue the others
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Exception when collect metadata from class " + appClass.getName(), e);
                                }
                                continue;
                            }
                        } else {
                            // Servlet name is not sub-class of Application, not real scenario #3
                            // Skip this servlet configuration.
                            continue;
                        }
                    }
                }
                // Servlet class name is not null
                else {
                    // Could be scenario #2
                    // E.g.
                    // <servlet>
                    //   <servlet-name>sample2</servlet-name>
                    //   <servlet-class>com.ibm.ws.jaxrs20.webcontainer.LibertyJaxRsServlet</servlet-class>
                    //   <init-param>
                    //     <param-name>javax.ws.rs.Application</param-name>
                    //     <param-value>com.ibm.sample.jaxrs.DemoApplication</param-value>
                    //   </init-param>
                    // </servlet>
                    // <servlet-mapping>
                    //   <servlet-name>sample2</servlet-name>
                    //   <url-pattern>/sample2/*</url-pattern>
                    // </servlet-mapping>
                    if (servletClassName.equals(JaxRsServerConstants.LIBERTY_JAXRS_SERVLET_CLASS_NAME)) {
                        // This is scenario #2
                        IServletConfig sconfig = webAppConfig.getServletInfo(servletName);

                        // Check if valid sub-class of Application is defined in init-param
                        appClassName = sconfig.getInitParameter(JaxRsServerConstants.JAXRS_APPLICATION_PARAM);
                        if (appClassName == null) {
                            // No valid init-param defined. Skip this servlet configuration.
                            Tr.warning(tc, "warn.servlet.specified.without.application", new Object[] { moduleMetaData.getName(), servletName, servletClassName });

                            continue;
                        }
                        Class<?> appClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(appClassName);
                        Class<?> appBaseClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(Application.class.getName());
                        if (appBaseClass.isAssignableFrom(appClass)) {

                            try {
                                servletMappingUrl = getServletMappingUrl(webAppConfig, servletName);
                                appPath = getApplicationPathValue(appClass);
                                thisAppProviderAndPathClassNames = allProviderAndPathClassNames;
                            } catch (Exception e) {
                                // Skip the current application and continue the others
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Exception when collect metadata from class " + appClass.getName(), e);
                                }
                                continue;
                            }
                        } else {
                            // Not really sub-class of javax.ws.rs.core.Application, skip this servlet configuration
                            Tr.warning(tc, "warn.servlet.specified.with.invalid.application", new Object[] { moduleMetaData.getName(), servletName, appClassName });
                            continue;
                        }
                    } else {
                        // If it is some other JAX-RS servlet, we do not responsible for that.
                        // Just treat it as normal servlet and let web container to take care of it. Skip it.
                        continue;
                    }
                }

                try {
                    // All necessary information are ready. Register endpoint info
                    registerEndpointInfo(endpointInfoMap, servletName, servletClassName, servletMappingUrl, appClassName, appPath, thisAppProviderAndPathClassNames);
                    if (allAppPathClassNames.contains(appClassName)) {
                        allAppPathClassNames.remove(appClassName);
                    }
                } catch (Exception e) {
                    // This one has some problem, let's skip it and continue.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when register endpoint " + servletName, e);
                    }
                    continue;
                }
            }

            // Go through all left items in ApplicationPath classes collection.
            // new EndpointInfo for each, add into temp hashmap.
            // If ApplicationPath value conflict, throw exception.
            for (String appClassName : allAppPathClassNames) {

                Class<?> appClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(appClassName);
                /**
                 * fix the issue when a class with @ApplicationPath but not extends from Application
                 */
                Class<?> appBaseClass = JaxRsModuleMetaData.getJaxRsModuleMetaData(moduleMetaData).getAppContextClassLoader().loadClass(Application.class.getName());
                if (!appBaseClass.isAssignableFrom(appClass)) {
                    continue;
                }

                String appPath = null;
                Set<String> thisAppProviderAndPathClassNames = null;
                try {
                    appPath = getApplicationPathValue(appClass);
                    thisAppProviderAndPathClassNames = allProviderAndPathClassNames;
                } catch (Exception e) {
                    // Skip the current application and continue the others
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when collect metadata from class " + appClass.getName(), e);
                    }
                    continue;
                }

                try {
                    // Register endpoint info
                    registerEndpointInfo(endpointInfoMap, appClassName, null, null, appClassName, appPath, thisAppProviderAndPathClassNames);
                } catch (Exception e) {
                    // This one has some problem, let's skip it and continue.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception when register endpoint " + appClassName, e);
                    }
                    continue;
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

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception when build jaxrs web module info: ", e);
            }
        }

        return null;
    }

    static private void registerEndpointInfo(LinkedHashMap<String, EndpointInfo> endpointInfoMap, String servletName, String servletClassName, String servletMappingUrl,
                                             String appClassName, String appPath, Set<String> providerAndPathClassNames) throws Exception {

        String key = servletMappingUrl;
        appPath = UriEncoder.decodeString(appPath);
        if (key == null) {
            key = appPath;
        }
        if (key == null) {
            // Both servlet mapping url and application path are null
            throw new Exception("Both servlet mapping url and application path are null.");
        }

        EndpointInfo endpointInfo = new EndpointInfo(servletName, servletClassName, servletMappingUrl, appClassName, appPath, providerAndPathClassNames);
        EndpointInfo existingInfo = endpointInfoMap.get(key);
        if (existingInfo != null) {
            // Found duplicated servlet mapping url, throw exception to fail application starting.
            throw new Exception("Found duplicated servlet mapping url, " + key + ", with endpoint infos " + existingInfo
                                + " and " + endpointInfo + ", throw exception to fail application starting.");
        }

        if ((servletName == null) || (appClassName == null) || (providerAndPathClassNames == null)) {
            // These values should not be null.
            throw new Exception("invalid values for servletName or appClassName or providerAndPathClassNames");
        }

        endpointInfoMap.put(key, endpointInfo);

        // If the application path has encoded characters, we must also create an EndpointInfo for
        // the decoded URI that should correspond to the encoded URI. A URL pattern will then be added
        // to the servlet mapping; otherwise, web container won't know what to do with the request
        // and will send back a 404
//        if (appPath != null && appPath.indexOf('%') != -1) {
//            String decodedURI = UriEncoder.decodeString(appPath);
//            EndpointInfo endpointInfo2 = new EndpointInfo(servletName, servletClassName, servletMappingUrl, appClassName, decodedURI, providerAndPathClassNames);
//            endpointInfoMap.put(decodedURI, endpointInfo2);
//        }
    }

    static private String getServletMappingUrl(WebAppConfig webAppConfig, String servletName) {
        String url = null;
        IServletConfig sconfig = webAppConfig.getServletInfo(servletName);
        if ((sconfig != null) && (sconfig.getMappings() != null) && (sconfig.getMappings().size() > 0)) {
            for (String map : sconfig.getMappings()) {
                // Let's format the first one and use it.
                if ((map != null) && (map.length() > 0)) {
                    url = map.endsWith("*") ? map.substring(0, map.length() - 1) : map;
                    break;
                }
            }
        }
        return url;
    }

    static private String getApplicationPathValue(Class<?> applicationClassClass) {

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

    @Activate
    protected void activate(ComponentContext cc) {

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    }

    @Override
    public JaxRsModuleType getSupportType() {
        return JaxRsModuleType.WEB;
    }
}
