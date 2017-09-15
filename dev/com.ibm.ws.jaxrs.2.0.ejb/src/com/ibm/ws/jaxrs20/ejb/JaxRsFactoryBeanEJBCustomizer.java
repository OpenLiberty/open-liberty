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
package com.ibm.ws.jaxrs20.ejb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.ejb.internal.EjbProviderProxy;
import com.ibm.ws.jaxrs20.ejb.internal.JaxRsEJBConstants;
import com.ibm.ws.jaxrs20.metadata.CXFJaxRsProviderResourceHolder;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo.RuntimeType;
import com.ibm.ws.jaxrs20.server.internal.JaxRsServerConstants;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

@Component(name = "com.ibm.ws.jaxrs20.ejb.JaxRsFactoryEJBBeanCustomizer", immediate = true, property = { "service.vendor=IBM" })
public class JaxRsFactoryBeanEJBCustomizer implements JaxRsFactoryBeanCustomizer {
    private final TraceComponent tc = Tr.register(JaxRsFactoryBeanEJBCustomizer.class);
    private CXFJaxRsProviderResourceHolder cxfPRHolder;

    //private final Set<Class<ExceptionMapper<?>>> exceptionMappers = new HashSet<Class<ExceptionMapper<?>>>();

    private static final Set<String> SERVER_PROVIDER_CLASS_NAMES;
    static {
        SERVER_PROVIDER_CLASS_NAMES = new HashSet<String>();
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.MessageBodyWriter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.MessageBodyReader");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ExceptionMapper");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ContextResolver");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ReaderInterceptor");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.WriterInterceptor");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ParamConverterProvider");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.ContainerRequestFilter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.ContainerResponseFilter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.DynamicFeature");
        SERVER_PROVIDER_CLASS_NAMES.add("org.apache.cxf.jaxrs.ext.ContextResolver");
    }

    @Override
    public Priority getPriority() {
        return Priority.Medium;
    }

    @Override
    public synchronized void onPrepareProviderResource(BeanCustomizerContext context) {
        EndpointInfo endpointInfo = context.getEndpointInfo();
        String ejbModuleName = null;

        if (endpointInfo instanceof EJBInJarEndpointInfo) {
            ejbModuleName = ((EJBInJarEndpointInfo) endpointInfo).getEJBModuleName();
        }

        JaxRsModuleMetaData moduleMetaData = context.getModuleMetaData();
        //A map from EJB class to method-JNDI map
        Map<String, EJBInfo> ejbInfos = new HashMap<String, EJBInfo>();
        Set<ProviderResourceInfo> perRequestProviderAndPathInfos = endpointInfo.getPerRequestProviderAndPathInfos();
        Set<ProviderResourceInfo> singletonProviderAndPathInfos = endpointInfo.getSingletonProviderAndPathInfos();
        Iterator<ProviderResourceInfo> perRequestIterator = perRequestProviderAndPathInfos.iterator();
        Iterator<ProviderResourceInfo> singletonIterator = singletonProviderAndPathInfos.iterator();
        try {
            EJBEndpoints ejbEndpoints = moduleMetaData.getModuleContainer().adapt(EJBEndpoints.class);
            List<EJBEndpoint> ejbEndpointList = new ArrayList<EJBEndpoint>();
            ejbEndpointList.addAll(ejbEndpoints.getEJBEndpoints()); // ejbEndpoints.getEJBEndpoints() is an UnmodifiableCollection
            for (EJBEndpoint ejbEndpoint : ejbEndpointList) {
                //A map from methodToString to JNDI
                HashMap<String, String> methodToJNDI = new HashMap<String, String>();
                HashMap<String, Object> ejbCache = new HashMap<String, Object>();
                String ejbClassName = ejbEndpoint.getClassName();
                String ejbName = ejbEndpoint.getName();
                List<String> localInterfaceNameList = ejbEndpoint.getLocalBusinessInterfaceNames();
                EJBInfo ejbInfo = new EJBInfo(ejbClassName, ejbName, ejbEndpoint.getEJBType(), methodToJNDI, ejbCache, localInterfaceNameList, ejbModuleName);
                ejbInfos.put(ejbClassName, ejbInfo);
                //if the impl bean doesn't implements any inerface, then add the jndi for mehtod of this imp bean
                if (localInterfaceNameList.size() == 0) {
                    Method[] methods = Thread.currentThread().getContextClassLoader().loadClass(ejbClassName).getMethods();
                    String jndiName = getJNDIName(ejbEndpoint, null, ejbModuleName);
                    for (Method m : methods) {
                        String implBeanMthod = EJBUtils.methodToString(m);
                        methodToJNDI.put(implBeanMthod, jndiName);
                    }

                } else {
                    for (String localInterfaceName : localInterfaceNameList) {
                        Class<?> localInterface = moduleMetaData.getAppContextClassLoader().loadClass(localInterfaceName);
                        Method[] localInterfaceMethods = localInterface.getMethods();
                        //Add method to JNDI mapping
                        for (Method localInterfaceMethod : localInterfaceMethods) {
                            String localInterfaceMethodToString = EJBUtils.methodToString(localInterfaceMethod);
                            String jndiName = getJNDIName(ejbEndpoint, localInterfaceName, ejbModuleName);
                            methodToJNDI.put(localInterfaceMethodToString, jndiName);
                        }
                    }
                }

            }
            cxfPRHolder = context.getCxfRPHolder();
            findEJBClass(ejbEndpointList, perRequestIterator, true);
            findEJBClass(ejbEndpointList, singletonIterator, false);
            handleAbstractClassInterface(moduleMetaData, ejbEndpointList, endpointInfo.getAbstractClassInterfaceList(), perRequestProviderAndPathInfos,
                                         singletonProviderAndPathInfos, ejbModuleName);
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to get EJBEndpoints due to adapt failed");
            }
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to load EJB class " + e.toString());
            }
        }
        context.setContextObject(ejbInfos);
    }

    /**
     * @param moduleMetaData
     * @param ejbEndpointList
     * @param abstractClassInterfaceList
     * @param singletonProviderAndPathInfos
     * @param perRequestProviderAndPathInfos
     * @param cxfPRHolder
     */
    private void handleAbstractClassInterface(JaxRsModuleMetaData moduleMetaData, List<EJBEndpoint> ejbEndpointList, List<String> abstractClassInterfaceList,
                                              Set<ProviderResourceInfo> perRequestProviderAndPathInfos, Set<ProviderResourceInfo> singletonProviderAndPathInfos,
                                              String ejbModuleName) {
        for (String abstractClassInterfaceName : abstractClassInterfaceList) {
            for (int i = 0; i < ejbEndpointList.size(); i++) {
                EJBEndpoint ejbEndpoint = ejbEndpointList.get(i);
                EJBType ejbType = ejbEndpoint.getEJBType();
                if (!(ejbType.equals(EJBType.SINGLETON_SESSION) || ejbType.equals(EJBType.STATELESS_SESSION)))
                    // jaxrs only handle singleton session bean or stateless
                    // session bean
                    continue;
                String ejbClassName = ejbEndpoint.getClassName();
                List<String> interfaceNames = ejbEndpoint.getLocalBusinessInterfaceNames();
                if (interfaceNames.contains(abstractClassInterfaceName)) {
                    String jndiName = getJNDIName(ejbEndpoint, abstractClassInterfaceName, ejbModuleName);
                    addResourceProvider(moduleMetaData, jndiName, cxfPRHolder, ejbType, abstractClassInterfaceName, ejbClassName, perRequestProviderAndPathInfos,
                                        singletonProviderAndPathInfos);
                }

            }

        }

    }

    @Override
    public boolean isCustomizableBean(Class<?> clazz, Object context) {

        if (context == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, EJBInfo> ejbInfos = (Map<String, EJBInfo>) (context);
        EJBInfo ejbInfo = ejbInfos.get(clazz.getName());
        if (ejbInfo != null) {
            return true;
        } else {
            Iterator<Entry<String, EJBInfo>> iterator = ejbInfos.entrySet().iterator();
            while (iterator.hasNext()) {
                List<String> localInterfaceNameList = iterator.next().getValue().getLocalInterfaceNameList();
                if (localInterfaceNameList.contains(clazz.getName()))
                    return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T onSingletonProviderInit(T provider, Object context, Message m) {
        if (context == null) {
            return null;
        }
        Map<String, EJBInfo> ejbInfos = (Map<String, EJBInfo>) (context);
        EJBInfo ejbInfo = ejbInfos.get(provider.getClass().getName());
        String ejbName = ejbInfo.getEjbName();
        String ejbModuleName = ejbInfo.getEjbModuleName();
        List<String> ejbLocalInterfaces = ejbInfo.getLocalInterfaceNameList();
        EjbProviderProxy providerProxy = new EjbProviderProxy(ServerProviderFactory.getInstance(m).createExceptionMapper(EJBException.class,
                                                                                                                         m) != null, ejbName, ejbLocalInterfaces, ejbModuleName);
        Object returnedObj = providerProxy.createEjbProviderObject(provider);
        if (ejbInfo.getEjbType().equals(EJBType.STATELESS_SESSION)) {
            Tr.warning(tc,
                       "warning.jaxrs.ejb.provider.mismatch",
                       provider.getClass().getName(), "STATELESS", "EJB");
        }
        return (T) ((returnedObj == null) ? provider : returnedObj);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T onSingletonServiceInit(T service, Object context) {

        return service;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T beforeServiceInvoke(T serviceObject, boolean isSingleton,
                                     Object context) {

        return serviceObject;

    }

    @Override
    public Object serviceInvoke(Object serviceObject, Method m,
                                Object[] params, boolean isSingleton, Object context, Message inMessage) throws Exception {

        Map<String, EJBInfo> ejbInfos = (Map<String, EJBInfo>) (context);
        EJBInfo ejbInfo = ejbInfos.get(serviceObject.getClass().getName());
        if (ejbInfo == null) {
            return serviceObject;
        } ;
        String jndiKey = EJBUtils.methodToString(m);
        String jndiName = ejbInfo.getMethodToJNDI().get(jndiKey);
        if (jndiName == null)
            return m.invoke(serviceObject, params);
        Object ejbServiceObject = null;
        // when looking for an EJB, search from cache first. If found, it means the EJB instance was looked up before. Just use it and don't throw warning message.
        //If can't be found in cache, then JNDI lookup this instance and add it to cache, print the warning message only if EJB is singlton.
        ejbServiceObject = ejbInfo.getEjbInstanceCache().get(jndiName);
        if (null == ejbServiceObject) {
            try {
                ejbServiceObject = new InitialContext().lookup(jndiName);
                ejbInfo.getEjbInstanceCache().put(jndiName, ejbServiceObject);
                if (ejbInfo.getEjbType().equals(EJBType.SINGLETON_SESSION) && (!isSingleton)) {
                    Tr.warning(tc,
                               "warning.jaxrs.ejb.resource.mismatch",
                               serviceObject.getClass().getName(), "PERREQUEST", "SINGLETON", "EJB");
                }
                if (ejbInfo.getEjbType().equals(EJBType.STATELESS_SESSION) && (isSingleton)) {
                    Tr.warning(tc,
                               "warning.jaxrs.ejb.resource.mismatch",
                               serviceObject.getClass().getName(), "SINGLETON", "STATELESS", "EJB");
                }
            } catch (NamingException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "Couldn't get instance for "
                                 + serviceObject.getClass().getName()
                                 + " through JNDI: " + jndiName
                                 + ", will use JAX-RS instance.");
                return null;
            }
        }

        if (null != ejbServiceObject) {
            //EJB instance has implemented interface, need to cast into the interface and replace the method object
            if (jndiName.indexOf("!") > 0) {
                Method method = null;
                String interfaceName = jndiName.substring(jndiName.lastIndexOf("!") + 1);
                Class clazzInerface = null;
                clazzInerface = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
                for (Method inteM : clazzInerface.getMethods()) {
                    if (EJBUtils.matchMethod(inteM, m)) {
                        method = inteM;
                        break;
                    }
                }
                if (method != null) {
                    try {
                        return method.invoke(clazzInerface.cast(ejbServiceObject), params);
                    } catch (Exception e) {
                        List<Class<?>> exceptionTypes = Arrays.asList(m.getExceptionTypes());
                        boolean ejbExceptionMapped = ServerProviderFactory.getInstance(inMessage).createExceptionMapper(EJBException.class, inMessage) != null;//this.exceptionMappers.contains(javax.ejb.EJBException.class);
                        Throwable causeException = e.getCause();
                        Class<? extends Throwable> exceptionClass = causeException.getClass();
                        if (ejbExceptionMapped) {
                            if (EJBException.class.equals(exceptionClass)) {
                                Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                                if (causedByException != null && exceptionTypes.contains(causedByException.getClass()))
                                    throw causedByException;
                            }
                        } else {
                            if (EJBException.class.isAssignableFrom(exceptionClass)) {
                                Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                                if (causedByException != null)
                                    throw causedByException;
                            }
                        }
                        if (e instanceof InvocationTargetException)
                            throw (Exception) causeException;
                        else
                            throw e;
                    }
                } else
                    return null;
            } else {
                try {
                    //EJB instance does't implement any interface. Invoke the original method with the ejbObject
                    return m.invoke(ejbServiceObject, params);
                } catch (Exception e) {
                    List<Class<?>> exceptionTypes = Arrays.asList(m.getExceptionTypes());
                    boolean ejbExceptionMapped = ServerProviderFactory.getInstance(inMessage).createExceptionMapper(EJBException.class, inMessage) != null;//this.exceptionMappers.contains(javax.ejb.EJBException.class);
                    Throwable causeException = e.getCause();
                    Class<? extends Throwable> exceptionClass = causeException.getClass();
                    if (ejbExceptionMapped) {
                        if (EJBException.class.equals(exceptionClass)) {
                            Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                            if (causedByException != null && exceptionTypes.contains(causedByException.getClass()))
                                throw causedByException;
                        }
                    } else {
                        if (EJBException.class.isAssignableFrom(exceptionClass)) {
                            Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                            if (causedByException != null)
                                throw causedByException;
                        }
                    }
                    if (e instanceof InvocationTargetException)
                        throw (Exception) causeException;
                    else
                        throw e;
                }
            }
        }

        else
            return null;
    }

    @Override
    public void afterServiceInvoke(Object serviceObject, boolean isSingleton,
                                   Object context) {
        //doing nothing
    }

    private String getJNDIName(EJBEndpoint ejbEndpoint, String interfaceName, String ejbModuleName) {

        /**
         * F138708: we need consider 2 cases:
         * 1)EJB jaxrs in war: JNDI lookup format is java:module/<beanName>[!<interface>]
         * 2)EJB jaxrs in ejb jar: JNDI lookup format is java:app/<ejbmodulename>/<beanName>[!<interface>]
         */
        String beanName = ejbEndpoint.getName();
        StringBuffer jndiName = (ejbModuleName == null) ? new StringBuffer("java:module/").append(beanName) : new StringBuffer("java:app/").append(ejbModuleName
                                                                                                                                                   + "/").append(beanName);
        if ((interfaceName != null) && (!(interfaceName.trim().equals(""))))
            jndiName.append("!").append(interfaceName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "jndi name is" + jndiName.toString());
        }
        return jndiName.toString();
    }

    private void findEJBClass(List<EJBEndpoint> ejbEndpointList,
                              Iterator<ProviderResourceInfo> providerResourceInfoIterator,
                              Boolean perRequest) {
        while (providerResourceInfoIterator.hasNext()) {
            ProviderResourceInfo providerResourceInfo = providerResourceInfoIterator.next();
            if (providerResourceInfo.getRuntimeType() != RuntimeType.POJO)
                // RuntimeType is POJO, means we need to handle as ejb, othewise, it has already been
                // handled by CDI
                continue;
            String className = providerResourceInfo.getClassName();
            for (int i = 0; i < ejbEndpointList.size(); i++) {
                EJBEndpoint ejbEndpoint = ejbEndpointList.get(i);
                EJBType ejbType = ejbEndpoint.getEJBType();
                if (!(ejbType.equals(EJBType.SINGLETON_SESSION) || ejbType.equals(EJBType.STATELESS_SESSION)))
                    // jaxrs only handle singleton session bean or stateless
                    // session bean
                    continue;
                String ejbClassName = ejbEndpoint.getClassName();
                List<String> interfaceNames = ejbEndpoint.getLocalBusinessInterfaceNames();
                if (ejbClassName.equals(className)) {
                    providerResourceInfo.setRuntimeType(RuntimeType.EJB);
                    providerResourceInfo.putCustomizedProperty(JaxRsEJBConstants.EJB_TYPE, ejbType);
                    ejbEndpoint.getLocalBusinessInterfaceNames();
                    resetCXFHolder(providerResourceInfo, cxfPRHolder, perRequest, ejbType, className);
                } else if (interfaceNames.contains(className)) {
                    providerResourceInfo.setRuntimeType(RuntimeType.EJB);
                    providerResourceInfo.putCustomizedProperty(JaxRsEJBConstants.EJB_TYPE, ejbType);
                    resetCXFHolder(providerResourceInfo, cxfPRHolder, perRequest, ejbType, className);
                }

            }
        }
    }

    @Override
    public Application onApplicationInit(Application app,
                                         JaxRsModuleMetaData metaData) {
        String appClassName = app.getClass().getName();
        if (appClassName.equals(JaxRsServerConstants.APPLICATION_ROOT_CLASS_NAME))
            return app;
        //if an Application class implemments any interface, then it will not be replaced no matter if it's ejb bean.
        if (app.getClass().getInterfaces().length > 0)
            return app;

        Application ejbInstance = null;

        /**
         * F138708: we should check if this is a EJB module,
         */
        String ejbModuleName = null;
        try {
            if (JaxRsUtils.isEJBModule(metaData.getModuleContainer())) {
                ejbModuleName = metaData.getModuleInfo().getName();
            }
        } catch (UnableToAdaptException e2) {
            //ignore
        }

        EJBEndpoints ejbEndpoints;
        try {
            ejbEndpoints = metaData.getModuleContainer().adapt(
                                                               EJBEndpoints.class);

            List<EJBEndpoint> ejbEndpointList = new ArrayList<EJBEndpoint>();
            ejbEndpointList.addAll(ejbEndpoints.getEJBEndpoints()); // ejbEndpoints.getEJBEndpoints()
            for (int i = 0; i < ejbEndpointList.size(); i++) {
                EJBEndpoint ejbEndpoint = ejbEndpointList.get(i);
                EJBType ejbType = ejbEndpoint.getEJBType();
                if (ejbType != EJBType.SINGLETON_SESSION
                    && ejbType != EJBType.STATELESS_SESSION)
                    continue;
                String ejbClassName = ejbEndpoint.getClassName();
//                List<String> interfaceNames = ejbEndpoint
//                                .getLocalBusinessInterfaceNames();
                String jndiName = null;
                if (appClassName.equals(ejbClassName)) {
                    jndiName = getJNDIName(ejbEndpoint, null, ejbModuleName);
                    ejbInstance = (Application) (new InitialContext().lookup(jndiName));
                    break;
                }

            }
        } catch (UnableToAdaptException e1) {
            return app;
        } catch (NamingException e) {
            return app;
        }
        return (ejbInstance == null ? app : (Application) ejbInstance);
    }

    @FFDCIgnore(value = { IllegalAccessException.class, InstantiationException.class })
    private void resetCXFHolder(ProviderResourceInfo providerResourceInfo, CXFJaxRsProviderResourceHolder cxfPRHolder,
                                Boolean perRequest,
                                EJBType ejbType,
                                String className) {
        if (!providerResourceInfo.isJaxRsProvider()) {
            if (ejbType.equals(EJBType.SINGLETON_SESSION) && perRequest) //this means EJB is singleton, we need to modidy CXFPRHolder
            {
                try {
                    Object po = providerResourceInfo.getProviderResourceClass().newInstance();
                    providerResourceInfo.setObject(po);
                    cxfPRHolder.removeResouceProvider(providerResourceInfo.getProviderResourceClass());//remove from original ResourceProvider map and re-add the new one.
                    cxfPRHolder.addResouceProvider(providerResourceInfo.getProviderResourceClass(), new SingletonResourceProvider(po));

                } catch (IllegalAccessException e) {
                    Tr.warning(tc,
                               "warning.failed.instantiate.ejb.instance",
                               providerResourceInfo.getProviderResourceClass().getName());
                    providerResourceInfo.setRuntimeType(RuntimeType.POJO);
                    providerResourceInfo.removeCustomizedProperty(
                                                                  JaxRsEJBConstants.EJB_TYPE);

                } catch (InstantiationException e) {
                    Tr.warning(tc,
                               "warning.failed.instantiate.ejb.instance",
                               providerResourceInfo.getProviderResourceClass().getName());
                    providerResourceInfo.setRuntimeType(RuntimeType.POJO);
                    providerResourceInfo.removeCustomizedProperty(
                                                                  JaxRsEJBConstants.EJB_TYPE);
                }
            }
            if (ejbType.equals(EJBType.STATELESS_SESSION) && !perRequest) // this means EJB is per-request, we need to modify CXFPRHolder as well
            {

                cxfPRHolder.removeResouceProvider(providerResourceInfo.getProviderResourceClass());//remove from original ResourceProvider map and re-add the new one.
                cxfPRHolder.addResouceProvider(providerResourceInfo.getProviderResourceClass(),
                                               new PerRequestResourceProvider(providerResourceInfo.getProviderResourceClass()));
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    @FFDCIgnore(value = { IllegalAccessException.class, InstantiationException.class })
    private void addResourceProvider(JaxRsModuleMetaData moduleMetaData, String jndiName, CXFJaxRsProviderResourceHolder cxfPRHolder, EJBType ejbType,
                                     String abstractClassInterfaceName,
                                     String ejbClassName,
                                     Set<ProviderResourceInfo> perRequestProviderAndPathInfos, Set<ProviderResourceInfo> singletonProviderAndPathInfos) {
        try {
            Class<?> ejbClazz = moduleMetaData.getAppContextClassLoader().loadClass(ejbClassName);
            Class<?> abstractClassInterfaceClazz = moduleMetaData.getAppContextClassLoader().loadClass(abstractClassInterfaceName);
            if (isValidResource(abstractClassInterfaceClazz)) {
                if (ejbType.equals(EJBType.SINGLETON_SESSION)) //this means EJB is singleton, we need to modidy CXFPRHolder
                {
                    Object po = ejbClazz.newInstance();
                    cxfPRHolder.addResourceClasses(abstractClassInterfaceClazz);
                    cxfPRHolder.addAbstractResourceMapItem(abstractClassInterfaceClazz, ejbClazz);
                    cxfPRHolder.addResouceProvider(abstractClassInterfaceClazz, new SingletonResourceProvider(po));
                    ProviderResourceInfo providerResourceInfo = new ProviderResourceInfo(po, true, false);
                    providerResourceInfo.setRuntimeType(RuntimeType.EJB);
                    providerResourceInfo.putCustomizedProperty(JaxRsEJBConstants.EJB_TYPE, ejbType);
                    singletonProviderAndPathInfos.add(providerResourceInfo);
                }
                if (ejbType.equals(EJBType.STATELESS_SESSION)) // this means EJB is per-request, we need to modify CXFPRHolder as well
                {
                    cxfPRHolder.addResourceClasses(abstractClassInterfaceClazz);
                    cxfPRHolder.addAbstractResourceMapItem(abstractClassInterfaceClazz, ejbClazz);
                    cxfPRHolder.addResouceProvider(abstractClassInterfaceClazz, new PerRequestResourceProvider(ejbClazz));
                    ProviderResourceInfo providerResourceInfo = new ProviderResourceInfo(ejbClazz, true, false);
                    providerResourceInfo.setRuntimeType(RuntimeType.EJB);
                    providerResourceInfo.putCustomizedProperty(JaxRsEJBConstants.EJB_TYPE, ejbType);
                    perRequestProviderAndPathInfos.add(providerResourceInfo);
                }
            }
            if (isValidProvider(abstractClassInterfaceClazz)) {
                Object po = ejbClazz.newInstance();
                cxfPRHolder.addProvider(po);
                ProviderResourceInfo providerResourceInfo;
                if (isValidResource(ejbClazz))
                    providerResourceInfo = new ProviderResourceInfo(po, true, true);
                else
                    providerResourceInfo = new ProviderResourceInfo(po, true, false);
                providerResourceInfo.setRuntimeType(RuntimeType.EJB);
                providerResourceInfo.putCustomizedProperty(JaxRsEJBConstants.EJB_TYPE, ejbType);
                singletonProviderAndPathInfos.add(providerResourceInfo);
            }
        } catch (ClassNotFoundException e1) {
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        }

    }

    public static boolean isValidResource(Class<?> c) {
        if (c == null || c == Object.class) {
            return false;
        }
        if (c.getAnnotation(Path.class) != null) {
            return true;
        }

        if (c.getInterfaces() != null) {
            for (Class<?> ci : c.getInterfaces()) {
                if (isValidResource(ci))
                    return true;
            }
        }

        return isValidResource(c.getSuperclass());
    }

    public static boolean isValidProvider(Class<?> c) {
        if (c == null || c == Object.class) {
            return false;
        }
        if (c.getAnnotation(Provider.class) != null) {
            return true;
        }
        for (Class<?> itf : c.getInterfaces()) {
            if (SERVER_PROVIDER_CLASS_NAMES.contains(itf.getName())) {
                return true;
            }
        }
        return isValidProvider(c.getSuperclass());
    }

    @Override
    public <T> T onSetupProviderProxy(T provider, Object contextObject) {

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#destroyApplicationScopeResources(com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData)
     */
    @Override
    public void destroyApplicationScopeResources(JaxRsModuleMetaData jaxRsModuleMetaData) {
        // TODO Auto-generated method stub

    }

}