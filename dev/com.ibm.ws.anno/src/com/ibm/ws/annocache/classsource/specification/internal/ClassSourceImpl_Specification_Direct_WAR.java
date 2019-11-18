/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.classsource.specification.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedDirectory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

public class ClassSourceImpl_Specification_Direct_WAR
    extends ClassSourceImpl_Specification_Direct
    implements ClassSource_Specification_Direct_WAR {

    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = ClassSourceImpl_Specification_Direct_WAR.class.getSimpleName();

    //

    public ClassSourceImpl_Specification_Direct_WAR(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super(factory, appName, modName, modCatName);

        this.ignoreClassesPath = false;
        this.classesPath = null; // The default will be used.

        this.ignoreLibPath = false;
        this.libPath = null; // The default will be used.

        this.libPaths = null; // Will be obtained from the listing of the lib path.
    }

    //

    protected boolean ignoreClassesPath;

    @Override
    @Trivial
    public boolean getIgnoreClassesPath() {
        return ignoreClassesPath;
    }

    @Override
    public void setIgnoreClassesPath(boolean ignoreClassesPath) {
        this.ignoreClassesPath = ignoreClassesPath;
    }

    @Override
    @Trivial
    public String getDefaultClassesPath() {
        return DEFAULT_CLASSES_PATH;
    }

    protected String classesPath;

    @Override
    @Trivial
    public String getClassesPath() {
        return classesPath;
    }

    @Override
    public void setClassesPath(String classesPath) {
        this.classesPath = classesPath;
    }

    //

    protected boolean ignoreLibPath;

    @Override
    @Trivial
    public boolean getIgnoreLibPath() {
        return ignoreLibPath;
    }

    @Override
    public void setIgnoreLibPath(boolean ignoreLibPath) {
        this.ignoreLibPath = ignoreLibPath;
    }

    @Override
    @Trivial
    public String getDefaultLibPath() {
        return DEFAULT_LIB_PATH;
    }

    protected String libPath;

    @Override
    @Trivial
    public String getLibPath() {
        return libPath;
    }

    @Override
    public void setLibPath(String libPath) {
        this.libPath = libPath;
    }

    protected List<String> libPaths;

    @Override
    @Trivial
    public List<String> getLibPaths() {
        return libPaths;
    }

    @SuppressWarnings("hiding")
    @Override
    public void addLibPath(String libPath) {
        addLibPath(libPath, IS_NOT_PARTIAL, IS_NOT_EXCLUDED);
    }

    @SuppressWarnings("hiding")
    @Override
    public void addLibPath(String libPath, boolean isPartial, boolean isExcluded) {
        if ( libPaths == null ) {
            libPaths = new ArrayList<String>();
        }

        libPaths.add(libPath);

        if ( isPartial ) {
            addPartialPath(libPath);
        } else if ( isExcluded ) {
            addExcludedPath(libPath);
        }
    }

    @SuppressWarnings("hiding")
	@Override
    public void addLibPaths(List<String> libPaths) {
        addLibPaths(libPaths, IS_NOT_PARTIAL, IS_NOT_EXCLUDED);
    }

    @SuppressWarnings("hiding")
    @Override
    public void addLibPaths(List<String> libPaths, boolean isPartial, boolean isExcluded) {
        if ( this.libPaths == null ) {
            this.libPaths = new ArrayList<String>();
        }

        this.libPaths.addAll(libPaths);

        if ( isPartial ) {
            addPartialPaths(libPaths);
        } else if ( isExcluded ) {
            addExcludedPaths(libPaths);
        }
    }

    private Set<String> partialPaths;

    @SuppressWarnings("hiding")
    @Override
    @Trivial
    public boolean isPartialPath(String libPath) {
        return ( (partialPaths != null) && partialPaths.contains(libPath) );
    }

    @SuppressWarnings("hiding")
    @Override
    public void addPartialPath(String libPath) {
        if ( partialPaths == null ) {
            partialPaths = new HashSet<String>();
        }
        partialPaths.add(libPath);
    }

    @SuppressWarnings("hiding")
    @Override
    public void addPartialPaths(Collection<String> libPath) {
        if ( partialPaths == null ) {
            partialPaths = new HashSet<String>();
        }
        partialPaths.addAll(libPaths);
    }
    
    private Set<String> excludedPaths;

    @SuppressWarnings("hiding")
    @Override
    @Trivial
    public boolean isExcludedPath(String libPath) {
        return ( (excludedPaths != null) && excludedPaths.contains(libPath) );
    }

    @SuppressWarnings("hiding")
    @Override
    public void addExcludedPath(String libPath) {
        if ( excludedPaths == null ) {
            excludedPaths = new HashSet<String>();
        }
        excludedPaths.add(libPath);
    }

    @SuppressWarnings("hiding")
    @Override
    public void addExcludedPaths(Collection<String> libPath) {
        if ( excludedPaths == null ) {
            excludedPaths = new HashSet<String>();
        }
        excludedPaths.addAll(libPaths);
    }

    @SuppressWarnings("hiding")
    @Trivial
    public ScanPolicy selectPolicy(String libPath) {
        if ( isPartialPath(libPath) ) {
            return ScanPolicy.PARTIAL;
        } else if ( isExcludedPath(libPath) ) {
            return ScanPolicy.EXCLUDED;
        } else {
            return ScanPolicy.SEED;
        }
    }

    //

    @Override
    public void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        if ( !getIgnoreClassesPath() ) {
            String useClassesPath = getClassesPath();

            if ( useClassesPath == null ) {
                useClassesPath = getDefaultClassesPath();

                @SuppressWarnings("unused")
                ClassSourceImpl_MappedDirectory classesClassSource = addDirectoryClassSource(
                    rootClassSource,
                    "WEB-INF/classes", getModulePath(), getDefaultClassesPath(),
                    ScanPolicy.SEED);
                // throws ClassSource_Exception

            } else {
                @SuppressWarnings("unused")
                ClassSourceImpl_MappedDirectory classesClassSource = addDirectoryClassSource(
                    rootClassSource,
                    "WEB-INF/classes", useClassesPath,
                    ScanPolicy.SEED);
                // throws ClassSource_Exception
            }
        }

        if ( !getIgnoreLibPath() ) {
            List<String> useLibPaths = getLibPaths();
        
            String useModulePath = getModulePath();

            if ( useLibPaths == null ) {
                String useLibPath = getLibPath();
                if ( useLibPath == null ) {
                    useLibPath = useModulePath + getDefaultLibPath();
                }

                useLibPaths = new ArrayList<String>();
                selectJars(useLibPath, useLibPaths);
            }

            for ( String nextLibPath : useLibPaths ) {
                String nextLibName;
                if ( nextLibPath.startsWith(useModulePath) ) {
                    nextLibName = nextLibPath.substring( useModulePath.length() );
                } else {
                    nextLibName = nextLibPath;
                }

                ScanPolicy scanPolicy = selectPolicy(nextLibPath);
                @SuppressWarnings("unused")
                ClassSourceImpl_MappedJar libClassSource =
                    addJarClassSource(rootClassSource, nextLibName, nextLibPath, scanPolicy);
                // throws ClassSource_Exception
            }
        }
    }

    //

    @Override
    @Trivial
    public void logInternal(Logger useLogger) {
        super.logInternal(useLogger);

        String methodName = "logInternal";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Classes path [ " + getClassesPath() + " ]");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Library path [ " + getLibPath() + " ]");

        List<String> useLibPaths = getLibPaths();
        if ( useLibPaths != null ) {
            for ( String nextLibPath : useLibPaths ) {
                ScanPolicy nextScanPolicy = selectPolicy(nextLibPath);
                useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "      Library [ " + nextLibPath + " ] [ " + nextScanPolicy + " ]");
            }
        }
    }
}
