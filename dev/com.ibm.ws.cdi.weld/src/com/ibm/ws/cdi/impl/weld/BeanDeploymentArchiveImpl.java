/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldInjectionTargetFactory;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.resources.MemberTransformer;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.cdi.internal.interfaces.EndPointsInfo;
import com.ibm.ws.cdi.internal.interfaces.ManagedBeanDescriptor;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.ws.cdi.utils.WeldCDIUtils;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * The implementation of Weld spi BeanDeploymentArchive to represent a CDI bean
 * archive.
 *
 *
 */
public class BeanDeploymentArchiveImpl implements WebSphereBeanDeploymentArchive {

    //the classes which is directly in this archive
    private final Set<String> archiveClassNames = new HashSet<String>();
    //all of the classes ... those in this archive and any additional ones
    private final Set<String> allClasses = new HashSet<String>();
    //only those classes which are beans are actually loaded ... stored in an ordered map to make debug easier
    private final Map<String, Class<?>> beanClasses = new TreeMap<String, Class<?>>();

    //this archive's classloader
    private final ClassLoader classloader;

    private final Set<Class<?>> ejbClasses = new HashSet<Class<?>>();
    private final Set<Class<?>> managedBeanClasses = new HashSet<Class<?>>();
    private final Set<Class<?>> injectionClasses = new HashSet<Class<?>>();

    private final Set<Class<?>> jeeComponentClasses = new HashSet<Class<?>>();

    private final Set<String> additionalClasses = new HashSet<String>();
    private final Set<String> additionalBeanDefiningAnnotations = new HashSet<String>();

    private final Set<String> extensionClassNames = new HashSet<String>();

    private final ServiceRegistry weldServiceRegistry;
    private final String id;
    private final EEModuleDescriptor eeModuleDescriptor;

    private final Set<WebSphereBeanDeploymentArchive> accessibleBDAs = new HashSet<WebSphereBeanDeploymentArchive>();
    private final Set<WebSphereBeanDeploymentArchive> descendantBDAs = new HashSet<WebSphereBeanDeploymentArchive>();

    private final Bootstrap bootstrap;

    private final WebSphereCDIDeployment cdiDeployment;
    private final Set<EjbDescriptor<?>> ejbDescriptors = new HashSet<EjbDescriptor<?>>();

    private final Map<Class<?>, Set<EjbDescriptor<?>>> ejbDescriptorMap = new HashMap<Class<?>, Set<EjbDescriptor<?>>>();
    private boolean scanned = false;
    private boolean hasBeans = false;
    private boolean endpointsScanned = false;

    private final Set<Class<?>> nonCDIInterceptors = new HashSet<Class<?>>();
    private BeansXml beansXml;
    private BeanDiscoveryMode beanDiscoveryMode = null;

    private Set<String> directBeanDefiningAnnotations;
    private Set<String> accessibleBeanDefiningAnnotations;

    private final CDIRuntime cdiRuntime;

    private Boolean isExtension = null;

    private final boolean extensionCanSeeApplicationBDAs;

    private final ResourceLoader resourceLoader;

    private final Map<Class<?>, InjectionTarget<?>> injectionTargets = new HashMap<Class<?>, InjectionTarget<?>>();
    private final Map<Class<?>, List<InjectionPoint>> staticInjectionPoints = new HashMap<Class<?>, List<InjectionPoint>>();

    private final CDIArchive archive;

    private static final TraceComponent tc = Tr.register(BeanDeploymentArchiveImpl.class);

    //package visibility only ... use factory
    BeanDeploymentArchiveImpl(WebSphereCDIDeployment cdiDeployment,
                              String archiveID,
                              CDIArchive archive,
                              CDIRuntime cdiRuntime,
                              Set<String> archiveClassNames, //the classes directly in this archive
                              Set<String> additionalClasses, //additional classes not actually in this archive
                              Set<String> additionalBeanDefiningAnnotations,
                              boolean extensionCanSeeApplicationBDAs,
                              Set<String> extensionClassNames,
                              EEModuleDescriptor eeModuleDescriptor) throws CDIException {
        this.id = archiveID;
        this.eeModuleDescriptor = eeModuleDescriptor;
        this.archive = archive;
        this.classloader = archive.getClassLoader();
        this.cdiDeployment = cdiDeployment;
        this.extensionCanSeeApplicationBDAs = extensionCanSeeApplicationBDAs;

        this.bootstrap = cdiDeployment.getBootstrap();

        //archive classes only
        this.archiveClassNames.addAll(archiveClassNames);
        //additional bean defining annotation classes (those not directly in the archive)
        this.additionalBeanDefiningAnnotations.addAll(additionalBeanDefiningAnnotations);
        //additional classes; both regular and annotations
        this.additionalClasses.addAll(additionalClasses);
        this.additionalClasses.addAll(additionalBeanDefiningAnnotations);

        //all classes = archive classes + additional classes
        this.allClasses.addAll(this.archiveClassNames);
        this.allClasses.addAll(this.additionalClasses);

        this.extensionClassNames.addAll(extensionClassNames);

        this.cdiRuntime = cdiRuntime;

        this.weldServiceRegistry = new SimpleServiceRegistry();

        this.resourceLoader = new ResourceLoaderImpl(archive.getClassLoader());
        EjbEndpointService ejbEndpointService = cdiRuntime.getEjbEndpointService();
        if (ejbEndpointService != null) {
            EjbServices ejbServices = ejbEndpointService.getWebSphereEjbServices(cdiDeployment.getDeploymentID());
            this.weldServiceRegistry.add(EjbServices.class, ejbServices);
        }

        WebSphereInjectionServices injectionServices = cdiDeployment.getInjectionServices();
        this.weldServiceRegistry.add(InjectionServices.class, injectionServices);
        this.weldServiceRegistry.add(ResourceLoader.class, this.resourceLoader);
        this.weldServiceRegistry.add(EEModuleDescriptor.class, eeModuleDescriptor);
    }

    @Override
    public Set<String> scanForBeanDefiningAnnotations(boolean includeAccessible) throws CDIException {

        Set<String> beanDefiningAnnotations = new HashSet<String>(this.additionalBeanDefiningAnnotations);

        //these are the annotations directly in this BDA
        if (this.directBeanDefiningAnnotations == null) {
            this.directBeanDefiningAnnotations = new HashSet<String>();

            BeanDiscoveryMode mode = getBeanDiscoveryMode();
            if (mode != BeanDiscoveryMode.NONE) {
                this.directBeanDefiningAnnotations.addAll(archive.getBeanDefiningAnnotations());
                this.directBeanDefiningAnnotations.retainAll(this.archiveClassNames);
            }

            this.directBeanDefiningAnnotations.addAll(CDIUtils.BEAN_DEFINING_ANNOTATION_NAMES);
        }
        beanDefiningAnnotations.addAll(this.directBeanDefiningAnnotations);

        //do we want to get the annotations in the accessible BDAs as well
        if (includeAccessible) {
            if (this.accessibleBeanDefiningAnnotations == null) {
                this.accessibleBeanDefiningAnnotations = new HashSet<String>();
                for (WebSphereBeanDeploymentArchive child : accessibleBDAs) {
                    this.accessibleBeanDefiningAnnotations.addAll(child.scanForBeanDefiningAnnotations(false));
                }
            }
            beanDefiningAnnotations.addAll(this.accessibleBeanDefiningAnnotations);
        }

        return beanDefiningAnnotations;
    }

    /**
     * Determine the bean deployment archive scanning mode
     * If there is a beans.xml, the bean discovery mode will be used.
     * If there is no beans.xml, the mode will be annotated, unless the enableImplicitBeanArchives is configured as false via the server.xml.
     * If there is no beans.xml and the enableImplicitBeanArchives attribute on cdi12 is configured to false, the scanning mode is none.
     * If there is no beans.xml and this archive is an extension, the bean discovery mode is none.
     *
     * @return
     */
    private BeanDiscoveryMode getBeanDiscoveryMode() {
        if (beanDiscoveryMode == null) {
            BeansXml beansXml = getBeansXml();

            beanDiscoveryMode = BeanDiscoveryMode.ANNOTATED;
            if (beansXml != null) {
                beanDiscoveryMode = beansXml.getBeanDiscoveryMode();
            } else if ((cdiRuntime.isImplicitBeanArchivesScanningDisabled(this.archive) || isExtension())) {
                // If the server.xml has the configuration of enableImplicitBeanArchives sets to false, we will not scan the implicit bean archives
                beanDiscoveryMode = BeanDiscoveryMode.NONE;
            }
        }
        return beanDiscoveryMode;

    }

    @Override
    public void scan() throws CDIException {
        if (!this.scanned) {
            //mark as scanned up front to prevent loops
            this.scanned = true;

            //scan the children
            for (WebSphereBeanDeploymentArchive child : accessibleBDAs) {
                if (!child.hasBeenScanned()) {
                    child.scan();
                }
            }

            //find the names of all potential bean classes in this archive
            Set<String> rawBeanclassNames = scanForBeanClassNames();

            //load only those classes
            ClassLoader classLoader = getClassLoader();
            Map<String, Class<?>> loadedBeanClasses = CDIUtils.loadClasses(classLoader, rawBeanclassNames);

            //filter the classes down to make sure they only exist in one BDA
            for (Map.Entry<String, Class<?>> classEntry : loadedBeanClasses.entrySet()) {
                String className = classEntry.getKey();
                Class<?> loadedClass = classEntry.getValue();
                ClassLoader actualClassLoader = loadedClass.getClassLoader();
                if (actualClassLoader == classLoader || !isAccessibleBean(loadedClass)) {
                    this.beanClasses.put(className, loadedClass);
                }
            }
        }

        this.hasBeans = this.beanClasses.size() > 0;

        if (archive.getType() != ArchiveType.RUNTIME_EXTENSION) {
            //find any ejb endpoints but not for runtime extensions
            scanForEndpoints();
        }

        //find which classes are eligible for Resource injection
        initializeInjectionClasses(this.beanClasses.values());
        initializeJEEComponentClasses(this.allClasses);

    }

    private boolean isAccessibleBean(Class<?> beanClass) {
        boolean accessibleBean = false;
        for (WebSphereBeanDeploymentArchive child : accessibleBDAs) {
            if (child.containsBeanClass(beanClass)) {
                accessibleBean = true;
                break;
            }
        }
        return accessibleBean;
    }

    private Set<String> scanForBeanClassNames() throws CDIException {
        Set<String> classNames = new HashSet<String>();

        BeanDiscoveryMode mode = getBeanDiscoveryMode();
        if (mode == BeanDiscoveryMode.ANNOTATED) {
            //first find the bean defining annotations in this archive, any accessible archives and any default bean defining annotations
            Set<String> beanDefiningAnnotations = scanForBeanDefiningAnnotations(true);
            //then find the classes in this archive that are annotated with those annotations
            Set<String> scannedClassNames = archive.getAnnotatedClasses(beanDefiningAnnotations);
            //keep only those classes which are directly in this archive
            scannedClassNames.retainAll(this.archiveClassNames);
            classNames.addAll(scannedClassNames);
        } else if (mode == BeanDiscoveryMode.ALL) {
            classNames.addAll(this.archiveClassNames);
        }
        if ((mode != BeanDiscoveryMode.ALL) && isExtension()) {
            classNames.addAll(this.extensionClassNames);
        }
        classNames.addAll(this.additionalClasses);

        //the client main class can never be a bean in a client module
        if (archive.getType() == ArchiveType.CLIENT_MODULE) {
            String appMainClassName = archive.getClientModuleMainClass();
            classNames.remove(appMainClassName);
        }

        return classNames;
    }

    private void initializeInjectionClasses(Collection<Class<?>> beanClasses) throws CDIException {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        classes.addAll(beanClasses);

        if (archive.getType() == ArchiveType.CLIENT_MODULE) {
            Map<String, Class<?>> clientClasses = getClientContainerManagedClasses();
            classes.removeAll(clientClasses.values());
        }

        //we need to remove the exclude classes
        removeVetoedClasses(classes);

        //Remove the ejbs from the current bda
        classes.removeAll(getEJBClasses());
        classes.removeAll(getManagedBeanClasses());

        this.injectionClasses.addAll(classes);
    }

    private void initializeJEEComponentClasses(Set<String> allClassNames) throws CDIException {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        //the class names from the InjectionClassList interface covers all of the Web Components
        //it will soon include WebSockets as well

        ClassLoader classLoader = getClassLoader();

        List<String> jeeComponentClassNames = archive.getInjectionClassList();
        if (jeeComponentClassNames != null && jeeComponentClassNames.size() > 0) {
            for (String className : allClassNames) {
                if (jeeComponentClassNames.contains(className)) {
                    Class<?> clazz = CDIUtils.loadClass(classLoader, className);
                    if (clazz != null) {
                        classes.add(clazz);
                    } else { 
                        Tr.debug(tc, "jee Component Class was null", className);
                    }
                }
            }
        }

        //add mdbs (session beans are also CDI beans so don't need to be included)
        Collection<EjbDescriptor<?>> ejbs = getEjbs();
        for (EjbDescriptor<?> ejb : ejbs) {
            if (ejb.isMessageDriven()) {
                classes.add(ejb.getBeanClass());
                if (ejb.getBeanClass() == null) { 
                    Tr.debug(tc, "Message Bean's bean class was null", ejb);
                }
            }
        }

        //add ManagedBean classes
        Set<Class<?>> managedBeans = getManagedBeanClasses();
        classes.addAll(managedBeans);

        //add the client classes
        if (archive.getType() == ArchiveType.CLIENT_MODULE) {
            Map<String, Class<?>> clientClasses = getClientContainerManagedClasses();
            classes.addAll(clientClasses.values());
        }

        //TODO
        //we are missing all JPA classes

        classes.addAll(nonCDIInterceptors);
        this.jeeComponentClasses.addAll(classes);
    }

    private void scanForEndpoints() throws CDIException {

        if (!endpointsScanned) {
            endpointsScanned = true;

            EjbEndpointService ejbEndpointService = cdiRuntime.getEjbEndpointService();

            if (ejbEndpointService != null) {
                ArchiveType type = getType();

                if (type == ArchiveType.EJB_MODULE) {
                    //for an ejb module, simply add all the ejb descriptors to the bda
                    EndPointsInfo endPointsInfo = ejbEndpointService.getEndPointsInfo(archive);
                    addManagedBeanDescriptors(endPointsInfo.getManagedBeanDescriptors());
                    addEjbDescriptors(endPointsInfo.getEJBDescriptors());
                    nonCDIInterceptors.addAll(endPointsInfo.getNonCDIInterceptors());

                } else if (type == ArchiveType.WEB_MODULE) {
                    //for a web module, the managed beans descriptors might live on the child bdas so we need to find the right one
                    EndPointsInfo endPointsInfo = ejbEndpointService.getEndPointsInfo(archive);
                    Collection<EjbDescriptor<?>> ejbDescriptors = endPointsInfo.getEJBDescriptors();

                    Collection<ManagedBeanDescriptor<?>> managedBeanDescriptors = endPointsInfo.getManagedBeanDescriptors();

                    //add managed bean descriptor to the correct archive
                    for (ManagedBeanDescriptor<?> managedBeanDescriptor : managedBeanDescriptors) {
                        boolean added = false;
                        for (WebSphereBeanDeploymentArchive child : getWebSphereBeanDeploymentArchives()) {
                            if (child.getAllClazzes().contains(managedBeanDescriptor.getBeanClass().getName())) {
                                Class<?> clazz = CDIUtils.loadClass(classloader, managedBeanDescriptor.getBeanClass().getName());
                                if (clazz == managedBeanDescriptor.getBeanClass()) {

                                    child.addManagedBeanDescriptor(managedBeanDescriptor);
                                    added = true;
                                    break;
                                }
                            }
                        }
                        if (!added) {
                            addManagedBeanDescriptor(managedBeanDescriptor);
                        }
                    }
                    //add ejb descriptor to the correct archive
                    for (EjbDescriptor<?> ejbDescriptor : ejbDescriptors) {
                        boolean added = false;
                        for (WebSphereBeanDeploymentArchive child : getWebSphereBeanDeploymentArchives()) {
                            if (child.getAllClazzes().contains(ejbDescriptor.getBeanClass().getName())) {
                                Class<?> clazz = CDIUtils.loadClass(classloader, ejbDescriptor.getBeanClass().getName());
                                if (clazz == ejbDescriptor.getBeanClass()) {
                                    child.addEjbDescriptor(ejbDescriptor);
                                    added = true;
                                    break;
                                }
                            }
                        }
                        if (!added) {
                            addEjbDescriptor(ejbDescriptor);
                        }
                    }
                    nonCDIInterceptors.addAll(endPointsInfo.getNonCDIInterceptors());
                }
            }
        }
    }

    /**
     * Get the set of classes managed by the client archive.
     * <p>
     * This is needed so that we don't try and do injection on any classes which are managed by the client archive.
     * <p>
     * Must only be called if this bean archive is a client application jar.
     *
     * @return The set of classes which are managed by the client archive
     */
    private Map<String, Class<?>> getClientContainerManagedClasses() throws CDIException {

        Map<String, Class<?>> clientClasses = null;
        if (archive.getType() == ArchiveType.CLIENT_MODULE) {
            Set<String> classNames = new HashSet<String>();

            String appMainClassName = null;
            String appCallbackHandlerName = null;

            appMainClassName = archive.getClientModuleMainClass();
            appCallbackHandlerName = archive.getClientAppCallbackHandlerName();

            if (appMainClassName != null) {
                classNames.add(appMainClassName);
            }
            if (appCallbackHandlerName != null) {
                classNames.add(appCallbackHandlerName);
            }

            clientClasses = CDIUtils.loadClasses(archive.getClassLoader(), classNames);
        }

        return clientClasses;
    }

    //scan() must be called first
    @Override
    public ReferenceContext initializeInjectionServices() throws CDIException {
        Set<Class<?>> injectionClasses = getInjectionClasses();
        ReferenceContext referenceContext = archive.getReferenceContext(injectionClasses);

        return referenceContext;
    }

    /**
     * Need to removed the vetoed classes from the set so that we can ignore them
     *
     * @param classes
     */
    private void removeVetoedClasses(Set<Class<?>> classes) {
        //get hold of classnames
        Set<String> classNames = new HashSet<String>();
        for (Class<?> clazz : classes) {
            classNames.add(clazz.getName());
        }
        // take into considerations of the exclude in beans.xml
        Collection<String> includedClasses = WeldCDIUtils.filterClassesBasedOnBeansXML(this.beansXml, this.resourceLoader, classNames);
        Iterator<Class<?>> iterator = classes.iterator();
        while (iterator.hasNext()) {
            Class<?> clazz = iterator.next();
            if (WeldCDIUtils.isClassVetoed(clazz)) {
                iterator.remove();
            } else if (!includedClasses.contains(clazz.getName())) {
                iterator.remove();
            }

        }
    }

    @Override
    public Set<Class<?>> getInjectionClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>(this.injectionClasses);

        for (WebSphereBeanDeploymentArchive bda : descendantBDAs) {
            classes.addAll(bda.getInjectionClasses());
        }
        return classes;
    }

    @Override
    public Set<Class<?>> getJEEComponentClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>(this.jeeComponentClasses);

        for (WebSphereBeanDeploymentArchive bda : descendantBDAs) {
            classes.addAll(bda.getJEEComponentClasses());
        }
        return classes;
    }

    @Override
    public boolean hasBeans() {
        return hasBeans;
    }

    @Override
    @Trivial
    // This is marked trivial because it's called when iterating over all BDAs to look
    // for a single class which causes lots of noise in the trace without adding any value
    public Set<String> getAllClazzes() {
        return allClasses;
    }

    @Override
    public Collection<String> getBeanClasses() {
        return beanClasses.keySet();
    }

    @Override
    public boolean containsBeanClass(Class<?> beanClass) {
        //check to see whether it contains this class
        boolean containsBeanClass = false;
        Class<?> localBeanClass = this.beanClasses.get(beanClass.getName());
        if (localBeanClass != null && beanClass.equals(localBeanClass)) {
            containsBeanClass = true;
        }

        return containsBeanClass;

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.jboss.weld.bootstrap.spi.BeanDeploymentArchive#getBeanDeploymentArchives
     * ()
     */
    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return new ArrayList<BeanDeploymentArchive>(accessibleBDAs);
    }

    @Override
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive accessibleBDA) {
        this.accessibleBDAs.add(accessibleBDA);
    }

    @Override
    public void addDescendantBda(WebSphereBeanDeploymentArchive descendantBda) {
        descendantBDAs.add(descendantBda);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.spi.BeanDeploymentArchive#getBeansXml()
     */
    @Override
    public BeansXml getBeansXml() {
        if (this.beansXml == null) {
            Resource beansXmlResource = archive.getBeansXml();
            if (beansXmlResource != null) {
                URL beansXmlUrl = beansXmlResource.getURL();
                Bootstrap bootstrap = getCDIDeployment().getBootstrap();
                final ClassLoader origTCCL = getContextClassLoader();
                try {
                    // Must use this class's loader as the context classloader to ensure
                    // that we load Liberty's XML parser rather than any parser defined
                    // in the application.
                    setContextClassLoader(BeanDeploymentArchiveImpl.class.getClassLoader());
                    beansXml = bootstrap.parse(beansXmlUrl);
                } finally {
                    setContextClassLoader(origTCCL);
                }
            }
        }
        return this.beansXml;
    }

    /**
     * Check whether the archive is an extension (application or runtime) or not
     *
     * @return
     */
    @Override
    public boolean isExtension() {
        if (isExtension == null) {
            if (archive.getType() == ArchiveType.ON_DEMAND_LIB) {
                isExtension = false;
            } else {
                Set<String> extensionClazz = archive.getExtensionClasses();
                isExtension = extensionClazz.isEmpty() ? false : true;
            }
        }
        return isExtension;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.spi.BeanDeploymentArchive#getEjbs()
     */
    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return ejbDescriptors;
    }

    private Set<Class<?>> getEJBClasses() {
        return this.ejbClasses;
    }

    private Set<Class<?>> getManagedBeanClasses() {
        return this.managedBeanClasses;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.spi.BeanDeploymentArchive#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.spi.BeanDeploymentArchive#getServices()
     */
    @Override
    public ServiceRegistry getServices() {
        return weldServiceRegistry;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Trivial
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BeanDeploymentArchiveImpl other = (BeanDeploymentArchiveImpl) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public CDIArchive getArchive() {
        return archive;
    }

    /**
     * @return the beanManager
     */

    @Override
    public WeldManager getBeanManager() {
        return bootstrap.getManager(this);
    }

    @Override
    public String toString() {
        return "BDA for " + id + "(" + archive.getType() + ")";
    }

    @Override
    public WebSphereCDIDeployment getCDIDeployment() {
        return cdiDeployment;
    }

    @Override
    public boolean extensionCanSeeApplicationBDAs() {
        return archive.getType() == ArchiveType.RUNTIME_EXTENSION && extensionCanSeeApplicationBDAs;
    }

    @Override
    public Set<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives() {
        return accessibleBDAs;
    }

    @Override
    public Set<WebSphereBeanDeploymentArchive> getDescendantBdas() {
        return descendantBDAs;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public void addEjbDescriptors(Collection<EjbDescriptor<?>> ejbDescriptors) {
        for (EjbDescriptor<?> ejbDescriptor : ejbDescriptors) {
            addEjbDescriptor(ejbDescriptor);
        }
    }

    @Override
    public void addManagedBeanDescriptors(Collection<ManagedBeanDescriptor<?>> managedBeanDescriptors) {
        for (ManagedBeanDescriptor<?> managedBeanDescriptor : managedBeanDescriptors) {
            addManagedBeanDescriptor(managedBeanDescriptor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEjbDescriptor(EjbDescriptor<?> ejbDescriptor) {
        if (getBeanDiscoveryMode() != BeanDiscoveryMode.NONE) {
            this.ejbDescriptors.add(ejbDescriptor);
            Class<?> beanClass = ejbDescriptor.getBeanClass();
            this.beanClasses.put(beanClass.getName(), beanClass);
            this.ejbClasses.add(beanClass);
            Set<EjbDescriptor<?>> ejbDescriptors = ejbDescriptorMap.get(beanClass);
            if (ejbDescriptors == null) {
                ejbDescriptors = new HashSet<EjbDescriptor<?>>();
            }
            ejbDescriptors.add(ejbDescriptor);
            this.ejbDescriptorMap.put(beanClass, ejbDescriptors);
            this.hasBeans = true;
        }
    }

    @Override
    public void addManagedBeanDescriptor(ManagedBeanDescriptor<?> managedBeanDescriptor) {
        if (getBeanDiscoveryMode() != BeanDiscoveryMode.NONE) {
            this.managedBeanClasses.add(managedBeanDescriptor.getBeanClass());
            if (managedBeanDescriptor.getBeanClass() == null) { 
                Tr.debug(tc, "Managed bean descriptor's bean class was null", managedBeanDescriptor);
            }
        }
    }

    @Override
    public CDIRuntime getCDIRuntime() {
        return cdiRuntime;
    }

    /** {@inheritDoc} */
    @Override
    public void addToBeanClazzes(Class<?> clazz) {
        this.beanClasses.put(clazz.getName(), clazz);
        this.allClasses.add(clazz.getName());
    }

    @Override
    public void createInjectionTargetsForJEEComponentClasses() throws CDIException {
        Set<Class<?>> jeeComponentClasses = getJEEComponentClasses();

        //jeeComponentClasses is per module but we have multiple bdas per module
        //so if the class is not directly in this bda, check the descendant bdas
        for (Class<?> clazz : jeeComponentClasses) {
            if (allClasses.contains(clazz.getName())) {
                createInjectionTargetsForJEEComponentClass(clazz);
            } else {
                for (WebSphereBeanDeploymentArchive child : getDescendantBdas()) {
                    if (child.getAllClazzes().contains(clazz.getName())) {
                        child.createInjectionTargetsForJEEComponentClass(clazz);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void createInjectionTargetsForJEEComponentClass(Class<?> clazz) throws CDIException {
        createJEEComponentInjectionTarget(clazz);

        //if this is the main class of a client module then we also need to create IPs for static fields
        if (archive.getType() == ArchiveType.CLIENT_MODULE) {
            String mainClassName = archive.getClientModuleMainClass();
            if (clazz.getName().equals(mainClassName)) {
                createStaticFieldInjectionPoints(clazz);
            }
        }
    }

    private <T, X> InjectionPoint fireProcessInjectionPoint(Class<T> clazz, AnnotatedField<X> field) {
        BeanManagerImpl beanManager = (BeanManagerImpl) getBeanManager();
        InjectionPointFactory factory = InjectionPointFactory.instance();
        EnhancedAnnotatedField<T, X> enhancedField = beanManager.getServices().get(MemberTransformer.class).loadEnhancedMember(field, getId());
        FieldInjectionPoint<T, X> injectionPoint = factory.createFieldInjectionPoint(enhancedField, null, clazz, beanManager);
        return injectionPoint;
    }

    private <X> void createStaticFieldInjectionPoints(Class<X> clazz) {
        WeldManager beanManager = getBeanManager();
        AnnotatedType<X> annotatedType = beanManager.createAnnotatedType(clazz);

        Set<AnnotatedField<? super X>> fields = annotatedType.getFields();
        for (AnnotatedField<?> field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (field.isStatic()) {
                    InjectionPoint injectionPoint = fireProcessInjectionPoint(clazz, field);

                    List<InjectionPoint> injectionPoints = this.staticInjectionPoints.get(clazz);
                    if (injectionPoints == null) {
                        injectionPoints = new ArrayList<InjectionPoint>();
                        this.staticInjectionPoints.put(clazz, injectionPoints);
                    }
                    injectionPoints.add(injectionPoint);
                }
            }
        }

    }

    private <T> void createJEEComponentInjectionTarget(Class<T> clazz) {
        WeldManager beanManager = getBeanManager();
        AnnotatedType<T> annotatedType = null;
        try {
            annotatedType = beanManager.createAnnotatedType(clazz);
        } catch (ResourceLoadingException rle) {
            //noop
            //ignore if the JavaEE component class cannot be loaded. It is possible it is not used in the application
            //but Web Container added it in.

        }
        if (annotatedType != null) {
            try {
                // If the JavaEE component classes is non cdi interceptor, we need to call a different api to create
                // injection target
                InjectionTarget<T> injectionTarget;
                if (nonCDIInterceptors.contains(clazz)) {
                    WeldInjectionTargetFactory<T> weldInjectionTargetFactory = getBeanManager().getInjectionTargetFactory(annotatedType);
                    InjectionTarget<T> it = weldInjectionTargetFactory.createInterceptorInjectionTarget();
                    injectionTarget = beanManager.fireProcessInjectionTarget(annotatedType, it);
                } else {
                    injectionTarget = beanManager.fireProcessInjectionTarget(annotatedType);
                }
                //call a different api for creating interceptor

                this.injectionTargets.put(clazz, injectionTarget);
            } catch (IllegalArgumentException e) {
                throw new DefinitionException(e);
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> InjectionTarget<T> getJEEComponentInjectionTarget(Class<T> clazz) {
        return (InjectionTarget<T>) this.injectionTargets.get(clazz);
    }

    /**
     * {@inheritDoc}
     *
     * @param <T>
     * @throws CDIException
     */
    @Override
    public <T> List<InjectionPoint> getJEEComponentInjectionPoints(Class<T> clazz) throws CDIException {
        InjectionTarget<T> injectionTarget = getJEEComponentInjectionTarget(clazz);
        List<InjectionPoint> injectionPoints = null;

        if (injectionTarget != null) {
            injectionPoints = new ArrayList<InjectionPoint>();
            Set<InjectionPoint> jeeClassInjectionPoints = injectionTarget.getInjectionPoints();
            if (jeeClassInjectionPoints != null && jeeClassInjectionPoints.size() > 0) {
                injectionPoints.addAll(jeeClassInjectionPoints);
            }

            String mainClassName = archive.getClientModuleMainClass();

            if (mainClassName != null) {
                if (clazz.getName().equals(mainClassName)) {
                    List<InjectionPoint> staticInjectionPoints = this.staticInjectionPoints.get(clazz);
                    if (staticInjectionPoints != null) {
                        injectionPoints.addAll(staticInjectionPoints);
                    }
                }
            }
        }

        return injectionPoints;
    }

    @Override
    public <T> void addJEEComponentInjectionTarget(Class<T> clazz, InjectionTarget<T> injectionTarget) {
        this.injectionTargets.put(clazz, injectionTarget);
    }

    @Override
    public boolean hasBeenScanned() {
        return scanned;
    }

    /** {@inheritDoc} */
    @Override
    public ArchiveType getType() {
        return archive.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader() {
        return archive.getClassLoader();
    }

    /** {@inheritDoc} */
    @Override
    public Set<EjbDescriptor<?>> getEjbDescriptor(Class<?> clazz) {

        return ejbDescriptorMap.get(clazz);
    }

    /** {@inheritDoc} */
    @Override
    public String getEEModuleDescriptorId() {
        return eeModuleDescriptor.getId();
    }

    /**
     * This method is used by Weld 3 for performance optimisation
     *
     * @return all bean classes
     */
    public Collection<Class<?>> getLoadedBeanClasses() {
        return this.beanClasses.values();
    }

    /**
     * This method is used by Weld 3 for performance optimisation
     *
     * @return all classes in the archive
     */
    public Collection<String> getKnownClasses() {
        return getAllClazzes();
    }

    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static void setContextClassLoader(final ClassLoader newLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(newLoader);
                return null;
            }
        });
    }
}
