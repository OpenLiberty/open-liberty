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
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;

import com.ibm.ws.cdi.CDIException;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public interface WebSphereCDIDeployment extends CDI11Deployment {

    /**
     * Get the Resource Injection Service for this deployment
     *
     * @return a ResourceInjectionServiceImpl
     */
    public WebSphereInjectionServices getInjectionServices();

    /**
     * Set the top level ClassLoader for this deployment. If the deployment represents an EAR then it will be the
     * EAR's classloader. If it is a standalone WAR then it will be the WAR's classloader.
     *
     * @param classloader The top level classloader
     */
    public void setClassLoader(ClassLoader classloader);

    /**
     * Get the top level ClassLoader for this deployment.
     *
     * @return the top level ClassLoader
     */
    public ClassLoader getClassLoader();

    /**
     * Get the WeldBootstrap for this deployment
     *
     * @return the WeldBootstrap
     */
    public WeldBootstrap getBootstrap();

    /**
     * Get the unique id for this deployment. At the moment this is the same as the J2EE name for the application.
     *
     * @return the unique deployment id
     */
    public String getDeploymentID();

    /**
     * Find a BDA by it's unique archive id. @see BeanDeploymentArchive.getId()
     *
     * @param archiveID the archive id
     * @return a WebSphereBeanDeploymentArchive with that id
     */
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchive(String archiveID);

    /**
     * <p>
     * Similar to <code>getBeanDeploymentArchive(Class<?> beanClass)</code>, this method returns
     * the {@link WebSphereBeanDeploymentArchive} containing the given class.
     * The difference is that the getBeanDeploymentArchive method will only return the BDA if
     * the passed-in class is a bean class. This method will return the BDA if the passed-in
     * class is in the archive in any form.
     * </p>
     *
     *
     * @param clazz the class
     * @return the {@link WebSphereBeanDeploymentArchive} containing the bean class or null if no such {@link WebSphereBeanDeploymentArchive} exists
     */
    public WebSphereBeanDeploymentArchive getBeanDeploymentArchiveFromClass(Class<?> clazz);

    /**
     * Get all BDAs relating only to this application. i.e. not Shared Libs or internal Runtime Extensions
     *
     * @return all application BDAs
     */
    public Collection<WebSphereBeanDeploymentArchive> getApplicationBDAs();

    /**
     * @return true if any part of the application is CDI enabled
     * @throws CDIException
     */
    public boolean isCDIEnabled();

    /**
     * Does the specified BDA, or any of BDAs accessible by it, have any beans or an extension which might add beans.
     *
     * @param bdaId the id of the BDA
     * @return true if the specified BDA, or any of BDAs accessible by it, have any beans
     * @throws CDIException
     */
    public boolean isCDIEnabled(String bdaId);

    /** {@inheritDoc} */
    @Override
    public String toString();

    /**
     * Add a BeanDeploymentArchive to this deployment
     *
     * @param bda the BDA to add
     * @throws CDIException
     */
    public void addBeanDeploymentArchive(WebSphereBeanDeploymentArchive bda) throws CDIException;

    /**
     * Add a Set of BDAs to the deployment
     *
     * @param bdas the BDAs to add
     * @throws CDIException
     */
    public void addBeanDeploymentArchives(Set<WebSphereBeanDeploymentArchive> bdas) throws CDIException;

    /**
     * Scan all the BDAs in the deployment to see if there are any bean classes or EJB Endpoints.
     *
     * This method must be called before we try to do any real work with the deployment or the BDAs
     *
     * @throws CDIException
     */
    public void scan() throws CDIException;

    /**
     * Initialize the Resource Injection Service with each BDA's bean classes.
     *
     * This method must be called after scanForBeans() and scanForEjbEndpoints() but before we try to do
     * any real work with the deployment or the BDAs
     *
     * @throws CDIException
     */
    public void initializeInjectionServices() throws CDIException;

    /**
     * Shutdown and clean up the whole deployment. The deployment will not be usable after this call has been made.
     */
    public void shutdown();

    /**
     * Same as Deployment.getBeanDeploymentArchives() except returns a collection of WebSphereBeanDeploymentArchive
     *
     * @return all the WebSphereBeanDeploymentArchives
     */
    public Collection<WebSphereBeanDeploymentArchive> getWebSphereBeanDeploymentArchives();

    /**
     * Validate all JEE Component Classes
     *
     * @throws CDIException
     */
    public void validateJEEComponentClasses() throws CDIException;

    /**
     * @param referenceContext
     */
    public void addReferenceContext(ReferenceContext referenceContext);

    /**
     * Provides access to the current container
     *
     * @return the CDI instance for the current container
     */
    public CDI<Object> getCDI();

    /**
     * Provides additional extensions added by the WebSphereCDIExtensionMetaData SPI class
     *
     * @param extensions a set of additional CDI extension classes.
     */
    void registerSPIExtension(Set<Extension> extensions);

}
