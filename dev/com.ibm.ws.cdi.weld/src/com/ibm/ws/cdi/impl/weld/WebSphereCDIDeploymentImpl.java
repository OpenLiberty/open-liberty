/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

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

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.probe.ProbeExtension;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.executor.ExecutorServicesImpl;
import com.ibm.ws.cdi.impl.CDIImpl;
import com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.TransactionService;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.liberty.ExtensionMetaData;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class WebSphereCDIDeploymentImpl implements WebSphereCDIDeployment {
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

    private Application application;
    private final SimpleServiceRegistry serviceRegistry;
    private Iterable<Metadata<Extension>> extensions;
    private final Set<Extension> spiExtensions = new HashSet<Extension>();
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
        this.cdi = new CDIImpl(cdiRuntime);
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
     * Get all BDAs relating only to this application. i.e. not Shared Libs or internal Runtime Extensions
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
    public void initializeOrderedBeanDeploymentArchives() {
        orderedBDAs.addAll(extensionBDAs.values());
        orderedBDAs.addAll(applicationBDAs);
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
                for (ClassLoader classLoader : extensionClassLoaders) {
                    //This ensures that oldCL will be set to the TCCL from before the first itteration of the loop
                    if (oldCL != null) {
                        CDIUtils.getAndSetLoader(classLoader);
                    } else {
                        oldCL = CDIUtils.getAndSetLoader(classLoader);
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
            //if the probe is enabled, add the probe extension

            if (CDIUtils.isDevelopementMode()) {
                extensionSet.add(getProbeExtension());
                WebSphereBeanDeploymentArchive bda = getBeanDeploymentArchive(ProbeExtension.class);
                extensionBDAs.put(bda.getId(), bda);
            }
            extensions = extensionSet;

            for (Extension spiExtension : spiExtensions) {
                ExtensionMetaData metaData = new ExtensionMetaData(spiExtension);
                extensionSet.add(metaData);

                WebSphereBeanDeploymentArchive bda = getBeanDeploymentArchive(spiExtension.getClass());
                extensionBDAs.put(bda.getId(), bda);
            }

            //Create an ordered list so we will find the extension bdas first in getBeanDeploymentArchiveFromClass
            initializeOrderedBeanDeploymentArchives();
        }
        return extensions;
    }

    private Metadata<Extension> getProbeExtension() {

        return CDIUtils.loadExtension(ProbeExtension.class.getName(), ProbeExtension.class.getClassLoader());

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
    //This method is called a lot
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClass(Class<?> clazz) {
        // Note this method looks for a BDA which has the given class in it, **whether the class is a bean or not**
        // We needed this so that we can find the correct bean manager for non-beans like servlets
        WebSphereBeanDeploymentArchive wbda = classBDAMap.get(clazz);

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

                        wbda = bda;
                        classBDAMap.put(clazz, bda);
                        break;
                    }
                }
            }
        }

        return wbda;
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
        if (type != ArchiveType.SHARED_LIB &&
            type != ArchiveType.RUNTIME_EXTENSION) {
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
            if (bda.getType() != ArchiveType.MANIFEST_CLASSPATH && bda.getType() != ArchiveType.WEB_INF_LIB) {
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

    @Override
    public void registerSPIExtension(Set<Extension> extensions) {
        spiExtensions.addAll(extensions);
    }

}
