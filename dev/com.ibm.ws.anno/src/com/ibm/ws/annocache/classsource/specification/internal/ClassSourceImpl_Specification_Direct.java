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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_MappedJar;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Direct;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

public abstract class ClassSourceImpl_Specification_Direct
    extends ClassSourceImpl_Specification
    implements ClassSource_Specification_Direct {

    @SuppressWarnings("hiding")	
    public static final String CLASS_NAME = ClassSourceImpl_Specification_Direct.class.getSimpleName();

    //

    protected ClassSourceImpl_Specification_Direct(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super(factory, appName, modName, modCatName);
    }

    //

    protected String modulePath;

    @Override
    public String getModulePath() {
        return modulePath;
    }

    @Override
    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    //

    protected String appLibRootPath;

    @Override
    public String getAppLibRootPath() {
        return appLibRootPath;
    }

    @Override
    public void setAppLibRootPath(String appLibRootPath) {
        this.appLibRootPath = appLibRootPath;

        this.appLibPaths = new ArrayList<String>();
        this.selectJars(this.appLibRootPath, this.appLibPaths);
    }

    //

    protected List<String> appLibPaths;

    @Override
    public List<String> getAppLibPaths() {
        return appLibPaths;
    }

    @Override
    public void addAppLibPath(String appLibPath) {
        if ( appLibPaths == null ) {
            appLibPaths = new ArrayList<String>();
        }
        appLibPaths.add(appLibPath);
    }

    @SuppressWarnings("hiding")
	@Override
    public void addAppLibPaths(List<String> appLibPaths) {
        if ( this.appLibPaths == null ) {
            this.appLibPaths = new ArrayList<String>();
        }
        this.appLibPaths.addAll(appLibPaths);
    }

    //

    protected List<String> manifestPaths;

    @Override
    public List<String> getManifestPaths() {
        return manifestPaths;
    }

    @Override
    public void addManifestPath(String manifestPath) {
        if ( manifestPaths == null ) {
            manifestPaths = new ArrayList<String>();
        }

        manifestPaths.add(manifestPath);

    }

    @SuppressWarnings("hiding")
	@Override
    public void addManifestPaths(List<String> manifestPaths) {
        if ( this.manifestPaths == null ) {
            this.manifestPaths = new ArrayList<String>();
        }

        this.manifestPaths.addAll(manifestPaths);
    }

    //

    @Override
    public abstract void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception;

    @Override
    public void addExternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        // TODO: Need to find a shorter name to use for these.
        // TODO: Need to make sure the names of these are different
        //       than the module element names.

        List<String> useAppLibPaths = getAppLibPaths();

        if ( useAppLibPaths != null ) {
            for ( String nextAppLibPath : useAppLibPaths ) {
                @SuppressWarnings("unused")
                ClassSourceImpl_MappedJar appLibClassSource =
                    addJarClassSource(rootClassSource, nextAppLibPath, nextAppLibPath, ScanPolicy.EXTERNAL);
                    // throws ClassSource_Exception
            }
        }

        List<String> useManifestPaths = getManifestPaths();

        if ( useManifestPaths != null ) {
            for ( String nextManifestPath : useManifestPaths ) {
                @SuppressWarnings("unused")
                ClassSourceImpl_MappedJar manifestClassSource =
                    addJarClassSource(rootClassSource, nextManifestPath, nextManifestPath, ScanPolicy.EXTERNAL);
                // throws ClassSource_Exception
            }
        }
    }

    // TODO: This must recurse!

    public void selectJars(String targetPath, List<String> jarPaths) {
        if ( targetPath == null ) {
            return;
        }

        if ( !targetPath.endsWith("/") ) {
        	throw new IllegalArgumentException("Path [ " + targetPath + " ] must have a trailing '/'.");
        }

        File targetDir = new File(targetPath);
        if ( !UtilImpl_FileUtils.exists(targetDir) ) {
            return;
        }

        File[] targetFiles = UtilImpl_FileUtils.listFiles(targetDir);
        if ( targetFiles != null ) {
            for ( File nextTargetFile : targetFiles ) {
                String nextTargetName = nextTargetFile.getName();
                if ( nextTargetName.toUpperCase().endsWith(".JAR") ) {
                    String nextTargetPath = targetPath + nextTargetName;
                    jarPaths.add(nextTargetPath);
                }
            }
        }
    }

    //

    @Override
    @Trivial
    public void logInternal(Logger useLogger) {
        String methodName = "logInternal";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Module Internals:");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Module path [ " + getModulePath() + " ]");
    }

    @Override
    @Trivial
    public void logExternal(Logger useLogger) {
        String methodName = "logExternal";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Module Externals:");

        String useAppLibPath = getAppLibRootPath();
        
        if ( useAppLibPath == null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Application library path [ Null ]");
        } else {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Application library path [ " + useAppLibPath + " ]");
        }

        List<String> useLibraryJarPaths = getAppLibPaths();
        if ( useLibraryJarPaths != null ) {
            for ( String nextJarPath : useLibraryJarPaths ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Application library jar [ " + nextJarPath + " ]");
            }
        }

        List<String> useManifestJarPaths = getManifestPaths();
        if ( useManifestJarPaths != null ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "    Manifest jars");
            for ( String nextJarPath : useManifestJarPaths ) {
                useLogger.logp(Level.FINER, CLASS_NAME, methodName, "      Manifest jar [ " + nextJarPath + " ]");
            }
        }
    }
}
