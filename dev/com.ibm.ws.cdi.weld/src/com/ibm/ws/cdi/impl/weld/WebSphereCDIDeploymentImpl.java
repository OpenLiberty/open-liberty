/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.executor.ExecutorServicesImpl;
import com.ibm.ws.cdi.impl.CDIImpl;
import com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ManagedBeanDescriptor;
import com.ibm.ws.cdi.internal.interfaces.TransactionService;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.liberty.ExtensionMetaData;
import com.ibm.ws.classloading.LibertyClassLoadingService;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class WebSphereCDIDeploymentImpl implements WebSphereCDIDeployment {

    private static final TraceComponent tc = Tr.register(WebSphereCDIDeploymentImpl.class);

    @SuppressWarnings("rawtypes")
    private static final ServiceCaller<LibertyClassLoadingService> classLoadingServiceCaller = new ServiceCaller<LibertyClassLoadingService>(WebSphereCDIDeploymentImpl.class,
                                                                                                                                             LibertyClassLoadingService.class);

    private final String id;
    private final Map<String, WebSphereBeanDeploymentArchive> deploymentDBAs = new HashMap<String, WebSphereBeanDeploymentArchive>();
    private final Set<WebSphereBeanDeploymentArchive> applicationBDAs = new HashSet<WebSphereBeanDeploymentArchive>();
    private final Map<String, WebSphereBeanDeploymentArchive> extensionBDAs = new HashMap<String, WebSphereBeanDeploymentArchive>();

    private final List<WebSphereBeanDeploymentArchive> orderedBDAs = new ArrayList<WebSphereBeanDeploymentArchive>();
    private WeldBootstrap bootstrap;
    private ClassLoader classloader;
    private final Set<ClassLoader> extensionClassLoaders = new HashSet<ClassLoader>();

    //overall isCDIEnabled for whole app
    private Boolean cdiEnabled;
    //per archive cache of isCDIEnabled (does a bda or any it can access have any beans)
    private final Map<String, Boolean> cdiStatusMap = new HashMap<String, Boolean>();

    private final ConcurrentMap<Class<?>, WebSphereBeanDeploymentArchive> classBDAMap = new ConcurrentHashMap<Class<?>, WebSphereBeanDeploymentArchive>();
    private final ConcurrentMap<Class<?>, WebSphereBeanDeploymentArchive> classBDAMapFavouringEjb = new ConcurrentHashMap<Class<?>, WebSphereBeanDeploymentArchive>();

    private Application application;
    private final SimpleServiceRegistry serviceRegistry;
    private Iterable<Metadata<Extension>> extensions;
    private final WebSphereInjectionServicesImpl injectionServices;
    private final CDIRuntime cdiRuntime;
    private final CDIImpl cdi;

    public WebSphereCDIDeploymentImpl(Application application, CDIRuntime cdiRuntime) {
        this.bootstrap = new WeldBootstrap();

        this.id = application.getJ2EEName().toString();
        this.application = application;

        this.serviceRegistry = new SimpleServiceRegistry();
        TransactionService wsTransactionService = cdiRuntime.getTransactionService();
        TransactionServicesImpl transactionServices = null;
        if (wsTransactionService != null) {
            transactionServices = new TransactionServicesImpl(wsTransactionService);
        }
        this.serviceRegistry.add(TransactionServices.class, transactionServices);
        this.serviceRegistry.add(SecurityServices.class, cdiRuntime.getSecurityServices());
        this.serviceRegistry.add(ProxyServices.class, cdiRuntime.getProxyServices());

        ExecutorService executorService = cdiRuntime.getExecutorService();
        if (executorService != null) {
            ExecutorServices executorServices = new ExecutorServicesImpl(executorService, cdiRuntime.getScheduledExecutorService());
            serviceRegistry.add(ExecutorServices.class, executorServices);
        }

        //create a resource injection service for this deployment
        this.injectionServices = new WebSphereInjectionServicesImpl(this);
        this.cdiRuntime = cdiRuntime;
        this.cdi = new CDIImpl(cdiRuntime, this);
    }

    /**
     * Get the Resource Injection Service for this deployment
     *
     * @return a ResourceInjectionServiceImpl
     */
    @Override
    public WebSphereInjectionServicesImpl getInjectionServices() {
        return this.injectionServices;
    }

    /**
     * Set the top level ClassLoader for this deployment. If the deployment represents an EAR then it will be the
     * EAR's classloader. If it is a standalone WAR then it will be the WAR's classloader.
     *
     * @param classloader The top level classloader
     */
    @Override
    public void setClassLoader(ClassLoader classloader) {
        this.classloader = classloader;
        this.extensionClassLoaders.add(classloader);
    }

    /**
     * Get the top level ClassLoader for this deployment.
     *
     * @return the top level ClassLoader
     */
    @Override
    public ClassLoader getClassLoader() {
        return classloader;
    }

    /**
     * Get the WeldBootstrap for this deployment
     *
     * @return the WeldBootstrap
     */
    @Override
    public WeldBootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * Get the unique id for this deployment. At the moment this is the same as the J2EE name for the application.
     *
     * @return the unique deployment id
     */
    @Override
    public String getDeploymentID() {
        return id;
    }

    /**
     * Find a BDA by it's unique archive id. @see BeanDeploymentArchive.getId()
     *
     * @param archiveID the archive id
     * @return a WebSphereBeanDeploymentArchive with that id
     */
    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchive(String archiveID) {
        return deploymentDBAs.get(archiveID);
    }

    /**
     * Get all BDAs which belong to this application. i.e. not internal Runtime Extensions
     *
     * @return all application BDAs
     */
    @Override
    public Collection<WebSphereBeanDeploymentArchive> getApplicationBDAs() {
        return Collections.unmodifiableSet(applicationBDAs);
    }

    /**
     * @return true if any part of the application is CDI enabled
     */
    @Override
    public boolean isCDIEnabled() {
        if (this.cdiEnabled == null) {
            this.cdiEnabled = isCDIEnabled(getApplicationBDAs());
        }
        return this.cdiEnabled;
    }

    /**
     * Do any of the specified BDAs, or any of BDAs accessible by them, have any beans
     *
     * BDAs for Runtime Extensions are ignored
     *
     * @param bdas
     * @return
     * @throws CDIException
     */
    private boolean isCDIEnabled(Collection<WebSphereBeanDeploymentArchive> bdas) {
        boolean anyHasBeans = false;
        for (WebSphereBeanDeploymentArchive bda : bdas) {
            boolean hasBeans = false;

            if (bda.getType() != ArchiveType.RUNTIME_EXTENSION) {
                hasBeans = isCDIEnabled(bda);
            }
            anyHasBeans = anyHasBeans || hasBeans;

            if (anyHasBeans) {
                break;
            }
        }
        return anyHasBeans;
    }

    /**
     * Does the specified BDA, or any of BDAs accessible by it, have any beans or it is an extension which could add beans
     *
     * @param bda the BDA
     * @return true if the specified BDA, or any of BDAs accessible by it, have any beans
     */
    private boolean isCDIEnabled(WebSphereBeanDeploymentArchive bda) {
        Boolean hasBeans = cdiStatusMap.get(bda.getId());
        if (hasBeans == null) {
            //it's enabled if it has beans or it is an extension which could add beans
            hasBeans = bda.hasBeans() || bda.isExtension();
            //setting this now should prevent loops when checking children in the next step
            cdiStatusMap.put(bda.getId(), hasBeans);

            //it's also enabled if any of it's children are enabled (but not including runtime extensions)
            hasBeans = hasBeans || isCDIEnabled(bda.getWebSphereBeanDeploymentArchives());
            //remember the result
            cdiStatusMap.put(bda.getId(), hasBeans);
        }

        return hasBeans;
    }

    /**
     * Does the specified BDA, or any of BDAs accessible by it, have any beans or an extension which might add beans.
     *
     * @param bdaId the id of the BDA
     * @return true if the specified BDA, or any of BDAs accessible by it, have any beans
     */
    @Override
    public boolean isCDIEnabled(String bdaId) {

        boolean hasBeans = false;

        //the top level isCDIEnabled can fail faster
        if (isCDIEnabled()) {
            Boolean hasBeansBoolean = cdiStatusMap.get(bdaId);
            if (hasBeansBoolean == null) {
                WebSphereBeanDeploymentArchive bda = deploymentDBAs.get(bdaId);
                if (bda == null) {
                    // turns out that JAXWS create modules dynamically at runtime. That should be the only way
                    // that there is no bda for a valid module/bda id. At the moment, CDI is not supported in
                    // a module unless it exists at startup.
                    hasBeans = false;
                } else {
                    hasBeans = isCDIEnabled(bda);
                }
            } else {
                hasBeans = hasBeansBoolean;
            }
        }

        return hasBeans;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WebSphere CDI Deployment for " + id;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return new ArrayList<BeanDeploymentArchive>(deploymentDBAs.values());
    }

    /**
     * Create the ordered list for the bdas - extension bdas first and then followed by the application bdas
     *
     */
    @Trivial
    public void initializeOrderedBeanDeploymentArchives() {
        orderedBDAs.addAll(extensionBDAs.values());
        orderedBDAs.addAll(applicationBDAs);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "initializeOrderedBeanDeploymentArchives", orderedBDAs);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives() {
        return new ArrayList<WebSphereBeanDeploymentArchive>(deploymentDBAs.values());
    }

    /** {@inheritDoc} */
    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
        BeanDeploymentArchive bda = getBeanDeploymentArchiveFromClass(beanClass);
        //return the bda if the class is contained in one of the bdas;

        if (bda == null) {
            try {
                bda = createBDAOntheFly(beanClass);
            } catch (CDIException e) {
                //FFDC and carry on
                bda = null;
            }
        }
        return bda;
        //add the graph to the deployment

    }

    /**
     * Create a bda and wire it in the deployment graph
     *
     * @param beanClass the bean class
     * @return the newly created bda
     * @throws CDIException
     */
    private BeanDeploymentArchive createBDAOntheFly(Class<?> beanClass) throws CDIException {
        //Add the class in one of the bdas if an existing bda share the same classloader as the beanClass
        //Otherwise, we need to create a brand new bda and then add the bda to the graph
        //when it reaches here, it means no bda found for this class. We need to create a bda
        //Let's see whether there is a bda with the same classloader as the beanClass, if there is, let's add this class

        BeanDeploymentArchive bdaToReturn = findCandidateBDAtoAddThisClass(beanClass);

        if (bdaToReturn != null) {
            return bdaToReturn;
        } else {
            //If it comes here, it means no bda exists with the same classloader as the bean class, so we need to create a bda for it.
            return createNewBdaAndMakeWiring(beanClass);
        }

    }

    /**
     * Create a new bda and put the beanClass to the bda and then wire this bda in the deployment according to the classloading hierarchy
     *
     * @param beanClass
     * @param beanClassCL the beanClass classloader
     * @return the newly created bda
     */
    private BeanDeploymentArchive createNewBdaAndMakeWiring(Class<?> beanClass) {
        try {
            OnDemandArchive onDemandArchive = new OnDemandArchive(cdiRuntime, application, beanClass);
            WebSphereBeanDeploymentArchive newBda = BDAFactory.createBDA(this, onDemandArchive, cdiRuntime);

            ClassLoader beanClassCL = onDemandArchive.getClassLoader();

            // need to make this bda to be accessible to other bdas according to classloader hierarchy
            for (WebSphereBeanDeploymentArchive wbda : getWebSphereBeanDeploymentArchives()) {

                ClassLoader thisBDACL = wbda.getClassLoader();
                //If the current archive is an extension bda, let's add this newly created bda accessible to it
                if (wbda.getType() == ArchiveType.RUNTIME_EXTENSION) {
                    newBda.addBeanDeploymentArchive(wbda);
                } else {

                    //let's check to see whether the wbda needs to be accessible to this new bda
                    //The current bda should be accessible to the newly created bda if the newly created bda's classloader
                    // is the same or the child classloader of the current bda
                    makeWiring(newBda, wbda, thisBDACL, beanClassCL);
                }

                if ((wbda.getType() == ArchiveType.RUNTIME_EXTENSION) && wbda.extensionCanSeeApplicationBDAs()) {
                    wbda.addBeanDeploymentArchive(newBda);
                } else {

                    //Let's check whether the wbda's classloader is the descendant classloader of the new bda
                    makeWiring(wbda, newBda, beanClassCL, thisBDACL);

                }

            }
            //Add this new bda to the deployment graph
            addBeanDeploymentArchive(newBda);
            return newBda;
        } catch (CDIException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Find the bda with the same classloader as the beanClass and then add the beanclasses to it
     *
     * @param beanClass
     * @param beanClassCL the beanClass classloader
     * @return the found bda
     * @throws CDIException
     */
    private BeanDeploymentArchive findCandidateBDAtoAddThisClass(Class<?> beanClass) throws CDIException {
        for (WebSphereBeanDeploymentArchive wbda : getWebSphereBeanDeploymentArchives()) {

            if (wbda.getClassLoader() == beanClass.getClassLoader()) {
                wbda.addToBeanClazzes(beanClass);
                return wbda;

            }
            //if the cl is null, which means the classloader is the root classloader
            //for this kind of bda, its id ends with CDIUtils.BDA_FOR_CLASSES_LOADED_BY_ROOT_CLASSLOADER
            //all classes loaded by the root classloader should be in a bda with the id ends with CDIUtils.BDA_FOR_CLASSES_LOADED_BY_ROOT_CLASSLOADER
            if ((beanClass.getClassLoader() == null) && (wbda.getId().endsWith(CDIUtils.BDA_FOR_CLASSES_LOADED_BY_ROOT_CLASSLOADER))) {
                wbda.addToBeanClazzes(beanClass);
                return wbda;
            }

        }

        return null;
    }

    /**
     * Make a wiring from the wireFromBda to the wireToBda if the wireFromBda's classloader is the descendant of the wireToBda's classloader
     *
     * @param wireFromBda
     * @param wireToBda
     * @param wireToBdaCL
     * @param wireFromBdaCL
     */
    private void makeWiring(WebSphereBeanDeploymentArchive wireFromBda, WebSphereBeanDeploymentArchive wireToBda, ClassLoader wireToBdaCL, ClassLoader wireFromBdaCL) {
        while (wireFromBdaCL != null) {
            if (wireFromBdaCL == wireToBdaCL) {
                wireFromBda.addBeanDeploymentArchive(wireToBda);
                break;
            } else {
                wireFromBdaCL = wireFromBdaCL.getParent();
            }
        }
        //if we are here, it means the wireToBdaCL is root classloader, loading java.xx classes. All other bdas should be accessible to this new bda.
        if (wireFromBdaCL == wireToBdaCL) {
            wireFromBda.addBeanDeploymentArchive(wireToBda);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    //Without this trivial we get exceptions when using binary logging.
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        if (extensions == null) {
            Set<Metadata<Extension>> extensionSet = new HashSet<Metadata<Extension>>();
            ClassLoader oldCL = null;

            try {
                for (final ClassLoader classLoader : extensionClassLoaders) {

                    //We had a customer who wishes to ensure the TCCL is the same in the constructor and in the observer methods.
                    //This is only possible for one TCCL per application due to limitations in weld.
                    boolean matchesAppTCCL = classLoadingServiceCaller.run((@SuppressWarnings("rawtypes") LibertyClassLoadingService cl) -> {
                        return cl.isThreadContextClassLoaderForAppClassLoader(application.getTCCL(), classLoader);
                    }).orElseThrow(() -> new IllegalStateException("ClassLoadingService missing"));;

                    ClassLoader newTCCL = classLoader;
                    if (matchesAppTCCL) {
                        newTCCL = application.getTCCL();
                    }

                    //This ensures that oldCL will be set to the TCCL from before the first itteration of the loop
                    if (oldCL != null) {
                        CDIUtils.getAndSetLoader(newTCCL);
                    } else {
                        oldCL = CDIUtils.getAndSetLoader(newTCCL);
                    }

                    Iterable<Metadata<Extension>> extensionIt = bootstrap.loadExtensions(classLoader);
                    if (extensionIt != null) {
                        for (Metadata<Extension> extension : extensionIt) {
                            //check whether the extension is in any archive. If not, remove it as it won't be started by Weld
                            Class<?> extClass = extension.getValue().getClass();
                            WebSphereBeanDeploymentArchive bda = getBeanDeploymentArchive(extClass);
                            if (bda != null) {
                                extensionSet.add(extension);
                                extensionBDAs.put(bda.getId(), bda);
                            }
                        }

                    }

                }
            } finally {
                if (oldCL != null) {
                    CDIUtils.getAndSetLoader(oldCL);
                }
            }

            //Now add the extensions from the SPI.
            //Because these are not in a META-INF Service file we have to construct instances
            //to pass to weld.

            for (WebSphereBeanDeploymentArchive deploymentBDA : deploymentDBAs.values()) {
                Set<Supplier<Extension>> spiExtensionSuppliers = deploymentBDA.getSPIExtensionSuppliers();

                if (spiExtensionSuppliers.isEmpty()) {
                    continue;
                }

                extensionBDAs.put(deploymentBDA.getId(), deploymentBDA);

                for (Supplier<Extension> spiExtensionSupplier : spiExtensionSuppliers) {
                    try {
                        Extension extension = spiExtensionSupplier.get();
                        ExtensionMetaData metaData = new ExtensionMetaData(extension);
                        extensionSet.add(metaData);
                    } catch (Exception e) {
                        Tr.error(tc, "exception.creating.extensions.CWOWB1012E", deploymentBDA.toString(), e.toString());
                    }
                }
            }

            //Create an ordered list so we will find the extension bdas first in getBeanDeploymentArchiveFromClass
            initializeOrderedBeanDeploymentArchives();
            extensions = extensionSet;
        }
        return extensions;
    }

    /** {@inheritDoc} */
    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
        // Note this method looks for BDA containing a **bean** of the given class
        // We think this is the correct behavior for the weld CDI11Deployment interfaces
        for (WebSphereBeanDeploymentArchive bda : deploymentDBAs.values()) {
            if (bda.containsBeanClass(beanClass)) {
                return bda;
            }
        }
        return null;
    }

    @Override
    @Trivial
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClass(Class<?> clazz) {
        return getBeanDeploymentArchiveFromClassInternal(clazz, false);
    }

    @Trivial
    //This method is called a lot
    private WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClassInternal(Class<?> clazz, boolean favourEJB) {
        // Note this method looks for a BDA which has the given class in it, **whether the class is a bean or not**
        // We needed this so that we can find the correct bean manager for non-beans like servlets
        WebSphereBeanDeploymentArchive wbda = favourEJB ? classBDAMapFavouringEjb.get(clazz) : classBDAMap.get(clazz);

        if (wbda instanceof MapNoValueMarker) {
            wbda = classBDAMap.get(clazz);
        }

        if (wbda == null) {
            for (WebSphereBeanDeploymentArchive bda : orderedBDAs) {
                if (bda.getAllClazzes().contains(clazz.getName())) {

                    Class<?> bdaClazz = null;
                    try {
                        bdaClazz = CDIUtils.loadClass(bda.getClassLoader(), clazz.getName());
                    } catch (CDIException e) {
                        // TODO Auto-generated catch block
                        // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                        // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
                        e.printStackTrace();
                    }
                    if (bdaClazz == clazz) {

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "mapping class: " + clazz.getName() + " " + System.identityHashCode(clazz) + " to bda: " + bda.toString());
                        }

                        //Always populate classBDAMap with the first BDA that contains this class
                        classBDAMap.putIfAbsent(clazz, bda);

                        //Do a quick check to see if this is also the first EJB_MODULE that contains the class and if so record it.
                        if (bda.getType() == ArchiveType.EJB_MODULE) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "mapping class: " + clazz.getName() + " " + System.identityHashCode(clazz) + " to EJB_MODULE bda: " + bda.toString());
                            }
                            classBDAMapFavouringEjb.putIfAbsent(clazz, bda);
                        }

                        //If we are not in favour EJB mode, check the standard map, if we've put something there return that.
                        //If we are in favour EJB mode
                        if (!favourEJB && classBDAMap.containsKey(clazz)) {
                            wbda = classBDAMap.get(clazz);
                            break;
                        } else if (classBDAMapFavouringEjb.containsKey(clazz)) {
                            wbda = classBDAMapFavouringEjb.get(clazz);
                            break;
                        }
                    }
                }
            }
        }

        //If we've reached here in EJB mode, cache that we don't have a value
        if (favourEJB) {
            classBDAMapFavouringEjb.putIfAbsent(clazz, new MapNoValueMarker());
        }

        //Always return the standard map's value as a fallback
        return classBDAMap.get(clazz);
    }

    @Trivial
    @Override
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClassFavouringEJB(Class<?> clazz) {

        if (classBDAMapFavouringEjb.containsKey(clazz)) {
            return classBDAMapFavouringEjb.get(clazz);
        }

        //If we haven't cached the value, call the internal. It will populate the map, and handle falling back to any BDA with the right class
        return getBeanDeploymentArchiveFromClassInternal(clazz, true);
    }

    /**
     * Add a BeanDeploymentArchive to this deployment
     *
     * @param bda the BDA to add
     * @throws CDIException
     */
    @Override
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive bda) throws CDIException {
        deploymentDBAs.put(bda.getId(), bda);
        extensionClassLoaders.add(bda.getClassLoader());
        ArchiveType type = bda.getType();
        if (type != ArchiveType.RUNTIME_EXTENSION) {
            applicationBDAs.add(bda);
        }
    }

    /**
     * Add a Set of BDAs to the deployment
     *
     * @param bdas the BDAs to add
     * @throws CDIException
     */
    @Override
    public void addBeanDeploymentArchives(Set<WebSphereBeanDeploymentArchive> bdas) throws CDIException {
        for (WebSphereBeanDeploymentArchive bda : bdas) {
            addBeanDeploymentArchive(bda);
        }
    }

    /**
     * Scan all the BDAs in the deployment to see if there are any bean classes.
     *
     * This method must be called before scanForEjbEndpoints() and before we try to do
     * any real work with the deployment or the BDAs
     *
     * @throws CDIException
     */
    @Override
    public void scan() throws CDIException {
        Collection<WebSphereBeanDeploymentArchive> allBDAs = new ArrayList<WebSphereBeanDeploymentArchive>(deploymentDBAs.values());
        for (WebSphereBeanDeploymentArchive bda : allBDAs) {
            bda.scanForBeanDefiningAnnotations(true);
        }
        for (WebSphereBeanDeploymentArchive bda : allBDAs) {
            if (!bda.hasBeenScanned()) {
                bda.scan();
            }
        }
    }

    /**
     * Initialize the Resource Injection Service with each BDA's bean classes.
     *
     * This method must be called after scanForBeans() and scanForEjbEndpoints() but before we try to do
     * any real work with the deployment or the BDAs
     *
     * @throws CDIException
     */
    @Override
    public void initializeInjectionServices() throws CDIException {

        Set<ReferenceContext> cdiReferenceContexts = new HashSet<ReferenceContext>();

        //first we need to initialize the injection service and collect the reference contexts and the injection classes
        for (WebSphereBeanDeploymentArchive bda : getApplicationBDAs()) {
            // Don't initialize child libraries, instead aggregate for the whole module
            // No reference context for shared libs either
            if (bda.getType() != ArchiveType.MANIFEST_CLASSPATH && bda.getType() != ArchiveType.WEB_INF_LIB && bda.getType() != ArchiveType.SHARED_LIB) {
                ReferenceContext referenceContext = bda.initializeInjectionServices();
                cdiReferenceContexts.add(referenceContext);
            }
        }

        // now we need to process the injections
        for (ReferenceContext referenceContext : cdiReferenceContexts) {
            try {
                referenceContext.process();
            } catch (InjectionException e) {
                throw new CDIException(e);
            }
        }

    }

    /**
     * Shutdown and clean up the whole deployment. The deployment will not be usable after this call has been made.
     */
    @Override
    public void shutdown() {
        if (this.bootstrap != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    bootstrap.shutdown();
                    return null;
                }
            });

            this.bootstrap = null;

            this.deploymentDBAs.clear();
            this.applicationBDAs.clear();
            this.extensionBDAs.clear();
            this.orderedBDAs.clear();
            this.classloader = null;
            this.extensionClassLoaders.clear();
            this.cdiEnabled = false;
            this.cdiStatusMap.clear();
            this.application = null;
            this.classBDAMap.clear();
        }
    }

    @Override
    public void validateJEEComponentClasses() throws CDIException {
        Collection<WebSphereBeanDeploymentArchive> bdas = getWebSphereBeanDeploymentArchives();
        for (WebSphereBeanDeploymentArchive bda : bdas) {
            if (!bda.getType().equals(ArchiveType.RUNTIME_EXTENSION)) {
                bda.createInjectionTargetsForJEEComponentClasses();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addReferenceContext(ReferenceContext referenceContext) {
        getInjectionServices().addReferenceContext(referenceContext);
    }

    /** {@inheritDoc} */
    @Override
    public CDI<Object> getCDI() {
        return this.cdi;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getUnversionedBeansXmlURLs() {
        Collection<URL> unversionedBeansXmlURLs = new ArrayList<URL>();
        for (WebSphereBeanDeploymentArchive bda : getWebSphereBeanDeploymentArchives()) {
            BeansXml beansXml = bda.getBeansXml();
            if (beansXml == null) { //check that there is a beans.xml file
                continue; //if there isn't then just move on to the next archive
            }

            //we're only looking for non-empty beans.xml file that do not have a version set
            if (!isEmpty(beansXml)) {
                //if the beans.xml was not an empty file then check if the version was set or not
                boolean unversionedBeansXml = beansXml.getVersion() == null;
                if (unversionedBeansXml) {
                    URL unversionedBeansXmlURL = bda.getBeansXmlResourceURL();
                    unversionedBeansXmlURLs.add(unversionedBeansXmlURL);
                }
            }
        }
        return unversionedBeansXmlURLs;
    }

    /**
     * Does the given BeansXml object represent an empty file?
     *
     * Returns true if the given BeansXml
     * - is the EMPTY_BEANS_XML instance
     * OR
     * - has a null URL
     * OR
     * - contains zero bytes
     *
     * Will return false if the file has any content, even if that content is not valid for a beans.xml file.
     * Validity of the content is checked by Weld.
     *
     * @param beansXml the BeansXml instance to check
     * @return true is it represents an empty file
     */
    public static boolean isEmpty(BeansXml beansXml) {
        boolean empty = false;

        //check if it was an empty beans.xml file
        //note that this is not a well documented "feature" of the Weld SPI but it is the most direct check
        if (beansXml == BeansXml.EMPTY_BEANS_XML) {
            empty = true;
        }

        if (!empty) {
            URL parsedURL = beansXml.getUrl();
            //if the URL is null then this may mean it was an empty beans.xml file
            //note that this may be an undocumented "feature" of the Weld SPI but if the URL is null we can't look in the original file anyway
            if (parsedURL == null) {
                empty = true;
            }

            if (!empty) {
                //this is the most expensive check
                //check if the file is really empty (zero bytes) ... just whitespace like a single space would cause Weld to complain anyway
                InputStream is = null;
                try {
                    is = parsedURL.openStream();
                    if (is.available() == 0) {
                        //file is empty
                        empty = true;
                    }
                } catch (IOException e) {
                    //could not read the file, assume it is empty
                    empty = true;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e1) {
                            //FFDC and ignore
                        }
                    }
                }
            }
        }

        return empty;
    }

    private static final class MapNoValueMarker implements WebSphereBeanDeploymentArchive {

        @Override
        public Collection<String> getBeanClasses() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBeanClasses'");
        }

        @Override
        public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBeanDeploymentArchives'");
        }

        @Override
        public BeansXml getBeansXml() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBeansXml'");
        }

        @Override
        public Collection<EjbDescriptor<?>> getEjbs() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getEjbs'");
        }

        @Override
        public String getId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getId'");
        }

        @Override
        public ServiceRegistry getServices() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getServices'");
        }

        @Override
        public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive accessibleBDA) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addBeanDeploymentArchive'");
        }

        @Override
        public void addDescendantBda(WebSphereBeanDeploymentArchive descendantBda) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addDescendantBda'");
        }

        @Override
        public void addEjbDescriptor(EjbDescriptor<?> ejbDescriptor) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addEjbDescriptor'");
        }

        @Override
        public void addEjbDescriptors(Collection<EjbDescriptor<?>> ejbDescriptors) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addEjbDescriptors'");
        }

        @Override
        public WebSphereCDIDeployment getCDIDeployment() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getCDIDeployment'");
        }

        @Override
        public void addToBeanClazzes(Class<?> clazz) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addToBeanClazzes'");
        }

        @Override
        public Set<String> getAllClazzes() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getAllClazzes'");
        }

        @Override
        public BeanManager getBeanManager() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBeanManager'");
        }

        @Override
        public Set<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getWebSphereBeanDeploymentArchives'");
        }

        @Override
        public Set<WebSphereBeanDeploymentArchive> getDescendantBdas() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDescendantBdas'");
        }

        @Override
        public CDIRuntime getCDIRuntime() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getCDIRuntime'");
        }

        @Override
        public ArchiveType getType() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getType'");
        }

        @Override
        public boolean hasBeans() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'hasBeans'");
        }

        @Override
        public ReferenceContext initializeInjectionServices() throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'initializeInjectionServices'");
        }

        @Override
        public Set<Class<?>> getInjectionClasses() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getInjectionClasses'");
        }

        @Override
        public Set<String> scanForBeanDefiningAnnotations(boolean scanChildren) throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'scanForBeanDefiningAnnotations'");
        }

        @Override
        public void scan() throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'scan'");
        }

        @Override
        public boolean extensionCanSeeApplicationBDAs() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'extensionCanSeeApplicationBDAs'");
        }

        @Override
        public void createInjectionTargetsForJEEComponentClasses() throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'createInjectionTargetsForJEEComponentClasses'");
        }

        @Override
        public void createInjectionTargetsForJEEComponentClass(Class<?> clazz) throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'createInjectionTargetsForJEEComponentClass'");
        }

        @Override
        public <T> List<InjectionPoint> getJEEComponentInjectionPoints(Class<T> clazz) throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getJEEComponentInjectionPoints'");
        }

        @Override
        public <T> InjectionTarget<T> getJEEComponentInjectionTarget(Class<T> clazz) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getJEEComponentInjectionTarget'");
        }

        @Override
        public <T> void addJEEComponentInjectionTarget(Class<T> clazz, InjectionTarget<T> injectionTarget) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addJEEComponentInjectionTarget'");
        }

        @Override
        public boolean containsBeanClass(Class<?> beanClass) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'containsBeanClass'");
        }

        @Override
        public boolean hasBeenScanned() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'hasBeenScanned'");
        }

        @Override
        public Set<Class<?>> getJEEComponentClasses() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getJEEComponentClasses'");
        }

        @Override
        public void addManagedBeanDescriptor(ManagedBeanDescriptor<?> managedBeanDescriptor) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addManagedBeanDescriptor'");
        }

        @Override
        public void addManagedBeanDescriptors(Collection<ManagedBeanDescriptor<?>> managedBeanDescriptors) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'addManagedBeanDescriptors'");
        }

        @Override
        public CDIArchive getArchive() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getArchive'");
        }

        @Override
        public ClassLoader getClassLoader() throws CDIException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getClassLoader'");
        }

        @Override
        public boolean isExtension() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'isExtension'");
        }

        @Override
        public Set<EjbDescriptor<?>> getEjbDescriptor(Class<?> clazz) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getEjbDescriptor'");
        }

        @Override
        public String getEEModuleDescriptorId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getEEModuleDescriptorId'");
        }

        @Override
        public Set<Supplier<Extension>> getSPIExtensionSuppliers() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getSPIExtensionSuppliers'");
        }

        @Override
        public void setSPIExtensionSuppliers(Set<Supplier<Extension>> spiExtensionSuppliers) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'setSPIExtensionSuppliers'");
        }

        @Override
        public Set<String> getBuildCompatibleExtensionClassNames() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBuildCompatibleExtensionClassNames'");
        }

        @Override
        public URL getBeansXmlResourceURL() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getBeansXmlResourceURL'");
        }
    }
}
