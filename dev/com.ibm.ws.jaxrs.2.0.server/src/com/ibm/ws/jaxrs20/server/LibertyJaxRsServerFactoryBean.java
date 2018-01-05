/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server;

import static com.ibm.ws.jaxrs20.utils.CustomizerUtils.createCustomizerKey;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer.BeanCustomizerContext;
import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;
import com.ibm.ws.jaxrs20.injection.LibertyClearInjectRuntimeCtxOutInterceptor;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.ws.jaxrs20.metadata.CXFJaxRsProviderResourceHolder;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo;
import com.ibm.ws.jaxrs20.server.component.JaxRsBeanValidation;

/**
 *
 */
public class LibertyJaxRsServerFactoryBean extends JAXRSServerFactoryBean {

    private final static TraceComponent tc = Tr.register(LibertyJaxRsServerFactoryBean.class);

    private final List<JaxRsFactoryBeanCustomizer> beanCustomizers = new LinkedList<JaxRsFactoryBeanCustomizer>();
//    private final Map<String, Object> beanCustomizerContexts = new HashMap<String, Object>();
    private Map<String, Object> beanCustomizerContexts;
    private final EndpointInfo endpointInfo;
    private final JaxRsModuleMetaData moduleMetadata;
    private ServletConfig servletConfig;
    private JaxRsProviderFactoryService providerFactoryService;

    private final static Integer lockObject = new Integer(0);

    /**
     * @param jaxRsModuleMetaData
     * @param endpointInfo
     * @param originalBeanCustomizers
     */
    public LibertyJaxRsServerFactoryBean(EndpointInfo endpointInfo,
                                         JaxRsModuleMetaData moduleMetaData,
                                         Set<JaxRsFactoryBeanCustomizer> originalBeanCustomizers,
                                         ServletConfig servletConfig, JaxRsProviderFactoryService providerFactoryService) {

        this.endpointInfo = endpointInfo;
        this.moduleMetadata = moduleMetaData;
        this.servletConfig = servletConfig;
        this.providerFactoryService = providerFactoryService;

        //set server bus for the endpoint
        Bus serverBus = this.moduleMetadata.getServerMetaData().getServerBus();
        this.setBus(serverBus);

        //Get the beanCustomizerContexts from the bus or create a new map
        this.beanCustomizerContexts = (Map<String, Object>) serverBus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);
        if (this.beanCustomizerContexts == null) {
            this.beanCustomizerContexts = new HashMap<String, Object>();
        }


        beanCustomizers.addAll(originalBeanCustomizers);
        Collections.sort(beanCustomizers, new Comparator<JaxRsFactoryBeanCustomizer>() {
            @Override
            public int compare(JaxRsFactoryBeanCustomizer o1, JaxRsFactoryBeanCustomizer o2) {
                return o1.getPriority().compareTo(o2.getPriority());
            }
        });
    }

    /**
     * Inject ThreadLocal proxy into Application if there is context injection:
     * Please be aware doesn't inject the application itself.
     *
     * 9.2.1 Application
     * The instance of the application-supplied Application subclass can be injected into a class field or method
     * parameter using the @Context annotation. Access to the Application subclass instance allows configuration
     * information to be centralized in that class. Note that this cannot be injected into the Application
     * subclass itself since this would create a circular dependency.
     *
     * @param app
     */
    void injectContextApplication(Application app) {
        ApplicationInfo appinfo = new ApplicationInfo(app, this.getBus());
        if (appinfo.contextsAvailable()) {
            InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();
            for (Map.Entry<Class<?>, Method> entry : appinfo.getContextMethods().entrySet()) {
                Method method = entry.getValue();
                Object value = method.getParameterTypes()[0] == Application.class ? null : appinfo.getContextSetterProxy(method);

                irc.setRuntimeCtxObject(entry.getKey().getName(),
                                        value);
                InjectionUtils.injectThroughMethod(app, method, value);
            }

            //FIXME: what if we have a method and field with same name?
            for (Field f : appinfo.getContextFields()) {
                Object value = f.getType() == Application.class ? null : appinfo.getContextFieldProxy(f);
                irc.setRuntimeCtxObject(f.getType().getName(), value);
                InjectionUtils.injectFieldValue(f, app, value);
            }
        }
    }

    private void onApplicationInit(CXFJaxRsProviderResourceHolder cxfPRHolder) throws ServletException, ClassNotFoundException {

        /**
         * step 1: load Application class and create the POJO Application object
         */
        Class<?> appClass = this.moduleMetadata.getAppContextClassLoader().loadClass(endpointInfo.getAppClassName());
        Application app = (Application) createSingletonInstance(appClass, servletConfig);

        injectContextApplication(app);

        /**
         * step 2: Application can be init only once, the init order depends on the priority of beanCustomizer
         * If CDI init the app first, the EJB should not, then jaxrs process continue.
         * If EJB init the app first, the CDI should not, then jaxrs process continue.
         * If EJB and CDI don't init app, the original app should be used as POJO.
         */

        boolean replaced = false;
        Application customizedApp = null;
        for (JaxRsFactoryBeanCustomizer customizer : beanCustomizers) {
            customizedApp = customizer.onApplicationInit(app, moduleMetadata);
            if ((customizedApp != null) && (customizedApp != app)) {
                app = customizedApp;
                replaced = true;
                endpointInfo.setCustomizedApp(true);
                break;
            }
        }
        //call postConstruct for Application if it has not been replaced in CDI/EJB
        if (!replaced) {

            Method postConstructMethod = ResourceUtils.findPostConstructMethod(app.getClass());
            InjectionUtils.invokeLifeCycleMethod(app, postConstructMethod);

        }

        /**
         * step 3: gather the provider & resources which belong to one application
         */
        // With the change to have servlet initialization occur in the background we are seeing intermittent FAT failures where resources go missing
        // Adding sync here to prevent the adding of resource classes at the same time in different threads
        synchronized (lockObject) {
            updateEndpointInfo(endpointInfo, app, moduleMetadata, cxfPRHolder);
        }

        /**
         * step 4: set properties to Application object
         */
        Map<String, Object> appProps = app.getProperties();
        if (appProps != null) {
            this.getProperties(true).putAll(appProps);
        }

        /**
         * step 5: set Application object to ServerFactoryBean
         */
        super.setApplication(app);
        endpointInfo.setApp(app);
    }

    protected Object createSingletonInstance(Class<?> cls, ServletConfig sc) throws ServletException {
        Constructor<?> c = ResourceUtils.findResourceConstructor(cls, false);
        if (c == null) {
            throw new ServletException("No valid constructor found for " + cls.getName());
        }
        boolean isDefault = c.getParameterTypes().length == 0;
        if (!isDefault && (c.getParameterTypes().length != 1
                           || c.getParameterTypes()[0] != ServletConfig.class
                              && c.getParameterTypes()[0] != ServletContext.class)) {
            throw new ServletException("Resource classes with singleton scope can only have "
                                       + "ServletConfig or ServletContext instances injected through their constructors");
        }
        Object[] values = isDefault ? new Object[] {} : new Object[] { c.getParameterTypes()[0] == ServletConfig.class ? sc : sc.getServletContext() };
        try {
            Object instance = c.newInstance(values);
            return instance;
        } catch (InstantiationException ex) {

            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated");
        } catch (IllegalAccessException ex) {

            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated due to IllegalAccessException");
        } catch (InvocationTargetException ex) {

            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated due to InvocationTargetException");
        }
    }

    protected synchronized void doInit() throws ServletException {

        /**
         * commonBundlerClassLoader for classes under package org.apache.cxf.jaxrs.validation
         * if no Bean Validation feature, no reason to import package org.apache.cxf.jaxrs.validation or javax.validation.*
         */

        //step 1: set address, delay start
        this.setAddress(endpointInfo.getAddress());
        this.setStart(false);
        /**
         * put the bean customizers list & context obj on bus, then we can get it anywhere in CXF
         */
        this.getBus().setProperty(JaxRsConstants.ENDPOINT_LIST_BEANCUSTOMIZER, beanCustomizers);
        this.getBus().setProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ, beanCustomizerContexts);

        /**
         * Allow the ProviderFactory to cache provider information for MessageBodyReaders and Writers
         */
        this.getBus().setProperty(JaxRsConstants.PROVIDER_CACHE_ALLOWED, true);
        this.getBus().setProperty(JaxRsConstants.PROVIDER_CACHE_CHECK_ALL, true);

        /**
         * add LibertyClearInjectRuntimeCtxOutInterceptor to clear InjectionRuntimeContext from thread local
         */
        this.getBus().getOutInterceptors().add(new LibertyClearInjectRuntimeCtxOutInterceptor<Message>(Phase.MARSHAL));

        //step 2: init application object
        CXFJaxRsProviderResourceHolder cxfPRHolder = new CXFJaxRsProviderResourceHolder();
        try {
            this.onApplicationInit(cxfPRHolder);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Class not found:" + e.getMessage());
            }
            throw new ServletException(e.getMessage(), e);

        } catch (ServletException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Servlet Exception:" + e.getMessage());
            }
            throw e;
        }

        if (endpointInfo.getPerRequestProviderAndPathInfos().isEmpty() && endpointInfo.getSingletonProviderAndPathInfos().isEmpty()
            && endpointInfo.getAbstractClassInterfaceList().isEmpty()) {
            throw new ServletException("At least one provider or resource class should be specified for application class \"" + endpointInfo.getAppClassName());
        }

        //step 3: prepare the EJB or CDI provider & resources to EJB or CDI's beanCustomizer
        for (JaxRsFactoryBeanCustomizer customizer : beanCustomizers) {
            String key = createCustomizerKey(customizer);
            Object oldCustomizerContext = beanCustomizerContexts.get(key);
            BeanCustomizerContext context = new BeanCustomizerContext(endpointInfo, moduleMetadata, cxfPRHolder);

            if (key != null && (oldCustomizerContext instanceof HashMap)) {
                context.setContextObject(oldCustomizerContext);
            }
            customizer.onPrepareProviderResource(context);

            Object customizerContext = context.getContextObject();
            if (customizerContext != null) {
                beanCustomizerContexts.put(createCustomizerKey(customizer), customizerContext);
            }
        }

        //step 4: check if BeanValidation required
        boolean isEnableBeanValidation = JaxRsBeanValidation.enableBeanValidationProviders(cxfPRHolder.getProviders());

        //step 5: using LibertyJaxRsInvoker
        this.setInvoker(new LibertyJaxRsInvoker(this, isEnableBeanValidation));

        //step 6: bind all default providers to endpoint
        boolean clientSide = false;
        providerFactoryService.bindProviders(clientSide, cxfPRHolder.getProviders());

        //step 7: set resource & providers to CXF
        //note: currently all providers & resources singletons are still from end users' getSingletons()
        //the replacement will be done on JaxRsFactoryBeanCustomizer.onSingletonProviderInit & JaxRsFactoryBeanCustomizer.onSingletonServiceInit
        List<Class<?>> resourceClasses = cxfPRHolder.getResourceClasses();
        synchronized (resourceClasses) {
            // we must sync here to avoid thread safety issues as the setResourceClasses method
            // will iterate over this this:
            this.setResourceClasses(resourceClasses);
        }

        List<Object> providersList = cxfPRHolder.getProviders();
        synchronized (providersList) {
            this.setProviders(providersList);
        }

        Map<Class<?>, ResourceProvider> resourceProviderMap = cxfPRHolder.getResouceProviderMap();
        synchronized (resourceProviderMap) {
            for (Map.Entry<Class<?>, ResourceProvider> entry : resourceProviderMap.entrySet()) {
                this.setResourceProvider(entry.getKey(), entry.getValue());
            }
        }

        /**
         * add abstract interface class & its implementation class
         */
        Map<Class<?>, Class<?>> absMap = cxfPRHolder.getAbstractResourceMap();
        synchronized (absMap) {
            for (ClassResourceInfo cri : this.serviceFactory.getClassResourceInfo()) {
                if (absMap.containsKey(cri.getServiceClass())) {
                    cri.setResourceClass(absMap.get(cri.getServiceClass()));
                }
            }
        }
        // Show what the new List of Resource Classes looks like

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            boolean serviceWasNull = false;
            Service service = this.serviceFactory.getService();
            if (service == null) {
                service = serviceFactory.create();
                serviceWasNull = true;
            }
            List<ClassResourceInfo> resources = ((JAXRSServiceImpl) service).getClassResourceInfos();
            StringBuilder stringBuilder = new StringBuilder();
            for (ClassResourceInfo classResourceInfo : resources) {
                stringBuilder.append(classResourceInfo.getResourceClass());
                stringBuilder.append(" , ");
            }
            String stringClassResourceInfo = stringBuilder.toString();
            if (serviceWasNull) {
                this.serviceFactory.setService(null);
            }
            Tr.debug(tc, "All known ClassResourceInfo: " + stringClassResourceInfo);
        }
    }

    private void updateEndpointInfo(EndpointInfo endpointInfo, Application app, JaxRsModuleMetaData moduleMetaData, CXFJaxRsProviderResourceHolder cxfPRHolder) {
        Set<ProviderResourceInfo> perRequestProviderAndPathInfos = endpointInfo.getPerRequestProviderAndPathInfos();
        Set<ProviderResourceInfo> singletonProviderAndPathInfos = endpointInfo.getSingletonProviderAndPathInfos();

        Set<String> scannedClassNames = endpointInfo.getProviderAndPathClassNames();

        perRequestProviderAndPathInfos.clear();
        singletonProviderAndPathInfos.clear();

        Set<Object> appSingletons = app.getSingletons();
        Set<Class<?>> appClasses = app.getClasses();

        if (((appSingletons == null) || (appSingletons.isEmpty()))
            && ((appClasses == null) || (appClasses.isEmpty()))) {
            /**
             * Need to use the scanned instances.
             * !Note: assume there is no redundant class in the scanned classes, so there is no class conflict detection
             */
            for (String className : scannedClassNames) {
                try {
                    Class<?> clazz = moduleMetaData.getAppContextClassLoader().loadClass(className);

                    boolean isJaxRsProvider = false;
                    Object singletonProviderInstance = null;
                    //note: as the abstract class can't be used to initialize instance, then it can't be provider or resource
                    if (ResourceUtils.isNotAbstractClass(clazz)) {

                        /**
                         * check if the class is a valid provider
                         */
                        if (ResourceUtils.isValidProvider(clazz)) {
                            isJaxRsProvider = true;
                            try {
                                Constructor<?> c = ResourceUtils.findResourceConstructor(clazz, false);
                                if (c == null) {
                                    // CWWKW0100W: The JAX-RS Provider class, {0} in the application contains no public constructor. The server will ignore this provider.
                                    Tr.warning(tc, "warn.provider.no.public.ctor", clazz.getName());
                                    continue;
                                }
                                if (c.getParameterTypes().length == 0) {
                                    singletonProviderInstance = c.newInstance();
                                    cxfPRHolder.addProvider(singletonProviderInstance);
                                } else {
                                    cxfPRHolder.addProvider(c);
                                }

                            } catch (Throwable ex) {
                                throw new RuntimeException("Provider " + clazz.getName() + " can not be created", ex);
                            }
                        }

                        ResourceProvider pr = null;
                        /**
                         * check if the class is a valid resource (class with @Path)
                         * in case of the registration to some invalid providers
                         */
                        if (!ResourceUtils.isValidResource(clazz)) {

                            if (isJaxRsProvider == true) {
                                if (null != singletonProviderInstance) {
                                    singletonProviderAndPathInfos.add(new ProviderResourceInfo(singletonProviderInstance, false, isJaxRsProvider));
                                } else {
                                    perRequestProviderAndPathInfos.add(new ProviderResourceInfo(clazz, false, isJaxRsProvider));
                                }
                            }

                            continue;
                        }

                        /**
                         * when a class is declared in @Provider + @Path,
                         * if the resource's constructor has params, the resource will be handled as singleton object with provider
                         */

                        if (isJaxRsProvider == true && null != singletonProviderInstance) {
                            pr = new SingletonResourceProvider(singletonProviderInstance);
                            singletonProviderAndPathInfos.add(new ProviderResourceInfo(singletonProviderInstance, true, isJaxRsProvider));
                        } else {
                            pr = new PerRequestResourceProvider(clazz);
                            perRequestProviderAndPathInfos.add(new ProviderResourceInfo(clazz, true, isJaxRsProvider));
                        }

                        /**
                         * double check: in case if the same class is added in getSingletons loop, ignore getClasses loop
                         */
                        if (cxfPRHolder.addResouceProvider(clazz, pr) == false) {
                            cxfPRHolder.addResourceClasses(clazz);
                        }
                    } else {
                        endpointInfo.getAbstractClassInterfaceList().add(className);
                    }

                } catch (ClassNotFoundException e) {
                }
            }
        } else {

            /**
             * for getClasses & getSingletons, the same class should be only processed once:
             * 1) redundant class in getClasses
             * 2) redundant object in getSingletons
             * 3) redundant class & object of the same class in getClasses & getSingletons
             */
            Map<String, String> classConflictMap = new HashMap<String, String>();

            //getSingletons is high priority than getClasses
            //Step 1: getSingletons loop
            if (appSingletons != null) {
                for (Object singleton : appSingletons) {

                    /**
                     * check if the class is a provider
                     */
                    boolean isJaxRsProvider = false;
                    Class<?> clazz = singleton.getClass();

                    /**
                     * the object of the same class should be processed only once
                     */
                    if (classConflictMap.containsKey(clazz.getName()))
                        continue;

                    classConflictMap.put(clazz.getName(), "S");

                    if (ResourceUtils.isValidProvider(clazz)) {
                        isJaxRsProvider = true;
                        cxfPRHolder.addProvider(singleton);
                    }

                    /**
                     * check if the class is a valid resource (class with @Path)
                     * in case of the registration to some invalid providers
                     */
                    if (!ResourceUtils.isValidResource(clazz)) {

                        if (isJaxRsProvider == true) {
                            singletonProviderAndPathInfos.add(new ProviderResourceInfo(singleton, false, isJaxRsProvider));
                        }

                        continue;
                    }

                    /**
                     * double check: in case if the same class is added in getSingletons loop, ignore getClasses loop
                     */
                    if (cxfPRHolder.addResouceProvider(clazz, new SingletonResourceProvider(singleton)) == false) {
                        cxfPRHolder.addResourceClasses(clazz);
                    }

                    singletonProviderAndPathInfos.add(new ProviderResourceInfo(singleton, true, isJaxRsProvider));
                }
            }

            //Step 2: getClasses loop
            if (appClasses != null) {
                for (Class<?> clazz : appClasses) {

                    /**
                     * the same class should be processed only once
                     */
                    if (classConflictMap.containsKey(clazz.getName()))
                        continue;

                    classConflictMap.put(clazz.getName(), "P");

                    boolean isJaxRsProvider = false;
                    Object singletonProviderInstance = null;
                    //note: as the abstract class can't be used to initialize instance, then it can't be provider or resource
                    if (ResourceUtils.isNotAbstractClass(clazz)) {

                        /**
                         * check if the class is a provider
                         */
                        if (ResourceUtils.isValidProvider(clazz)) {
                            isJaxRsProvider = true;
                            try {
                                Constructor<?> c = ResourceUtils.findResourceConstructor(clazz, false);
                                if (c == null) {
                                    // CWWKW0100W: The JAX-RS Provider class, {0} in the application contains no public constructor. The server will ignore this provider.
                                    Tr.warning(tc, "warn.provider.no.public.ctor", clazz.getName());
                                    continue;
                                }
                                if (c.getParameterTypes().length == 0) {
                                    singletonProviderInstance = c.newInstance();
                                    cxfPRHolder.addProvider(singletonProviderInstance);
                                } else {
                                    cxfPRHolder.addProvider(c);
                                }

                            } catch (Throwable ex) {
                                throw new RuntimeException("Provider " + clazz.getName() + " can not be created", ex);
                            }
                        }

                        /**
                         * check if the class is a valid resource (class with @Path)
                         * in case of the registration to some invalid providers
                         */
                        if (!ResourceUtils.isValidResource(clazz)) {

                            if (isJaxRsProvider == true && null != singletonProviderInstance) {
                                perRequestProviderAndPathInfos.add(new ProviderResourceInfo(singletonProviderInstance, false, isJaxRsProvider));
                            } else {
                                perRequestProviderAndPathInfos.add(new ProviderResourceInfo(clazz, false, isJaxRsProvider));
                            }
                            continue;
                        }

                        /**
                         * when a class is declared in @Provider + @Path,
                         * if the resource's constructor has params, the resource will be handled as singleton object with provider
                         */
                        ResourceProvider pr = null;
                        if (isJaxRsProvider == true && null != singletonProviderInstance) {
                            pr = new SingletonResourceProvider(singletonProviderInstance);
                            singletonProviderAndPathInfos.add(new ProviderResourceInfo(clazz, true, isJaxRsProvider));
                        } else {
                            pr = new PerRequestResourceProvider(clazz);
                            perRequestProviderAndPathInfos.add(new ProviderResourceInfo(clazz, true, isJaxRsProvider));
                        }

                        /**
                         * double check: in case if the same class is added in getSingletons loop, ignore getClasses loop
                         */
                        if (cxfPRHolder.addResouceProvider(clazz, pr) == false) {
                            cxfPRHolder.addResourceClasses(clazz);
                        }
                    } else {
                        endpointInfo.getAbstractClassInterfaceList().add(clazz.getName());
                    }
                }
            }
        }
    }

    public Object getBeanCustomizerContext(JaxRsFactoryBeanCustomizer customizer) {
        return beanCustomizerContexts.get(createCustomizerKey(customizer));
    }

    /**
     * call by LibertyJaxRsInvoker
     *
     * @param exchange
     * @param serviceObject
     * @param m
     * @param paramArray
     * @return
     * @throws Exception
     */
    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        JaxRsFactoryBeanCustomizer beanCustomizer = findBeanCustomizer(serviceObject.getClass());
        if (beanCustomizer != null) {
            final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
            final ClassResourceInfo cri = ori.getClassResourceInfo();

            try {
                Object rc = beanCustomizer.serviceInvoke(serviceObject,
                                                         m,
                                                         paramArray,
                                                         cri.isSingleton(),
                                                         getBeanCustomizerContext(beanCustomizer),
                                                         exchange.getInMessage());

                return rc;
            } finally {
                beanCustomizer.afterServiceInvoke(serviceObject,
                                                  cri.isSingleton(),
                                                  getBeanCustomizerContext(beanCustomizer));
            }

        } else
            return m.invoke(serviceObject, paramArray);
    }

    /**
     * call by LibertyJaxRsInvoker
     *
     * @param exchange
     * @param serviceObject
     * @return
     */

    public Object getServiceObject(Exchange exchange, Object serviceObject) {

        JaxRsFactoryBeanCustomizer beanCustomizer = findBeanCustomizer(serviceObject.getClass());
        if (beanCustomizer != null) {
            final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
            final ClassResourceInfo cri = ori.getClassResourceInfo();

            Object o = beanCustomizer.beforeServiceInvoke(serviceObject,
                                                          cri.isSingleton(),
                                                          getBeanCustomizerContext(beanCustomizer));
            if (o == serviceObject && !beanCustomizer.getClass().getName().equalsIgnoreCase("com.ibm.ws.jaxrs20.ejb.JaxRsFactoryBeanEJBCustomizer")) {
                //call postConstruct method if it has not been repleaced with EJB/CDI for per-request resources
                Method postConstructMethod = ResourceUtils.findPostConstructMethod(o.getClass());
                InjectionUtils.invokeLifeCycleMethod(o, postConstructMethod);

            }
            return o;
        } else {
            //if bean customizer is null, means it is a pojo, we need to call postConstruct here
            Method postConstructMethod = ResourceUtils.findPostConstructMethod(serviceObject.getClass());
            InjectionUtils.invokeLifeCycleMethod(serviceObject, postConstructMethod);
            return serviceObject;
        }
    }

    @Override
    protected void checkResources(boolean server) {
        super.checkResources(server);
    }

    public JaxRsFactoryBeanCustomizer findBeanCustomizer(Class<?> clazz) {
        if ((beanCustomizers == null) || (beanCustomizers.isEmpty()))
            return null;

        for (JaxRsFactoryBeanCustomizer beanCustomizer : beanCustomizers) {
            if (beanCustomizer.isCustomizableBean(clazz, getBeanCustomizerContext(beanCustomizer))) {
                return beanCustomizer;
            }
        }

        return null;
    }

    @Override
    public void setApplication(Application app) {
        super.setApplication(app);
        Set<String> appNameBindings = AnnotationUtils.getNameBindings(app.getClass().getAnnotations());
        for (ClassResourceInfo cri : getServiceFactory().getClassResourceInfo()) {
            Set<String> clsNameBindings = new LinkedHashSet<String>(appNameBindings);
            clsNameBindings.addAll(AnnotationUtils.getNameBindings(cri.getServiceClass().getAnnotations()));
            cri.setNameBindings(clsNameBindings);
        }
    }
}
