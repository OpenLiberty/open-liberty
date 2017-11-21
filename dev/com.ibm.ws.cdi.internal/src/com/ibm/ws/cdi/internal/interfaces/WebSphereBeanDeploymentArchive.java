/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import com.ibm.ws.cdi.CDIException;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * WebSphere specific extensions to the BDA interface
 */
public interface WebSphereBeanDeploymentArchive extends BeanDeploymentArchive {

    /**
     * Add a BDA to this one as being accessible. All accessible BDAs should be added.
     * Loops are possible and will be handled by Weld.
     *
     * @param accessibleBDA The BDA which is accessible by this one
     */
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive accessibleBDA);

    /**
     * Add a BDA to this one as a descendant (i.e. either a direct or an indirect child). Only Web, EJB and client modules should have descendants.
     * <p>
     * While a BDA can be accessible to multiple other BDAs, it should only be added as a descendant of a single parent module.
     * <p>
     * If you're calling this, you probably also want to add the same BDA using {@link #addBeanDeploymentArchive(WebSphereBeanDeploymentArchive)}
     *
     * @param descendantBda The BDA which is a descendant of this one
     */
    public void addDescendantBda(WebSphereBeanDeploymentArchive descendantBda);

    /**
     * Add an EjbDescriptor to this BDA. Note that the class returned by EjbDescriptor.getBeanClass()
     * should also be in the list of bean classes known about by this BDA. @see getBeanClazzes()
     *
     * @param ejbDescriptor An EjbDescriptor that describes an ejb endpoint
     */
    public void addEjbDescriptor(EjbDescriptor<?> ejbDescriptor);

    /**
     * Add multiple EjbDescriptors. @see addEjbDescriptor(EjbDescriptor<?>)
     *
     * @param ejbDescriptors A collection of EjbDescriptors to add
     */
    public void addEjbDescriptors(Collection<EjbDescriptor<?>> ejbDescriptors);

    /**
     * Get the parent WebSphereCDIDeployment of which this BDA is a part. There is currently one Deployment
     * per Application.
     *
     * @return the owning WebSphereCDIDeployment
     */
    public WebSphereCDIDeployment getCDIDeployment();

    /**
     * Add the specified class to the bean class set
     *
     * @param clazz
     */
    void addToBeanClazzes(Class<?> clazz);

    /**
     * Returns true if and only if the specified class is contained in this BeanDeploymentArchive
     */
    public boolean containsClass(Class<?> clazz);

    /**
     * Get the BeanManager for this BDA
     *
     * @return This BDA's BeanManager
     */
    public BeanManager getBeanManager();

    /**
     * Get the set of BDAs which are accessible by this BDA. This is the same set as
     * returned by {@link BeanDeploymentArchive.getBeanDeploymentArchives()}
     *
     * @return the set of accessible BDAs
     */
    public Set<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives();

    /**
     * Get the set of BDAs which are descendants of this BDA.
     * <p>
     * Only client, web or EJB modules should have descendant BDAs which are the libraries referenced from their manifest or included in WEB-INF/lib.
     *
     * @return the set of descendant BDAs
     */
    public Set<WebSphereBeanDeploymentArchive> getDescendantBdas();

    /**
     * Get the instance of CDIRuntime
     *
     * @return an instance of CDIRuntime
     */
    public CDIRuntime getCDIRuntime();

    /**
     * Get the type of this BDA.
     *
     * @return the type of this BDA
     */
    public ArchiveType getType();

    /**
     * Does this BDA have any bean classes. This does not include accessible BDAs.
     * It should only be called after scanForBeans().
     *
     * @return true if this BDA has at least one bean
     */
    public boolean hasBeans();

    /**
     * Initialize the WebSphere Injection Services with the bean classes known about by this BDA (see getInjectionClasses).
     * This should only be called after scan().
     *
     * @throws CDIException
     */
    public ReferenceContext initializeInjectionServices() throws CDIException;

    /**
     * Get the classes which should be passed to the injection services for this BDA. This will include the classes for
     * descendant BDAs which are of type Manifest Classpath or WEB-INF lib.
     *
     * @return The set of all injection classes for this BDA.
     */
    public Set<Class<?>> getInjectionClasses();

    /**
     * Scan for bean defining annototaions in this BDA or those accessible to it.
     * BDAs with a scan mode set to "none" will be ignored.
     *
     * Bean defining annotations are a fixed set {@link ScannerConstants.BEAN_DEFINING_ANNOTATIONS} plus any custom annotations that are meta-annotated with
     * {@link ScannerConstants.BEAN_DEFINING_META_ANNOTATIONS}
     *
     * @param scanChildren if true look in the accessible BDAs as well
     * @return the complete set of bean defining annotations for this bda
     * @throws CDIException
     */
    public Set<String> scanForBeanDefiningAnnotations(boolean scanChildren) throws CDIException;

    /**
     * Scan for bean classes and EJB Endpoints in this BDA. If the scan mode is "all" then this will return all classes in
     * the BDA. If the mode is "annotated" then it will only return classes with bean defining annotations.
     * If the mode is "none" then an empty set will be returned.
     *
     * This may result in EjbEndpoints being added to accessible BDAs as well since the EJBEndpoint service works on a per module basis
     * rather than per archive.
     *
     * @throws CDIException
     */
    public void scan() throws CDIException;

    /**
     * Get human readable identifier string for this BDA
     *
     * @return a string containing the BDA id.
     */
    @Override
    public String toString();

    /**
     * @return true if this is a runtime extension BDA that can see the application BDAs
     */
    boolean extensionCanSeeApplicationBDAs();

    /**
     * Create and Validate InjectionTargets for all JEE Component Classes
     *
     * @throws CDIException
     */
    void createInjectionTargetsForJEEComponentClasses() throws CDIException;

    /**
     * Create and Validate InjectionTargets for specified JEE Component Class
     *
     * @param clazz
     * @throws CDIException
     */
    void createInjectionTargetsForJEEComponentClass(Class<?> clazz) throws CDIException;

    /**
     * Get the list of Injection Points for the given JEE Component Class.
     * Note that although this returns a List, the current implementation means
     * that the InjectionPoints are not ordered.
     *
     * @param <T>
     *
     * @param clazz The JEE Component Class
     * @return A List of InjectionPoints
     * @throws CDIException
     */
    <T> List<InjectionPoint> getJEEComponentInjectionPoints(Class<T> clazz) throws CDIException;

    /**
     * Get the Injection Target for the given Class
     *
     * @param clazz The Class
     * @return An Injection Target
     */
    <T> InjectionTarget<T> getJEEComponentInjectionTarget(Class<T> clazz);

    /**
     * Add a new Injection Target
     *
     * @param injectionTarget The Injection Target
     */
    <T> void addJEEComponentInjectionTarget(Class<T> clazz, InjectionTarget<T> injectionTarget);

    /**
     * @param beanClass
     * @return
     */
    public boolean containsBeanClass(Class<?> beanClass);

    /**
     * @return whether the bean archive has been scanned
     */
    boolean hasBeenScanned();

    /**
     * @return
     */
    public Set<Class<?>> getJEEComponentClasses();

    /**
     * @param managedBeanDescriptor
     */
    void addManagedBeanDescriptor(ManagedBeanDescriptor<?> managedBeanDescriptor);

    /**
     * @param managedBeanDescriptors
     */
    void addManagedBeanDescriptors(Collection<ManagedBeanDescriptor<?>> managedBeanDescriptors);

    /**
     * @return the archive which backs this BDA
     */
    CDIArchive getArchive();

    /**
     * @return the classloader which should be used to load classes from this BDA
     * @throws CDIException
     */
    public ClassLoader getClassLoader() throws CDIException;

    /**
     * @return whether this archive is an extension
     */
    boolean isExtension();

    /**
     * Get hold of the set of EjbDescriptors for the given session bean
     *
     * @param clazz the ejb class
     * @return
     */
    Set<EjbDescriptor<?>> getEjbDescriptor(Class<?> clazz);

    /**
     * Get hold of the EEModuleDescriptorID for this module
     *
     * @return the the EEModuleDescriptorID
     */
    String getEEModuleDescriptorId();

}
