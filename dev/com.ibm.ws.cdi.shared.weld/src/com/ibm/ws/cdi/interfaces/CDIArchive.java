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
package com.ibm.ws.cdi.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.impl.ResourceInjectionBag;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public interface CDIArchive {

    /**
     * Get the J2EEName of the archive if it has one. Generally, modules have a J2EEName but Libraries do not.
     *
     * @return the J2EEName or null
     * @throws CDIException
     */
    J2EEName getJ2EEName() throws CDIException;

    /**
     * Get the name of the archive
     *
     * @return the name of the archive
     */
    String getName();

    /**
     * Get the type of the archive
     *
     * @return the archive type
     */
    ArchiveType getType();

    /**
     * Get the ClassLoader to use when loading classes contained in this archive.
     * Note that this may be unique to the archive or it may be shared with others.
     *
     * @return the ClassLoader
     * @throws CDIException
     */
    ClassLoader getClassLoader() throws CDIException;

    /**
     * Get the names of all of the classes contained in this archive
     *
     * @return the class names
     * @throws CDIException
     */
    Set<String> getClassNames() throws CDIException;

    /**
     * Get a specific resource from this archive
     *
     * @param path the relative path of the resource within the archive
     * @return a Resource
     */
    Resource getResource(String path);

    /**
     * Return true if the archive represents a module
     *
     * @return true if the archive represents a module
     */
    boolean isModule();

    /**
     * Get the Application which contains this Archive. May be null in the case of Archives created for Runtime Extensions.
     *
     * @return the parent Application
     */
    Application getApplication();

    /**
     * If this archive is a Client Module, get the name of the Main Class.
     *
     * @return the name of the Client Module Main Class
     * @throws CDIException
     */
    String getClientModuleMainClass() throws CDIException;

    /**
     * Get a list of the classes in this container which support JEE Injection
     *
     * @return a list of the classes in this container which support JEE Injection
     * @throws CDIException
     */
    List<String> getInjectionClassList() throws CDIException;

    /**
     * Get the MetaData for this archive.
     *
     * @return
     * @throws CDIException
     */
    MetaData getMetaData() throws CDIException;

    /**
     * Get all bindings from the ibm-managed-bean-bnd.xml file in this archive
     *
     * @param resourceRefConfigFactory
     * @return
     * @throws CDIException
     */
    ResourceInjectionBag getAllBindings() throws CDIException;

    /**
     * If this archive is a Client Module, get the name of the Application Callback Handler.
     *
     * @return the application callback handler
     * @throws CDIException
     */
    String getClientAppCallbackHandlerName() throws CDIException;

    /**
     * Get the relative path of this archive. Should be unique within the application.
     *
     * @return the path of the archive
     * @throws CDIException
     */
    String getPath() throws CDIException;

    /**
     * Get all of the module archives within the application
     *
     * @return all of the module archives
     * @throws CDIException
     */
    Collection<CDIArchive> getModuleLibraryArchives() throws CDIException;

    /**
     * Get all the bean defining annotations in the archive
     *
     * @return the names of bean defining annotations
     * @throws CDIException
     */
    Set<String> getBeanDefiningAnnotations() throws CDIException;

    /**
     * Get any classes which are annotated with the given annotations. This includes classes where a super-class has been annotated.
     *
     * @param annotations The annotations to look for.
     * @return The names of classes with the given annotations.
     * @throws CDIException
     */
    Set<String> getAnnotatedClasses(Set<String> annotations) throws CDIException;

    /**
     * Get a Resource which represents the beans.xml file in the archive
     *
     * @return a resource
     */
    Resource getBeansXml();

    /**
     * Get the classes named in the META-INF/services/javax.enterprise.inject.spi.Extension file
     *
     * @return the extension class names
     * @throws CDIException
     */
    Set<String> getExtensionClasses();

    /**
     * Initialize an Injection Engine Reference Context using the given injection classes
     *
     * @param injectionClasses The classes to initialize the Reference Context with
     * @return the initialized Reference Context
     * @throws CDIException
     */
    ReferenceContext getReferenceContext(Set<Class<?>> injectionClasses) throws CDIException;

    /**
     * Get the CDIRuntime associated with this Archive
     */
    CDIRuntime getCDIRuntime();

    /**
     * Return the reference context for this archive
     *
     * @return
     * @throws CDIException
     */
    ReferenceContext getReferenceContext() throws CDIException;
}
