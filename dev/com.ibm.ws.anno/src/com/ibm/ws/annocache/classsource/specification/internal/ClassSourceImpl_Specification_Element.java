/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.classsource.specification.internal;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Element;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;
import com.ibm.wsspi.annocache.util.Util_RelativePath;

public class ClassSourceImpl_Specification_Element implements ClassSource_Specification_Element {

    public static final String CLASS_NAME = ClassSourceImpl_Specification_Element.class.getSimpleName();

    //

    public ClassSourceImpl_Specification_Element(
        String name, ScanPolicy policy,
        Util_RelativePath relativePath) {

    	this(name, policy, relativePath, ClassSource.NO_ENTRY_PREFIX);
    }

    public ClassSourceImpl_Specification_Element(
        String name, ScanPolicy policy,
        Util_RelativePath relativePath, String entryPrefix) {

        this.name = name;
        this.policy = policy;

        this.relativePath = relativePath;
        this.entryPrefix = entryPrefix;
    }

    //

    protected final String name;
    protected final ScanPolicy policy;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ScanPolicy getPolicy() {
        return policy;
    }

    //

    protected final Util_RelativePath relativePath;
    protected final String entryPrefix;

    @Override
    public Util_RelativePath getPath() {
        return relativePath;
    }

    @Override
    public String getEntryPrefix() {
        return entryPrefix;
    }

    //

    @Override
    public void addTo(ClassSource_Aggregate rootClassSource) throws ClassSource_Exception {
        ClassSource_Factory classSourceFactory = rootClassSource.getFactory();
        Util_InternMap internMap = rootClassSource.getInternMap();

        String useName = getName();
        ScanPolicy usePolicy = getPolicy();

        Util_RelativePath usePath = getPath();
        if ( usePath == null ) {
            throw new IllegalArgumentException("Class source specification [ " + useName + " ] [ " + usePolicy + " ] has neither a class loader nor a path");
        }

        String useEntryPrefix = getEntryPrefix();

        Util_Factory utilFactory = classSourceFactory.getUtilFactory();
        String dirPath = utilFactory.denormalize(usePath.n_getFullPath());

        final File dir = new File(dirPath);
        Boolean dirIsFile = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf( dir.exists() && dir.isFile() );
            }
        });

        ClassSource classSource;

        if ( dirIsFile.booleanValue() ) {
            classSource = classSourceFactory.createJarClassSource(internMap, useName, dirPath, useEntryPrefix);
                // throws ClassSource_Exception

        } else {
            classSource = classSourceFactory.createDirectoryClassSource(internMap, useName, dirPath, useEntryPrefix);
            // throws ClassSource_Exception
        }

        rootClassSource.addClassSource(classSource, usePolicy);
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        String useName = getName();
        ScanPolicy usePolicy = getPolicy();

        Util_RelativePath usePath = getPath();
        String useEntryPrefix = getEntryPrefix();
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Class Source [ {0} ] [ {1} ]: [ {2} ] [ {3} ] Prefix [ {4} ]",
            new Object[] { useName, usePolicy, usePath.n_getBasePath(), usePath.n_getRelativePath(), useEntryPrefix });
    }
}
