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
package com.ibm.ws.jaxrs20.injection;

import static com.ibm.ws.jaxrs20.utils.CustomizerUtils.createCustomizerKey;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;

import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer.BeanCustomizerContext;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;

public abstract class InjectionRuntimeContextHelper {

    private static ThreadLocal<InjectionRuntimeContext> threadLocal = new ThreadLocal<InjectionRuntimeContext>() {

        @Override
        protected InjectionRuntimeContext initialValue() {

            InjectionRuntimeContext irc = new InjectionRuntimeContext();
            InjectionRuntimeContextHelper.setRuntimeContext(irc);
            return irc;

        }
    };

    /**
     * For internal usage only
     */
    public static InjectionRuntimeContext getRuntimeContext() {
        return threadLocal.get();

    }

    /**
     * For internal usage only
     */
    public static void setRuntimeContext(InjectionRuntimeContext runtimeContext) {
        threadLocal.set(runtimeContext);
    }

    /**
     * For internal usage only
     */
    public static void removeRuntimeContext() {
        threadLocal.remove();
    }

    public static boolean isEJBCDI(Class<?> c, Bus bus) {
        List<JaxRsFactoryBeanCustomizer> beanCustomizers = (List<JaxRsFactoryBeanCustomizer>) bus.getProperty(JaxRsConstants.ENDPOINT_LIST_BEANCUSTOMIZER);
        Map<String, BeanCustomizerContext> beanCustomizerContexts = (Map<String, BeanCustomizerContext>) bus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);

        if (beanCustomizers == null || beanCustomizers.isEmpty() || beanCustomizerContexts == null) {
            return false;
        }

        for (JaxRsFactoryBeanCustomizer beanCustomizer : beanCustomizers) {
            if (beanCustomizer.isCustomizableBean(c, beanCustomizerContexts.get(createCustomizerKey(beanCustomizer)))) {
                return true;
            }
        }

        return false;
    }

    public static JaxRsFactoryBeanCustomizer findBeanCustomizer(Class<?> c, Bus bus) {

        List<JaxRsFactoryBeanCustomizer> beanCustomizers = (List<JaxRsFactoryBeanCustomizer>) bus.getProperty(JaxRsConstants.ENDPOINT_LIST_BEANCUSTOMIZER);
        Map<String, BeanCustomizerContext> beanCustomizerContexts = (Map<String, BeanCustomizerContext>) bus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);

        if (beanCustomizers == null || beanCustomizers.isEmpty() || beanCustomizerContexts == null) {
            return null;
        }

        JaxRsFactoryBeanCustomizer rtn = null;
        for (JaxRsFactoryBeanCustomizer beanCustomizer : beanCustomizers) {
            if (beanCustomizer.isCustomizableBean(c, beanCustomizerContexts.get(createCustomizerKey(beanCustomizer)))) {
                return beanCustomizer;
            }
        }

        return rtn;

    }

    public static Object getBeanCustomizerContext(JaxRsFactoryBeanCustomizer beanCustomizer, Bus bus)
    {
        Map<String, BeanCustomizerContext> beanCustomizerContexts = (Map<String, BeanCustomizerContext>) bus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);
        return beanCustomizerContexts.get(createCustomizerKey(beanCustomizer));
    }

    /**
     * RuntimeContextTLS can init the EJB or CDI provider
     *
     * @param <T>
     *
     * @param <T>
     *
     * @param resource
     * @param message
     */
    public static <T> void initSingletonEJBCDIProvider(AbstractResourceInfo resource, Message message, T resourceObject) {

        /*
         * Only @Provider will use logic here.
         * As EJB can only be default constructor, CDI can be default constructor or 1 param with @Inject constructor
         * And a provider must have 1 public constructor(default constructor or @Context params constructor)
         * then the intersection is only default constructor.
         * so if ConstructorProxies!=null means there are some other invalid constructors to EJB or CDI container
         */
        if ((!(resource instanceof ProviderInfo)) || (resource instanceof ApplicationInfo) || resource.getConstructorProxies() != null) {
            return;
        }

        ProviderInfo<T> pi = (ProviderInfo<T>) resource;

        /**
         * if this provider is inited which means it may be replaced by EJB or CDI bean or it may be invoked with @PostConstrcut
         */
        if (pi.isInit()) {
            return;
        }

        synchronized(pi) {
            if (pi.isInit()) {
                return;
            }
            Class clz = pi.getProvider().getClass();
            Bus bus = resource.getBus();

            if (bus != null) { // if bus is null, then we don't need to do injection on this provider
                 List<JaxRsFactoryBeanCustomizer> beanCustomizers = (List<JaxRsFactoryBeanCustomizer>) bus.getProperty(JaxRsConstants.ENDPOINT_LIST_BEANCUSTOMIZER);
                  Map<String, Object> beanCustomizerContexts = (Map<String, Object>) bus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);

                  if (beanCustomizers != null && !beanCustomizers.isEmpty() && beanCustomizerContexts != null) {

                      Object newProviderInstance = null;
                      for (JaxRsFactoryBeanCustomizer beanCustomizer : beanCustomizers) {
                          Object context = beanCustomizerContexts.get(createCustomizerKey(beanCustomizer));
                          if (beanCustomizer.isCustomizableBean(clz, context)) {

                              newProviderInstance = beanCustomizer.onSingletonProviderInit(pi.getProvider(), context, message);

                              /**
                               * if newProviderInstance!= the original object, which means it is replaced to EJB or CDI, so just return
                               * no need call @PostConstruct
                               */
                              if (newProviderInstance != null) {
                                  //call setProvider to set isInit==true
                                  pi.setProvider(newProviderInstance);
                                  pi.setIsInit(true);
                                  return;
                              }
                        }
                    }
                }
                //No replacement happens which means this is a POJO, call @PostConstruct
                pi.setIsInit(true);
                Method postConstructMethod = ResourceUtils.findPostConstructMethod(clz);
                InjectionUtils.invokeLifeCycleMethod(pi.getProvider(), postConstructMethod);
            }
        }
    }

//    /**
//     * RuntimeContextTLS can init the EJB or CDI provider
//     *
//     * @param <T>
//     *
//     * @param <T>
//     *
//     * @param resource
//     * @param message
//     */
//    public static <T> T initSingletonEJBCDIResource(AbstractResourceInfo resource, Message message, T resourceObject) {
//
//        /*
//         * Only @Provider will use logic here.
//         * As EJB can only be default constructor, CDI can be default constructor or 1 param with @Inject constructor
//         * And a provider must have 1 public constructor(default constructor or @Context params constructor)
//         * then the intersection is only default constructor.
//         * so if ConstructorProxies!=null means there are some other invalid constructors to EJB or CDI container
//         */
//        if ((!(resource instanceof ClassResourceInfo)) || (resource instanceof ApplicationInfo) || resource.getConstructorProxies() != null) {
//            return null;
//        }
//        if (!resource.isSingleton())
//        {
//            return null;
//        }
//
//        ClassResourceInfo res = (ClassResourceInfo) resource;
//
//        /**
//         * if this provider is inited which means it may be replaced by EJB or CDI bean or it may be invoked with @PostConstrcut
//         */
//        if (((SingletonResourceProvider) res.getResourceProvider()).isInit()) {
//            return null;
//        }
//
//        boolean isReplaced = false;
//
//        JaxRsFactoryBeanCustomizer beanCustomizer = InjectionRuntimeContextHelper.findBeanCustomizer(resourceObject.getClass(), resource.getBus());
//
//        if (beanCustomizer == null) {
//            return null;
//        }
//        T o = beanCustomizer.onSingletonServiceInit(resourceObject,
//                                                    InjectionRuntimeContextHelper.getBeanCustomizerContext(beanCustomizer, resource.getBus()));
//        if (resourceObject != o)
//        {
//            isReplaced = true;
//        }
//        resourceObject = o;
//        SingletonResourceProvider sp = new SingletonResourceProvider(o, false, true);
//        ((ClassResourceInfo) resource).setResourceProvider(sp);
//
//        //No replacement happens which means this is a POJO, call @PostConstruct
//        if (isReplaced == false) {
//            Method postConstructMethod = ResourceUtils.findPostConstructMethod(resourceObject.getClass());
//            InjectionUtils.invokeLifeCycleMethod(resourceObject, postConstructMethod);
//        }
//        return resourceObject;
//    }
}
