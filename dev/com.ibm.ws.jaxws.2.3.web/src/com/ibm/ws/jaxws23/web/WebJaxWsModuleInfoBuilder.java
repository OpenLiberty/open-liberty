/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws23.web;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.metadata.builder.AbstractJaxWsModuleInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilderExtension;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Build JaxWsModuleInfo for web services in web applications.
 *
 * EndpointInfo building order:
 * if contains ejb in the war, then
 * 1. ejb based webservices which are not configured as servlet
 * 2. ejb based webservices which are also configured as servlet
 * And then
 * 1. pojo webservices which are not configured as servlet(if not metadata complete)
 * 2. pojo webservices which are also configured as servlet
 */
public class WebJaxWsModuleInfoBuilder extends AbstractJaxWsModuleInfoBuilder {
    private final static TraceComponent tc = Tr.register(WebJaxWsModuleInfoBuilder.class);

    public WebJaxWsModuleInfoBuilder() {
        super(JaxWsModuleType.WEB);
    }

    @Override
    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, Container containerToAdapt, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {
        // check if it is router web module for EJB based Web services
        if ( JaxWsUtils.isEJBModule( JaxWsMetaDataManager.getJaxWsModuleMetaData(moduleMetaData).getModuleContainer() ) ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJB module; ignore web services");
            }
            return null;
        }

        EndpointInfoBuilder endpointInfoBuilder = endpointInfoBuilderSRRef.getService();
        if ( endpointInfoBuilder == null ) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No EndpointInfoBuilder; ignore web services");
            }
            return null;
        }

        // TODO: The placement of the info store in the context is problematic:
        //       The value should be removed from the context and managed wholly
        //       within this builder.
        //
        //       There are two problems: First, the reference persists into the
        //       postBuild call, where the info store is no longer open.  Second,
        //       obtaining the store is expensive and should be avoided whenever
        //       possible.

        WebAnnotations webAnnotations = AnnotationsBetaHelper.getWebAnnotations(containerToAdapt);
        InfoStore infoStore = webAnnotations.getInfoStore();

        EndpointInfoBuilderContext endpointInfoBuilderContext =
            new EndpointInfoBuilderContext(infoStore, containerToAdapt);

        JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext =
            new JaxWsModuleInfoBuilderContext(moduleMetaData, containerToAdapt, endpointInfoBuilderContext);

        Map<String, String> servletNameClassPairsInWebXML = getServletNameClassPairsInWebXML(containerToAdapt);
        jaxWsModuleInfoBuilderContext.addContextEnv(JaxWsConstants.SERVLET_NAME_CLASS_PAIRS_FOR_EJBSINWAR, servletNameClassPairsInWebXML);

        // call the extensions to extra pre build the jaxWsModuleInfo,
        // eg: endpointInfo for EJBs in War
        for ( JaxWsModuleInfoBuilderExtension extension : extensions ) {
            extension.preBuild(jaxWsModuleInfoBuilderContext, jaxWsModuleInfo);
        }

        webAnnotations.openInfoStore(); // See the TODO, above.  This ought to be avoided if possible.

        try {
            WebAppConfig webAppConfig = containerToAdapt.adapt(WebAppConfig.class);

            Set<String> presentedServices = jaxWsModuleInfo.getEndpointImplBeanClassNames();
            setupContextRoot(moduleMetaData, webAppConfig);
            setupVirtualHostConfig(moduleMetaData, webAppConfig);

            if ( !webAppConfig.isMetadataComplete() ) {
                Collection<String> implClassNamesInWebXML = servletNameClassPairsInWebXML.values();

                // d95160: The prior implementation obtained classes from the SEED location.
                //         That implementation is not changed by d95160.

                AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();

                Set<String> webServiceClassNames = annotationTargets.getAnnotatedClasses(WebService.class.getName());
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "WebService classes: " + webServiceClassNames);
                }

                Set<String> webServiceProviderClassNames = annotationTargets.getAnnotatedClasses(WebServiceProvider.class.getName()); 
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "WebServiceProvider classes: " + webServiceProviderClassNames);
                }

                Set<String> serviceClassNames = new HashSet<String>( webServiceClassNames.size() + webServiceProviderClassNames.size() );
                serviceClassNames.addAll(webServiceClassNames); 
                serviceClassNames.addAll(webServiceProviderClassNames);

                for ( String serviceImplBeanClassName : serviceClassNames ) {
                    String skipReason;
                    if ( implClassNamesInWebXML.contains(serviceImplBeanClassName ) ) {
                        skipReason = "Listed in web.xml";
                    } else if ( presentedServices.contains(serviceImplBeanClassName) ) {
                        skipReason = "Presentation service";
                    } else if ( webServiceClassNames.contains(serviceImplBeanClassName) ) {
                        skipReason = null;
                    } else if ( !JaxWsUtils.isWebService( infoStore.getDelayableClassInfo(serviceImplBeanClassName)) ) {
                        skipReason = "Not a web service";
                    } else {
                        skipReason = null;
                    }
                    if ( skipReason != null ) {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, "Skip [ " + serviceImplBeanClassName + " ]: " + skipReason);
                        }
                        continue;
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Process service [ " + serviceImplBeanClassName + " ]");
                        }
                    }

                    jaxWsModuleInfo.addEndpointInfo(
                        serviceImplBeanClassName,
                        endpointInfoBuilder.build(endpointInfoBuilderContext, serviceImplBeanClassName, EndpointType.SERVLET));
                }
            }

            // now process the serviceImplBeanClassName in web.xml.
            // maybe the serviceImplBeanClassName is in sharedLibs which
            // can not be read by webAnnotations.getAnnotationTargets().
            processClassesInWebXML(endpointInfoBuilder, endpointInfoBuilderContext, webAppConfig, jaxWsModuleInfo, presentedServices);

        } finally {
            webAnnotations.closeInfoStore();
        }

        // call the extensions to extra post build the jaxWsModuleInfo, eg: security settings
        for ( JaxWsModuleInfoBuilderExtension extension : extensions ) {
            extension.postBuild(jaxWsModuleInfoBuilderContext, jaxWsModuleInfo);
        }

        return null;
    }

    private void setupContextRoot(ModuleMetaData moduleMetaData, WebAppConfig webAppConfig) {
        JaxWsModuleMetaData jaxWsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(moduleMetaData);
        String contextRoot = webAppConfig.getContextRoot();
        jaxWsModuleMetaData.setContextRoot(contextRoot);
    }

    private void setupVirtualHostConfig(ModuleMetaData moduleMetaData, WebAppConfig webAppConfig) {

        String webAppName = webAppConfig.getApplicationName();
        String contextRoot = webAppConfig.getContextRoot();

        String configedVirtualHostName = webAppConfig.getVirtualHostName();
        JaxWsModuleMetaData jaxWsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(moduleMetaData);
        if (configedVirtualHostName == null) {
            return;
        }

        DynamicVirtualHostManager dvhm = VirtualHostOSGIService.getInstance().getDynamicVirtualHostManagerService();

        try {
            final Field transportMap = dvhm.getClass().getDeclaredField("transportMap");
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    transportMap.setAccessible(true);
                    return null;
                }
            });
            Object transportObj = transportMap.get(dvhm);
            if (transportObj != null) {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<String, VirtualHost> vhostMap = (ConcurrentHashMap<String, VirtualHost>) transportObj;
                VirtualHost vHost = vhostMap.get(configedVirtualHostName);
                if (vHost != null) {
                    String vHostURL = vHost.getUrlString(contextRoot, true);
                    jaxWsModuleMetaData.getAppNameURLMap().put(webAppName, vHostURL);
                }
            }

        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    /**
     * Get all the Servlet name and className pairs from web.xml
     */
    private Map<String, String> getServletNameClassPairsInWebXML(Container containerToAdapt) throws UnableToAdaptException {
        Map<String, String> nameClassPairs = new HashMap<String, String>();

        WebAppConfig webAppConfig = containerToAdapt.adapt(WebAppConfig.class);
        Iterator<IServletConfig> cfgIter = webAppConfig.getServletInfos();
        while (cfgIter.hasNext()) {
            IServletConfig servletCfg = cfgIter.next();
            if (servletCfg.getClassName() == null) {
                continue;
            }
            nameClassPairs.put(servletCfg.getServletName(), servletCfg.getClassName());
        }
        
        return nameClassPairs;
    }

    /**
     * Process the serviceImplBean classes in web.xml file.
     */
    private void processClassesInWebXML(
        EndpointInfoBuilder endpointInfoBuilder,
        EndpointInfoBuilderContext ctx,
        WebAppConfig webAppConfig,
        JaxWsModuleInfo jaxWsModuleInfo,
        Set<String> presentedServices) throws UnableToAdaptException {

        Iterator<IServletConfig> cfgIter = webAppConfig.getServletInfos();
        while ( cfgIter.hasNext() ) {
            IServletConfig servletCfg = cfgIter.next();

            String servletName = servletCfg.getServletName();
            String servletClassName = servletCfg.getClassName();

            String skipReason;
            if ( servletClassName == null ) {
            	skipReason = "Null servlet class name";
            } else if ( presentedServices.contains(servletClassName) ) {
            	skipReason = "Presented Service";
            } else if ( !JaxWsUtils.isWebService( ctx.getInfoStore().getDelayableClassInfo(servletClassName)) ) {
                skipReason = "Not a web service";
            } else {
            	skipReason = null;
            }
            if ( skipReason != null ) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skip servlet [ " + servletName + " : " + servletClassName + " ]: " + skipReason);
                }
                continue;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Process servlet [ " + servletName + " : " + servletClassName + " ]");
                }
            }

            ctx.addContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_SERVLET_NAME, servletName);

            jaxWsModuleInfo.addEndpointInfo(
            	servletName,
            	endpointInfoBuilder.build(ctx, servletClassName, EndpointType.SERVLET) );
        }
    }
}
