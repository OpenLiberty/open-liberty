/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.InputStream;
import java.net.URL;

import javax.xml.validation.Schema;

import org.xml.sax.SAXException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Persistence.xml handling class.
 */
public abstract class JPAPXml
{
    private static final TraceComponent tc = Tr.register(JPAPXml.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    private final JPAApplInfo ivApplInfo;

    // Jar name where this persistence.xml is defined.
    private String ivArchiveName;

    // Scope specified for this persistence.xml
    private JPAPuScope ivPuScope;

    // Classloader used for the persistence.xml
    private ClassLoader ivClassLoader;

    // URL of the root where the persistence.xml is defined. See JPA spec 6.2
    // The probable URL forms for the root URL are:
    //    jar:file://.../xxxx.ear/xxxx.jar!/                for ejb jar file
    //    file://.../xxxx.war/WEB-INF/classes/              for web app classes directory in WAR file
    //    jar:file://.../xxxx.war/WEB-INF/lib/xxxx.jar!/    for jars in web app library directory
    //    jar:file://.../xxxx.ear/xxxx.jar!/                for persistence archives in application(EAR) root
    //    jar:file://.../xxxx.ear/somelib/xxxx.jar!/        for persistence archives in application(EAR) library directory
    private URL ivRootUrl;

    /**
     * Constructor that initializes common state.
     * 
     * @param appName name of the application where this persistence.xml was found
     * @param archiveName name of the archive where this persistence.xml was found
     * @param scope scope that applies to all persistence units in the archive
     * @param puRoot root of the persistence.xml; location of META-INF directory
     * @param classloader ClassLoader for the archive
     */
    protected JPAPXml(JPAApplInfo applInfo, String archiveName, JPAPuScope scope, URL puRoot, ClassLoader classloader)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + applInfo.getApplName() + ", " + archiveName + ", " + scope + ", " + puRoot);

        ivApplInfo = applInfo;
        ivArchiveName = archiveName;
        ivPuScope = scope;
        ivClassLoader = classloader;
        ivRootUrl = puRoot;
    }

    JPAApplInfo getApplInfo()
    {
        return ivApplInfo;
    }

    /**
     * Returns the name of the containing archive relative to the root of
     * the application (EAR). <p>
     * 
     * For EJB and Web modules, this will be the name of the module; for
     * library jars, this will include the library directory. When located
     * within a jar in a Web module, this will be the name of the Web module.
     */
    String getArchiveName()
    {
        return ivArchiveName;
    }

    /*
     * Getter for this pu scope.
     */
    JPAPuScope getPuScope()
    {
        return ivPuScope;
    }

    /*
     * Getter for class loader.
     */
    public ClassLoader getClassLoader()
    {
        return ivClassLoader;
    }

    /*
     * Getter for this pu's root URL.
     */
    URL getRootURL()
    {
        return ivRootUrl;
    }

    protected abstract InputStream openStream()
                    throws java.io.IOException;

    /**
     * Creates a new schema for the specified persistence xsd.
     * 
     * @throws SAXException if an error occurs
     */
    // d727932.1
    protected abstract Schema newSchema(String xsdName)
                    throws SAXException;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('[').append(ivApplInfo.getApplName());
        sb.append(", ").append(ivArchiveName);
        sb.append(", ").append(ivPuScope);
        sb.append(']');
        return sb.toString();
    }
}
