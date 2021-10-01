/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi.component;

import static com.ibm.ws.jaxrs20.utils.CustomizerUtils.createCustomizerKey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.message.Message;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.cdi.JAXRSCDIConstants;
import com.ibm.ws.jaxrs20.metadata.CXFJaxRsProviderResourceHolder;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo.RuntimeType;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * CDI customizer : responsible for CDI life cycle management if the Restful Application/Resource/Provider is a CDI managed bean
 * Priority is higher than EJB by default
 */
@Component(name = "com.ibm.ws.jaxrs20.cdi.component.JaxRsFactoryImplicitBeanCDICustomizer", immediate = true, property = { "service.vendor=IBM" })
public class JaxRsFactoryImplicitBeanCDICustomizer implements JaxRsFactoryBeanCustomizer, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(JaxRsFactoryImplicitBeanCDICustomizer.class);
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<>("managedObjectService");

    private CDIService cdiService;

    //The key is Object to match afterServiceInvoke.
    private final Map<Object, CreationalContext<?>> creationalContextsToRelease = new HashMap<>();

    private static List<String> validRequestScopeList = new ArrayList<>();
    private static List<String> validSingletonScopeList = new ArrayList<>();
    static {
        validRequestScopeList.add(JAXRSCDIConstants.REQUEST_SCOPE);
        validRequestScopeList.add(JAXRSCDIConstants.DEPENDENT_SCOPE);
        validRequestScopeList.add(JAXRSCDIConstants.SESSION_SCOPE);
        validSingletonScopeList.add(JAXRSCDIConstants.DEPENDENT_SCOPE);
        validSingletonScopeList.add(JAXRSCDIConstants.APPLICATION_SCOPE);
    }

    private final Map<ComponentMetaData, BeanManager> beanManagers = new WeakHashMap<>();

    private final ConcurrentHashMap<ModuleMetaData, Map<Class<?>, ManagedObjectFactory<?>>> managedObjectFactoryCache = new ConcurrentHashMap<>();

    private ServiceReference<JavaEEVersion> versionRef;
    private volatile Version platformVersion = JavaEEVersion.VERSION_7_0;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#getPriority()
     */
    @Override
    public Priority getPriority() {
        return Priority.Higher;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#isCustomizableBean(java.lang.Class, java.lang.Object)
     */
    @Override
    public boolean isCustomizableBean(Class<?> clazz, Object context) {
        if (context == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);
        if (newContext.isEmpty()) {
            return false;
        }
        if (newContext.containsKey(clazz)) {
            return true;
        }
        if (cdiService == null) {
            return false;
        }
        if (cdiService.isWeldProxy(clazz)) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#onSingletonProviderInit(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T onSingletonProviderInit(T provider, Object context, Message m) {
        if (context == null) {
            return null;
        }
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);
        if (newContext.isEmpty()) {
            return null;
        }
        ManagedObject<?> managedObject = newContext.get(provider.getClass());
        Object newProviderObject = null;
        if (managedObject == null) {
            newProviderObject = getInstanceFromManagedObject(provider, context);
        } else {
            newProviderObject = managedObject.getObject();
        }

        return (T) newProviderObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#onSingletonServiceInit(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T onSingletonServiceInit(T service, Object context) {
        if (context == null) {
            return null;
        }
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);
        if (newContext.isEmpty()) {
            return null;
        }

        ManagedObject<?> managedObject = newContext.get(service.getClass());
        Object newServiceObject = null;
        if (managedObject == null) {
            newServiceObject = getInstanceFromManagedObject(service, context);
        } else {
            newServiceObject = managedObject.getObject();
        }
        return (T) newServiceObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#beforeServiceInvoke(java.lang.Object, boolean, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T beforeServiceInvoke(T serviceObject, boolean isSingleton,
                                     Object context) {
        if (isSingleton || context == null) {
            return serviceObject;
        }

        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);
        if (newContext.isEmpty()) {
            return null;
        }

        Object newServiceObject = null;

        newServiceObject = getInstanceFromManagedObject(serviceObject, context);

        return (T) newServiceObject;

    }

    private BeanManager getBeanManager() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        BeanManager beanMgr = beanManagers.get(cmd);
        if (beanMgr == null) {
            beanMgr = cdiService.getCurrentModuleBeanManager();
            synchronized (beanManagers) {
                beanManagers.put(cmd, beanMgr);
            }
        }
        return beanMgr;
    }

    private Object getClassFromCDI(Class<?> clazz) {
        BeanManager manager = getBeanManager();
        Bean<?> bean = getBeanFromCDI(clazz);
        Object obj = null;
        if (bean != null && manager != null) {
            CreationalContext<?> cc = manager.createCreationalContext(bean);
            obj = manager.getReference(bean, clazz, cc);
            if (cc != null && obj != null) {
                creationalContextsToRelease.put(obj, cc);
            }
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstanceFromManagedObject(T serviceObject, Object context) {

        Class<?> clazz = serviceObject.getClass();
        //temp fix for session problem

        Object rtn = getClassFromCDI(clazz);
        if (rtn != null) {
            return (T) rtn;
        }
        //end temp fix
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);

        ManagedObject<T> newServiceObject = null;
        if (cdiService.isWeldProxy(clazz)) {
            newServiceObject = (ManagedObject<T>) getClassFromServiceObject(clazz, serviceObject);
        } else {
            newServiceObject = (ManagedObject<T>) getClassFromManagedObject(clazz);
        }
        if (newServiceObject != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get instance from CDI " + clazz.getName());
            }

            ManagedObject<?> oldMO = newContext.put(clazz, newServiceObject);
            if (oldMO != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                // because we are using a ThreadBasedHashMap here, we not actually clobbering data,
                // but we have the potential for problems as multiple threads are access the same
                // CDI-managed, per-request resource concurrently.
                Tr.debug(tc, "getInstanceFromManagedObejct - \"clobbered\" " + oldMO + " with " + newServiceObject + " for key " + clazz + " in map " + newContext);
            }

            return newServiceObject.getObject();
        } else {
            newContext.remove(clazz);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Get instance from CDI is null , use from rs for " + clazz.getName());
        }
        return serviceObject;
    }

    /**
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(value = { Exception.class })
    private <T> ManagedObject<T> getClassFromServiceObject(Class<T> clazz, Object serviceObject) {

        if (! clazz.equals(serviceObject.getClass())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't create object instance from ManagedObjectFactory for : " + clazz.getName() + "because the serviceObject and class had different types");
            }
            return null;
        }

        ManagedObjectFactory<T> managedObjectFactory = (ManagedObjectFactory<T>) getManagedObjectFactory(clazz);

        ManagedObject<T> bean = null;
        try {
            bean = managedObjectFactory.createManagedObject((T) serviceObject, null);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't create object instance from ManagedObjectFactory for : " + clazz.getName() + ", but ignore the FFDC: ", e);
            }
        }

        return bean;
    }

    /**
     * @param clazz
     * @return
     */
    @FFDCIgnore(value = { Exception.class })
    private ManagedObject<?> getClassFromManagedObject(Class<?> clazz) {

        ManagedObjectFactory<?> managedObjectFactory = getManagedObjectFactory(clazz);

        ManagedObject<?> bean = null;
        try {
            bean = managedObjectFactory.createManagedObject();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't create object instance from ManagedObjectFactory for : " + clazz.getName() + ", but ignore the FFDC: ", e);
            }
        }

        return bean;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#serviceInvoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], boolean, java.lang.Object)
     */
    @Override
    public Object serviceInvoke(Object serviceObject, Method m, Object[] params, boolean isSingleton, Object context, Message msg) throws Exception {
        return m.invoke(serviceObject, params);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#afterServiceInvoke(java.lang.Object, boolean, java.lang.Object)
     */
    @Override
    public void afterServiceInvoke(Object serviceObject, boolean isSingleton, Object context) {
        if (creationalContextsToRelease.containsKey(serviceObject)) {
            CreationalContext<?> cc = creationalContextsToRelease.remove(serviceObject);
            if (cc != null)
                cc.release();
        }

        @SuppressWarnings("unchecked")
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) (context);

        ManagedObject<?> mo = newContext.get(serviceObject.getClass());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "afterServiceInvoke mo=" + mo + " isSingleton=" + isSingleton + " newContext={0}", newContext);
        }
        if (!isSingleton) {
            if (mo != null) {
                mo.release();
            }
            newContext.put(serviceObject.getClass(), null);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#onApplicationInit(javax.ws.rs.core.Application, com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData)
     */
    @Override
    public Application onApplicationInit(Application app, JaxRsModuleMetaData metaData) {

        Class<?> clazz = app.getClass();

        if (!shouldHandle(clazz, true)) {
            return null;
        }
        Application newApp = null;

        ManagedObject<?> mo = getClassFromManagedObject(clazz);
        metaData.setManagedAppRef(mo);

        if (mo != null) {
            newApp = (Application) (mo.getObject());
        }

        if (newApp == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "App: app is null from CDI , get app from rs for " + app.getClass().getName());
            }
            return app;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "App: get app from CDI " + app.getClass().getName());
        }
        return newApp;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#onPrepareProviderResource(com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer.BeanCustomizerContext)
     */
    @Override
    public void onPrepareProviderResource(BeanCustomizerContext context) {

        EndpointInfo endpointInfo = context.getEndpointInfo();
        Set<ProviderResourceInfo> perRequestProviderAndPathInfos = endpointInfo.getPerRequestProviderAndPathInfos();
        Set<ProviderResourceInfo> singletonProviderAndPathInfos = endpointInfo.getSingletonProviderAndPathInfos();

        //The resources map may already exist on the context.  If it does we will want to add to it.
        @SuppressWarnings("unchecked")
        Map<Class<?>, ManagedObject<?>> resourcesManagedbyCDI = (Map<Class<?>, ManagedObject<?>>)context.getContextObject();
        if (resourcesManagedbyCDI == null || !(resourcesManagedbyCDI instanceof ThreadBasedHashMap)) {
            resourcesManagedbyCDI = new ThreadBasedHashMap();//HashMap<Class<?>, ManagedObject<?>>();
        }

        CXFJaxRsProviderResourceHolder cxfPRHolder = context.getCxfRPHolder();
        for (ProviderResourceInfo p : perRequestProviderAndPathInfos) {
            /**
             * CDI customizer only check if the POJO type bean is CDI bean
             * because when EJB priority is higher than CDI, engine will take the bean as EJB but not CDI,
             * that means EJB already processes it, CDI should not process it again,
             * then CDI should not cache the bean's info in resourcesManagedbyCDI
             */
            if (p.getRuntimeType() != RuntimeType.POJO)
                continue;

            Class<?> clazz = p.getProviderResourceClass();
            if (!hasValidConstructor(clazz, false)) {
                continue;
            }

            Bean<?> bean = null;
            try {
                bean = getBeanFromCDI(clazz);
            } catch (Exception e1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ManagedObjectFactory failed to create bean", e1);
                }
            }

            if (bean != null) {

                String scopeName = bean.getScope().getName();
                p.setRuntimeType(RuntimeType.CDI);
                resourcesManagedbyCDI.put(p.getProviderResourceClass(), null);

                if (p.isJaxRsProvider()) {
                    //if CDI Scope is APPLICATION_SCOPE or DEPENDENT_SCOPE, report warning and no action: get provider from CDI
                    if (validSingletonScopeList.contains(scopeName)) {
                        logProviderMismatch(clazz, scopeName, "CDI");
                    }
                    //else report warning, keep using provider from rs: change to use RuntimeType.POJO
                    else {
                        p.setRuntimeType(RuntimeType.POJO);
                        resourcesManagedbyCDI.remove(p.getProviderResourceClass());
                        logProviderMismatch(clazz, scopeName, "JAXRS");
                    }
                } else {
                    if (!validRequestScopeList.contains(scopeName)) { //means this is @ApplicationScoped in CDI
                        logResourceMismatch(clazz, "PerRequest", scopeName, "CDI");
                    }

                }
            } else {

                if (shouldHandle(clazz, false)) {
                    p.setRuntimeType(RuntimeType.IMPLICITBEAN);
                    resourcesManagedbyCDI.put(clazz, null);
                }
                continue;

            }

        }
        for (ProviderResourceInfo o : singletonProviderAndPathInfos) {
            /**
             * CDI customizer only check if the POJO type bean is CDI bean
             * because when EJB priority is higher than CDI, engine will take the bean as EJB but not CDI,
             * that means EJB already processes it, CDI should not process it again,
             * then CDI should not cache the bean's info in resourcesManagedbyCDI
             */
            if (o.getRuntimeType() != RuntimeType.POJO)
                continue;

            Class<?> clazz = o.getProviderResourceClass();
            if (!hasValidConstructor(clazz, true)) {
                continue;
            }

            Bean<?> bean = null;
            try {
                bean = getBeanFromCDI(clazz);
            } catch (Exception e1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ManagedObjectFactory failed to create bean", e1);
                }
            }

            if (bean != null) {

                String scopeName = bean.getScope().getName();
                o.setRuntimeType(RuntimeType.CDI);
                resourcesManagedbyCDI.put(o.getProviderResourceClass(), null);
                if (o.isJaxRsProvider()) {
                    if (validSingletonScopeList.contains(scopeName)) {
                        logProviderMismatch(clazz, scopeName, "CDI");
                    }
                    //else report warning, keep using provider from rs: change to use RuntimeType.POJO
                    else {
                        o.setRuntimeType(RuntimeType.POJO);
                        resourcesManagedbyCDI.remove(clazz);
                        logProviderMismatch(clazz, scopeName, "JAXRS");
                    }

                    //Old check is this, need verify by using FAT:
                    //if CDI Scope is APPLICATION_SCOPE or DEPENDENT_SCOPE, report warning and no action: get provider from CDI
                    //else report warning and no action: get provider from CDI

                } else {
                    if (!validSingletonScopeList.contains(scopeName)) { // means CDI is per-request, then modify cxfPRHolder to per-request as well.
                        cxfPRHolder.removeResouceProvider(clazz);//remove from original ResourceProvider map and re-add the new one.
                        cxfPRHolder.addResouceProvider(clazz, new PerRequestResourceProvider(clazz));
                        logResourceMismatch(clazz, "Singleton", scopeName, "CDI");
                    }

                }
            } else {
                if (shouldHandle(clazz, false)) {
                    o.setRuntimeType(RuntimeType.IMPLICITBEAN);
                    resourcesManagedbyCDI.put(clazz, null);
                }
                continue;
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Map of Managed Objects " + resourcesManagedbyCDI);
        }
        context.setContextObject(resourcesManagedbyCDI);

    }

    private Bean<?> getBeanFromCDI(Class<?> clazz) {
        if (!isCDIEnabled()) {
            return null;
        }
        BeanManager manager = getBeanManager();
        Set<Bean<?>> beans = manager.getBeans(clazz);
        Bean<?> bean = manager.resolve(beans);
        return bean;
    }

    private boolean isCDIEnabled() {

        BeanManager beanManager = getBeanManager();
        return beanManager == null ? false : true;
    }

    /**
     * @param clazz
     * @return
     */
    private boolean shouldHandle(Class<?> clazz, boolean singleton) {
        if (!hasValidConstructor(clazz, singleton)) {
            return false;
        }
        return hasInjectOrResourceAnnotation(clazz);

    }

    /**
     * @param clazz
     * @return
     */
    private boolean hasValidConstructor(final Class<?> clazz, final boolean singleton) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                if (constructors.length == 0) {
                    return true;
                }
                boolean hasDependent = clazz.isAnnotationPresent(Dependent.class);

                for (Constructor<?> c : constructors) {
                    boolean hasInject = c.isAnnotationPresent(Inject.class);
                    if (hasInject && hasDependent) {
                        return true;
                    }
                    Class<?>[] params = c.getParameterTypes();
                    Annotation[][] anns = c.getParameterAnnotations();
                    boolean match = true;
                    for (int i = 0; i < params.length; i++) {
                        if (singleton) {
                            //annotation is not null and not equals context
                            if (AnnotationUtils.getAnnotation(anns[i], Context.class) == null && !(anns.length == 0 && hasInject)) {
                                match = false;
                                break;
                            }
                        } else if ((!AnnotationUtils.isValidParamAnnotations(anns[i])) && !(anns.length == 0 && hasInject)) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        return true;
                    }
                }
                return false;
            }
        });

    }

    /**
     * @param clazz
     * @return
     */
    private boolean hasInjectOrResourceAnnotation(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                if (clazz.isAnnotationPresent(Inject.class) || clazz.isAnnotationPresent(Resource.class)) {
                    return true;
                }

                Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].isAnnotationPresent(Inject.class) || fields[i].isAnnotationPresent(Resource.class)) {
                        return true;
                    }
                }

                Method[] methods = clazz.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    if (methods[i].isAnnotationPresent(Inject.class) || methods[i].isAnnotationPresent(Resource.class)) {
                        return true;
                    }
                }

                Constructor<?>[] c = clazz.getConstructors();
                for (int i = 0; i < c.length; i++) {
                    if (c[i].isAnnotationPresent(Inject.class) || c[i].isAnnotationPresent(Resource.class)) {
                        return true;
                    }
                }

                Class<?> cls = clazz.getSuperclass();
                if (cls != null) {
                    return hasInjectOrResourceAnnotation(cls);
                }
                return false;

            }
        });

    }

    /**
     * CDI doesn't require to wrap proxy on the provider
     */
    @Override
    public <T> T onSetupProviderProxy(T provider, Object contextObject) {
        return null;
    }

    private ManagedObjectFactory<?> getManagedObjectFactory(Class<?> clazz) {


        ManagedObjectFactory<?> mof = null;
        try {
            ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
            Map<Class<?>, ManagedObjectFactory<?>> cache = managedObjectFactoryCache.get(mmd);
            if (cache != null) {
                mof = cache.get(clazz);
            } else {
                managedObjectFactoryCache.putIfAbsent(mmd, new ConcurrentHashMap<Class<?>, ManagedObjectFactory<?>>());
                cache = managedObjectFactoryCache.get(mmd);
            }
            if (mof != null) {
                return mof;
            }
            ManagedObjectService mos = managedObjectServiceRef.getServiceWithException();
            if (mos == null) {
                return null;
            }

            mof = mos.createManagedObjectFactory(mmd, clazz, true);
            cache.put(clazz, mof);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully to create ManagedObjectFactory for class: " + clazz.getName());
            }
        } catch (ManagedObjectException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to create ManagedObjectFactory for class: " + clazz.getName() + " Exception is: " + e.toString());
            }
        }

        return mof;
    }

    public void activate(ComponentContext compcontext, Map<String, Object> properties) {

        this.managedObjectServiceRef.activate(compcontext);

    }

    public void deactivate(ComponentContext componentContext) {

        this.managedObjectServiceRef.deactivate(componentContext);

    }

    @Reference(name = "managedObjectService",
               service = ManagedObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.unsetReference(ref);
    }

    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }

    @Reference(service = JavaEEVersion.class)
    protected synchronized void setVersion(ServiceReference<JavaEEVersion> reference) {
        versionRef = reference;
        platformVersion = Version.parseVersion((String) reference.getProperty("version"));
    }

    protected synchronized void unsetVersion(ServiceReference<JavaEEVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            platformVersion = JavaEEVersion.VERSION_7_0;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer#destroyApplicationScopeResources()
     */
    @Override
    public void destroyApplicationScopeResources(JaxRsModuleMetaData jaxRsModuleMetaData) {

        for (ModuleMetaData mmd : jaxRsModuleMetaData.getEnclosingModuleMetaDatas()) {
            managedObjectFactoryCache.remove(mmd);
            synchronized (beanManagers) {
                Iterator<ComponentMetaData> iter = beanManagers.keySet().iterator();
                while (iter.hasNext()) {
                    ComponentMetaData cmd = iter.next();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "destroyApplicationScopeResources - is " + cmd + " a child of " + mmd + "?");
                    }

                    if (mmd.equals(cmd.getModuleMetaData())) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "destroyApplicationScopeResources - yes");
                        }
                        iter.remove();
                    }
                }
            }
        }
        Bus bus = jaxRsModuleMetaData.getServerMetaData().getServerBus();

        @SuppressWarnings("unchecked")
        Map<String, BeanCustomizerContext> beanCustomizerContexts = (Map<String, BeanCustomizerContext>) bus.getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);
        if (beanCustomizerContexts == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Class<?>, ManagedObject<?>> newContext = (Map<Class<?>, ManagedObject<?>>) beanCustomizerContexts.get(createCustomizerKey(this));
        if (newContext == null) {
            return;
        }
        Collection<ManagedObject<?>> objects = newContext.values();
        for (ManagedObject<?> mo : objects) {

            if (mo != null) {
                mo.release();
            }
        }

//destroy application
        ManagedObject<?> appObject = (ManagedObject<?>) jaxRsModuleMetaData.getManagedAppRef();
        if (appObject != null) {
            appObject.release();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarting(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStarted(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopping(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.state.ApplicationStateListener#applicationStopped(com.ibm.ws.container.service.app.deploy.ApplicationInfo)
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // clear out bean managers cache on app shutdown to avoid memory leak
        synchronized(beanManagers) {
            beanManagers.clear();
        }

    }

    @Trivial
    private void logResourceMismatch(Class<?> clazz, String jaxrsScope, String cdiScope, String lifecycleMgr) {
        if (platformVersion.getMajor() > 7) {
            Tr.debug(tc, "CWWKW1001W: The scope " + jaxrsScope + " of JAXRS-2.0 Resource " + clazz.getSimpleName() +
                         " does not match the CDI scope " + cdiScope + ". Liberty gets resource instance from " +
                         lifecycleMgr + ".");
        } else {
            Tr.warning(tc, "warning.jaxrs.cdi.resource.mismatch", clazz.getSimpleName(), jaxrsScope, cdiScope, lifecycleMgr);
        }
    }

    @Trivial
    private void logProviderMismatch(Class<?> clazz, String scopeName, String lifecycleMgr) {
        Tr.warning(tc, "warning.jaxrs.cdi.provider.mismatch", clazz.getSimpleName(), scopeName, lifecycleMgr);
    }
}
