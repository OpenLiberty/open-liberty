/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
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
     */
    ClassLoader getClassLoader();

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
     * @return a Resource, or {@code null} if there is no such resource
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
     * @return the parent Application, or {@code null} if there isn't one
     */
    Application getApplication();

    /**
     * If this archive is a Client Module, get the name of the Main Class.
     *
     * @return the name of the Client Module Main Class, or {@code null} if this is not a client module
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
     * <p>
     * Must not be called on non-application archives.
     *
     * @return the MetaData
     * @throws CDIException
     */
    MetaData getMetaData() throws CDIException;

    /**
     * Get all bindings from the ibm-managed-bean-bnd.xml file in this archive. May be {@code null} for non-application archives.
     *
     * @param resourceRefConfigFactory
     * @return the bindings, or {@code null}
     * @throws CDIException
     */
    ResourceInjectionBag getAllBindings() throws CDIException;

    /**
     * If this archive is a Client Module, get the name of the Application Callback Handler.
     *
     * @return the application callback handler, or {@code null} if the archive is not a client module
     * @throws CDIException
     */
    String getClientAppCallbackHandlerName() throws CDIException;

    /**
     * Get the relative path of this archive. Should be unique within the application.
     * <p>
     * May be {@code null} for objects which don't correspond to a physical archive.
     *
     * @return the path of the archive, or {@code null}
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
     * @return a resource for the beans.xml, or {@code null} if there is no beans.xml
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
     * Get the build compatible extension classes found in this archive
     * <p>
     * Always an empty set on CDI &lt; 4.0
     *
     * @return the BCE class names
     */
    Set<String> getBuildCompatibleExtensionClasses();

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
     *
     * @return the CDIRuntime
     */
    CDIRuntime getCDIRuntime();

    /**
     * Return the reference context for this archive
     * <p>
     * Must not be called on non-application archives.
     *
     * @return the reference context
     * @throws CDIException
     */
    ReferenceContext getReferenceContext() throws CDIException;
}
