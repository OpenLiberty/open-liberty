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

package com.ibm.ws.jaxws.client.injection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.jaxws.client.JaxWsClientHandlerResolver;
import com.ibm.ws.jaxws.client.LibertyProviderImpl;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.JaxWsClientMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This object factory will be used to create instances of JAX-WS service subclasses or ports. It will be utilized by
 * both the resource injection engine and the JNDI naming code to create these instances.
 * 
 */
@Component(service = { javax.naming.spi.ObjectFactory.class, com.ibm.ws.jaxws.client.injection.ServiceRefObjectFactory.class },
           configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class ServiceRefObjectFactory implements javax.naming.spi.ObjectFactory {

    private static final TraceComponent tc = Tr.register(ServiceRefObjectFactory.class);

    private final AtomicServiceReference<JaxWsSecurityConfigurationService> securityConfigSR =
                    new AtomicServiceReference<JaxWsSecurityConfigurationService>("securityConfigurationService");

    /**
     * For getting the https host+port
     */
    private DynamicMBean httpsendpointInfoMBean;

    private DynamicMBean httpendpointInfoMBean;

    private ServerInfoMBean serverInfoMBean;

    @Activate
    protected void activate(ComponentContext cCtx) {
        securityConfigSR.activate(cCtx);
    }

    @Deactivate
    protected void deActivate(ComponentContext cCtx) {
        securityConfigSR.deactivate(cCtx);
    }

    @org.osgi.service.component.annotations.Reference(name = "securityConfigurationService", service = JaxWsSecurityConfigurationService.class,
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSecurityConfigurationService(ServiceReference<JaxWsSecurityConfigurationService> serviceRef) {
        securityConfigSR.setReference(serviceRef);
        LibertyProviderImpl.setSecurityConfigService(securityConfigSR);
    }

    protected void unsetSecurityConfigurationService(ServiceReference<JaxWsSecurityConfigurationService> serviceRef) {
        securityConfigSR.unsetReference(serviceRef);
    }

    @org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint)",
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC,
                                                      policyOption = ReferencePolicyOption.GREEDY)
    protected void setEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        this.httpendpointInfoMBean = endpointInfoMBean;
    }

    protected void unsetEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        if (this.httpendpointInfoMBean == endpointInfoMBean) {
            this.httpendpointInfoMBean = null;
        }
    }

    @org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl)",
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC,
                                                      policyOption = ReferencePolicyOption.GREEDY)
    protected void setHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        this.httpsendpointInfoMBean = endpointInfoMBean;
    }

    protected void unsetHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
        if (this.httpsendpointInfoMBean == endpointInfoMBean) {
            this.httpsendpointInfoMBean = null;
        }
    }

    /**
     * DS injection
     */
    @org.osgi.service.component.annotations.Reference
    protected void setServerInfoMBean(ServerInfoMBean serverInfoMBean) {
        this.serverInfoMBean = serverInfoMBean;
    }

    public String getWsdlUrl() {
        if (httpsendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpsendpointInfoMBean.getAttribute("Host"));
                int port = (Integer) httpsendpointInfoMBean.getAttribute("Port");
                return "https://" + host + ":" + port;
            } catch (Exception e) {

            }
        }
        if (httpendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpendpointInfoMBean.getAttribute("Host"));
                int port = (Integer) httpendpointInfoMBean.getAttribute("Port");
                return "http://" + host + ":" + port;
            } catch (Exception e) {

            }
        }
        return null;
    }

    /**
     * If the given host is "*", try to resolve this to a hostname or ip address
     * by first checking the configured ${defaultHostName}. If ${defaultHostName} is
     * "*", "localhost", or not specified, try obtaining the local ip address via InetAddress.
     * 
     * @return the resolved host, or "localhost" if the host could not be resolved
     */
    protected String resolveHost(String host) {
        if ("*".equals(host)) {
            // Check configured ${defaultHostName}
            host = serverInfoMBean.getDefaultHostname();
            if (host == null || host.equals("localhost")) {
                // This is, as a default, not useful. Use the local IP address instead.
                host = getLocalHostIpAddress();
            }
        }
        return (host == null || host.trim().isEmpty()) ? "localhost" : host;
    }

    /**
     * @return InetAddress.getLocalHost().getHostAddress(); or null if that fails.
     */
    protected String getLocalHostIpAddress() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            });

        } catch (PrivilegedActionException pae) {
            // FFDC it
            return null;
        }
    }

    public ServiceRefObjectFactory() {}

    /**
     * This method will create an instance of either a javax.xml.ws.Service subclass, or it will create an SEI type.
     * This will be called by either the resource injection engine or by the naming code when a JNDI lookup is done.
     */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context context, @Sensitive Hashtable<?, ?> environment) throws Exception {

        if (!(obj instanceof Reference)) {
            return null;
        }

        Reference ref = (Reference) obj;

        if (!ServiceRefObjectFactory.class.getName().equals(ref.getFactoryClassName())) {
            return null;
        }

        // Retrieve our service-ref metadata from the Reference object.
        WebServiceRefInfo wsrInfo = null;
        WebServiceRefInfoRefAddr wsrInfoRefAddr = (WebServiceRefInfoRefAddr) ref.get(WebServiceRefInfoRefAddr.ADDR_KEY);
        if (wsrInfoRefAddr != null) {
            wsrInfo = (WebServiceRefInfo) wsrInfoRefAddr.getContent();
        }

        // Make sure we found the WebServiceRefInfo object that contains the service-ref metadata.
        if (wsrInfo == null) {
            throw new Exception("Internal Error: Can not found the WebServiceRefInfo.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Service Ref JNDI name: " + wsrInfo.getJndiName());
        }

        // Get the client metadata
        JaxWsClientMetaData declaredClientMetaData = wsrInfo.getClientMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "declaredClientMetaData: " + declaredClientMetaData);
        JaxWsClientMetaData currentClientMetaData = JaxWsMetaDataManager.getJaxWsClientMetaData();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "currentClientMetaData: " + currentClientMetaData);
        if (declaredClientMetaData == null) {
            declaredClientMetaData = currentClientMetaData;
        }

        // If we didn't find the ClientMetaData, we have a problem.
        if (declaredClientMetaData == null) {
            throw new IllegalStateException("Internal Error: Can not found the JaxWsClientMetaData");
        }

        mergeWebServicesBndInfo(wsrInfo, declaredClientMetaData);

        //1. The Bus from client module which declares the serviceRef is used for locating WSDL or other usage.
        //2. The classLoader from current client module is used for loading service interface or other related stub classes.
        //The scenario is that, different modules could packge the same stub classes in their own classpaths.
        Bus originalThreadBus = BusFactory.getThreadDefaultBus(false);
        try {
            BusFactory.setThreadDefaultBus(declaredClientMetaData.getClientBus());
            // Collect all of our module-specific service-ref metadata.            
            TransientWebServiceRefInfo tInfo = new TransientWebServiceRefInfo(declaredClientMetaData, wsrInfo, currentClientMetaData.getModuleMetaData().getAppContextClassLoader());
            Object instance = getInstance(tInfo, wsrInfo);
            return instance;
        } finally {
            BusFactory.setThreadDefaultBus(originalThreadBus);
        }
    }

    /**
     * This method will create an instance of a JAX-WS service ref, using the metadata supplied in the WebServiceRefInfo
     * object.
     */
    private Object getInstance(TransientWebServiceRefInfo tInfo, WebServiceRefInfo wsrInfo) throws Exception {

        Object instance = null;

        // First, obtain an instance of the JAX-WS Service class.
        Service svc = null;

        List<WebServiceFeature> originalWsFeatureList = LibertyProviderImpl.getWebServiceFeatures();
        WebServiceRefInfo originalWebServiceRefInfo = LibertyProviderImpl.getWebServiceRefInfo();
        try {
            //Check @MTOM @RespectBinding @Addressing 
            //set web service features to ThreadLocal
            final List<WebServiceFeature> wsFeatureList =
                            wsrInfo.getWSFeatureForSEIClass(wsrInfo.getServiceRefTypeClassName());

            LibertyProviderImpl.setWebServiceRefInfo(wsrInfo);
            LibertyProviderImpl.setWebServiceFeatures(wsFeatureList);
            svc = getServiceInstance(tInfo, wsrInfo);
        } finally {
            LibertyProviderImpl.setWebServiceFeatures(originalWsFeatureList);
            LibertyProviderImpl.setWebServiceRefInfo(originalWebServiceRefInfo);
        }

        // Set handlerResolver
        svc.setHandlerResolver(new JaxWsClientHandlerResolver(tInfo.getWebServiceRefInfo(), tInfo.getClientMetaData()));

        // Next, retrieve the "type" class, which corresponds to the service-ref-type attribute.
        // If the "typeClass" is *not* the default (Object.class) *and* it's also not a subclass of the Service class,
        // then we have a situation where the user wants to do a port-type injection.
        // In that case, we need to call Service.getPort() on the service instance we just obtained above.
        Class<?> typeClass = tInfo.getServiceRefTypeClass();
        if (typeClass != null && !typeClass.getName().equals(Object.class.getName()) && !Service.class.isAssignableFrom(typeClass)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating a port instance based on class: " + tInfo.getServiceRefTypeClass().getName());
            }
            instance = svc.getPort(typeClass);
        } else {// Otherwise, this was just a normal Service-type injection so we'll return the service.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Service instance created based on class: " + svc.getClass().getName());
            }

            instance = svc;
        }

        return instance;

    }

    /**
     * This method will create an instance of a Service sub-class based on our metadata.
     */
    private Service getServiceInstance(TransientWebServiceRefInfo tInfo, WebServiceRefInfo wsrInfo) throws Exception {
        Class<?> svcSubClass = null;
        Service instance = null;
        if (tInfo.getServiceRefTypeClass() != null && Service.class.getName().equals(tInfo.getServiceRefTypeClassName())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating service instance using generic Service.create(QName)");
            }
            return Service.create(tInfo.getServiceQName());
        }

        if (tInfo.getServiceRefTypeClass() != null && Service.class.isAssignableFrom(tInfo.getServiceRefTypeClass())) {
            svcSubClass = tInfo.getServiceRefTypeClass();
        } else {
            svcSubClass = tInfo.getServiceInterfaceClass();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to create instance of service sub-class: " + svcSubClass.getName());
        }
        final Class<?> finalSvcSubClass = svcSubClass;
        Constructor<?> constructor = null;

        // first we get the constructor for the service subclass, we will always use
        // the URL, QName constructor because this constructor allow for null arguments
        try {
            final Constructor<?> finalConstructor = (Constructor<?>) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws NoSuchMethodException {
                    return finalSvcSubClass.getDeclaredConstructor(new Class[] { URL.class, QName.class });
                }
            });
            constructor = finalConstructor;
        } catch (PrivilegedActionException e) {
            if (e.getException() != null) {
                throw e.getException();
            } else {
                throw e;
            }
        }

        // now we will create the service instance with the constructor that was
        // previously created, it's okay if the URL we try to obtain turns out
        // to be null, a service instance will be created without the use of
        // the WSDL document we supplied
        final URL url = tInfo.getWsdlURL();

        //jsr-109:
//        For co-located clients (where the client and the server are in the same Java EE application unit) with
//        generated Service class, the location of the final WSDL document is resolved by comparing the Service name
//        on the @WebServiceClient annotation on the the generated Service to the Service names of all the deployed
//        port components in the Java EE application unit

        // Future plan need to consider:
//        if it is a co-located clients, need to verify the defined wsdlLocation and make it use the dynamic one
//        if there is no wsdlLocation defined, need to use the dynamic one
//        need to consider in the ear level
//        need to consider virtual host

        if (url == null)
        {
            JaxWsModuleMetaData jaxwsModuleMetaData = tInfo.getClientMetaData().getModuleMetaData();
            String applicationName = jaxwsModuleMetaData.getJ2EEName().getApplication();
            String contextRoot = jaxwsModuleMetaData.getContextRoot();
            Map<String, String> appNameURLMap = jaxwsModuleMetaData.getAppNameURLMap();
            Container moduleContainer = jaxwsModuleMetaData.getModuleContainer();
            NonPersistentCache overlayCache;
            try {
                overlayCache = moduleContainer.adapt(NonPersistentCache.class);
                JaxWsModuleInfo jaxWsModuleInfo = (JaxWsModuleInfo) overlayCache.getFromCache(JaxWsModuleInfo.class);
                if (jaxWsModuleInfo != null) {
                    for (EndpointInfo endpointInfo : jaxWsModuleInfo.getEndpointInfos()) {
                        String address = endpointInfo.getAddress(0).substring(1);
                        String serviceName = wsrInfo.getServiceQName().getLocalPart();
                        if (serviceName.equals(address))
                        {
                            String wsdlLocation = null;
                            if ((appNameURLMap != null) && (!appNameURLMap.isEmpty())) {
                                String applicationURL = appNameURLMap.get(applicationName);
                                wsdlLocation = applicationURL + "/" + address + "?wsdl";
                            } else {
                                wsdlLocation = getWsdlUrl() + contextRoot + "/" + address + "?wsdl";
                            }

                            final URL newURl = new URL(wsdlLocation);
                            wsrInfo.setWsdlLocation(wsdlLocation);

                            try {
                                final Constructor<?> finalConstructor = constructor;
                                final QName serviceQName = tInfo.getServiceQName();
                                instance = (Service) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                                    @Override
                                    public Object run() throws InstantiationException, IllegalAccessException, InvocationTargetException {
                                        finalConstructor.setAccessible(true);
                                        return finalConstructor.newInstance(new Object[] { newURl, serviceQName });
                                    }
                                });
                            } catch (PrivilegedActionException e) {
                                if (e.getException() != null) {
                                    throw e.getException();
                                } else {
                                    throw e;
                                }
                            }
                            break;

                        }

                    }
                }
            } catch (UnableToAdaptException e) {

            }
        }
        if (instance != null)
        {
            return instance;
        }

        if (url != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating Service with WSDL URL: " + url + " and QName: " + tInfo.getServiceQName() + " for class: " + svcSubClass.getName());
            }
        }
        try {
            final Constructor<?> finalConstructor = constructor;
            final QName serviceQName = tInfo.getServiceQName();
            instance = (Service) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws InstantiationException, IllegalAccessException, InvocationTargetException {
                    finalConstructor.setAccessible(true);
                    return finalConstructor.newInstance(new Object[] { url, serviceQName });
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getException() != null) {
                throw e.getException();
            } else {
                throw e;
            }
        }

        return instance;
    }

    /**
     * This class is simply a holder of various pieces of metadata which are specific to a particular invocation of
     * ServiceRefObjectFactory and should *not* be shared across modules/applications/etc. The WebServiceRefInfo object
     * that is obtained through the naming Reference object contains all the generic metadata associated with a
     * service-ref, such as the jndi name, wsdlLocation, and certain class names. Since a particular instance of
     * WebServiceRefInfo *might* be shared across modules/applications/servers, we can't store application or
     * module-specific information in it such as classloaders and classes. So while the naming Reference has a single
     * instance of the WebServiceRefInfo object that can be shared, each invocation of the
     * ServiceRefObjectFactory.getObjectInstance() method will need its own copy of the non-shared data (this class).
     */
    public class TransientWebServiceRefInfo {
        private Class<?> serviceInterfaceClass;
        private Class<?> serviceRefTypeClass;
        private final String wsdlLocation;
        private final QName serviceQName;
        private final QName portQName;

        private final ClassLoader classLoader;
        private final WebServiceRefInfo wsrInfo;
        private final JaxWsClientMetaData clientMetaData;
        private ModuleMetaData mmd;
        private final ComponentMetaData cmd;

        public TransientWebServiceRefInfo(JaxWsClientMetaData metadata, WebServiceRefInfo info, ClassLoader classLoader) {

            clientMetaData = metadata;
            wsrInfo = info;
            wsdlLocation = info.getWsdlLocation();
            serviceQName = info.getServiceQName();
            portQName = info.getPortQName();
            this.classLoader = classLoader;
            if (classLoader == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "TransientWebServiceRefInfo ClassLoader from Client Metadata is null. Getting Context ClassLoader.");
                }
                classLoader = getCurrentContextClassLoader();
            }

            cmd = JaxWsMetaDataManager.getComponentMetaData();
            if (cmd != null) {
                mmd = cmd.getModuleMetaData();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "TransientWebServiceRefInfo ctor:" +
                             "\n   ModuleMetaData : " + mmd +
                             "\n   ComponentMetaData: " + cmd +
                             "\n   ClientMetaData : " + clientMetaData +
                             "\n   ModuleName  : " + getName() +
                             "\n   ClassLoader    : " + classLoader +
                             "\n   WebServiceRefInfo: " + wsrInfo.toString());
            }

        }

        private ClassLoader getCurrentContextClassLoader() {
            ClassLoader cl = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
            return cl;
        }

        public JaxWsClientMetaData getClientMetaData() {
            return clientMetaData;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public Class<?> getServiceInterfaceClass() throws ClassNotFoundException {
            if (serviceInterfaceClass == null) {
                String className = wsrInfo.getServiceInterfaceClassName();
                if (className != null && !className.isEmpty()) {
                    serviceInterfaceClass = Class.forName(className, true, classLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Loaded service interface class: " + serviceInterfaceClass.getName());
                    }
                }
            }
            return serviceInterfaceClass;
        }

        public Class<?> getServiceRefTypeClass() throws ClassNotFoundException {
            if (serviceRefTypeClass == null) {
                String className = wsrInfo.getServiceRefTypeClassName();
                if (className != null && !className.isEmpty()) {
                    serviceRefTypeClass = Class.forName(className, true, classLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Loaded service ref type class: " + serviceRefTypeClass.getName());
                    }
                }
            }
            return serviceRefTypeClass;
        }

        public String getServiceInterfaceClassName() {
            return wsrInfo.getServiceInterfaceClassName();
        }

        public String getServiceRefTypeClassName() {
            return wsrInfo.getServiceRefTypeClassName();
        }

        public String getWsdlLocation() {
            return wsdlLocation;
        }

        public QName getServiceQName() {
            return serviceQName;
        }

        public QName getPortQName() {
            return portQName;
        }

        /**
         * @return the wsrInfo
         */
        public WebServiceRefInfo getWebServiceRefInfo() {
            return wsrInfo;
        }

        /**
         * This is a helper method to get a URL for the WSDL location.
         * 
         * @throws IOException
         * @throws MalformedURLException
         */
        @FFDCIgnore({ Exception.class, PrivilegedActionException.class })
        private URL getWsdlURL() throws IOException {
            URL url = null;
            final String resolvedWSDL = this.getWsdlLocation();
            if (resolvedWSDL != null && !resolvedWSDL.isEmpty()) {
                // First try to just new up a URL
                url = JaxWsUtils.resolve(resolvedWSDL, this.getClientMetaData().getModuleMetaData().getModuleContainer());

                // Next, try to find it via the ClassLoader.
                if (url == null) {
                    try {
                        url = AccessController.doPrivileged(new PrivilegedAction<URL>() {
                            @Override
                            public URL run() {
                                return classLoader.getResource(resolvedWSDL);
                            }
                        });
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to wsdlLocation URL via ClassLoader.getRResource(): {0}", e);
                        }
                    }
                }

                // Finally, try to open the WSDL as a File.
                if (url == null) {
                    try {
                        final File file = new File(resolvedWSDL);
                        url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                            @Override
                            public URL run() throws MalformedURLException {
                                // file.toURI() is always non-null
                                return file.toURI().toURL();
                            }
                        });
                    } catch (PrivilegedActionException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to obtain wsdlLocation URL via File object: {0}", e);
                        }
                    }

                }
            }

            return url;
        }

        public String getName() {
            return clientMetaData.getModuleMetaData().getName();
        }
    }

    /**
     * merge the configurations from the ibm-ws-bnd.xml
     * 
     * @param wsrInfo
     */
    private void mergeWebServicesBndInfo(WebServiceRefInfo wsrInfo, JaxWsClientMetaData jaxwsClientMetaData) {

        WebservicesBnd webServicesBnd = null;
        try {
            webServicesBnd = jaxwsClientMetaData.getModuleMetaData().getModuleContainer().adapt(WebservicesBnd.class);
        } catch (UnableToAdaptException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Can not get the custom binding file due to {0}", e);
            }
            return;
        }

        if (webServicesBnd != null) {

            String componenetName = wsrInfo.getComponenetName();
            com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef serviceRef = webServicesBnd.getServiceRef(wsrInfo.getJndiName(), componenetName);

            if (serviceRef != null) {
                List<Port> portList = serviceRef.getPorts();
                //store the port list in the map in WebServiceRefInfo object.
                if (portList != null && portList.size() > 0) {
                    for (Port port : portList) {
                        QName portQName = port.getPortQName();
                        PortComponentRefInfo portInfo = new PortComponentRefInfo(portQName);
                        portInfo.setAddress(port.getAddress());

                        portInfo.setUserName(port.getUserName());
                        portInfo.setPassword(port.getPassword());
                        portInfo.setSSLRef(port.getSSLRef());
                        portInfo.setKeyAlias(port.getKeyAlias());

                        //store the binding properties in the PortComponentRefInfo object.
                        portInfo.setProperties(port.getProperties());
                        wsrInfo.addPortComponentRefInfo(portInfo);
                    }
                }
                wsrInfo.setDefaultPortAddress(serviceRef.getPortAddress());

                //store the binding properties in the WebServiceRefInfo object.
                wsrInfo.setProperties(serviceRef.getProperties());

                String wsdlOverride = serviceRef.getWsdlLocation();
                if (wsdlOverride != null && !wsdlOverride.isEmpty()) {
                    wsrInfo.setWsdlLocation(wsdlOverride);
                }
            }
        }
    }
}
