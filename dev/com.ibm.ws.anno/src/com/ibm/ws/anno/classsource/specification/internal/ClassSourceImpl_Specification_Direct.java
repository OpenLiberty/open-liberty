/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.specification.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct;
import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public abstract class ClassSourceImpl_Specification_Direct
                extends ClassSourceImpl_Specification
                implements ClassSource_Specification_Direct {

    //

    protected ClassSourceImpl_Specification_Direct(ClassSourceImpl_Factory factory) {
        super(factory);
    }

    //

    protected String immediatePath;

    @Override
    public String getImmediatePath() {
        return immediatePath;
    }

    @Override
    public void setImmediatePath(String immediatePath) {
        this.immediatePath = immediatePath;
    }

    //

    protected String applicationLibraryPath;

    @Override
    public String getApplicationLibraryPath() {
        return applicationLibraryPath;
    }

    @Override
    public void setApplicationLibraryPath(String applicationLibraryPath) {
        this.applicationLibraryPath = applicationLibraryPath;
    }

    //

    protected List<String> applicationLibraryJarPaths;

    @Override
    public List<String> getApplicationLibraryJarPaths() {
        return applicationLibraryJarPaths;
    }

    @Override
    public void addApplicationLibraryJarPath(String applicationLibraryJarPath) {
        if (applicationLibraryJarPaths == null) {
            applicationLibraryJarPaths = new ArrayList<String>();
        }

        applicationLibraryJarPaths.add(applicationLibraryJarPath);
    }

    @Override
    public void addApplicationLibraryJarPaths(List<String> applicationLibraryJarPaths) {
        if (this.applicationLibraryJarPaths == null) {
            this.applicationLibraryJarPaths = new ArrayList<String>();
        }

        this.applicationLibraryJarPaths.addAll(applicationLibraryJarPaths);
    }

    //

    protected List<String> manifestJarPaths;

    @Override
    public List<String> getManifestJarPaths() {
        return manifestJarPaths;
    }

    @Override
    public void addManifestJarPath(String manifestJarPath) {
        if (manifestJarPaths == null) {
            manifestJarPaths = new ArrayList<String>();
        }

        manifestJarPaths.add(manifestJarPath);

    }

    @Override
    public void addManifestJarPaths(List<String> manifestJarPaths) {
        if (this.manifestJarPaths == null) {
            this.manifestJarPaths = new ArrayList<String>();
        }

        this.manifestJarPaths.addAll(manifestJarPaths);
    }

    //

    @Override
    public abstract ClassSource_Aggregate createClassSource(String targetName,
                                                            ClassLoader rootClassLoader) throws ClassSource_Exception;

    protected void addStandardClassSources(String moduleUri,
                                           ClassLoader rootClassLoader,
                                           ClassSource_Aggregate classSource) throws ClassSource_Exception {

        String useApplicationLibPath = getApplicationLibraryPath();

        List<String> applicationLibJarPaths;

        if (useApplicationLibPath != null) {
            applicationLibJarPaths = selectJars(useApplicationLibPath);
        } else {
            applicationLibJarPaths = getApplicationLibraryJarPaths();
        }

        if (applicationLibJarPaths != null) {
            for (String nextJarPath : applicationLibJarPaths) {
                getFactory().addJarClassSource(classSource, nextJarPath, nextJarPath, ScanPolicy.EXTERNAL); // throws ClassSource_Exception
            }
        }

        List<String> manifestJarPaths = getManifestJarPaths();

        if (manifestJarPaths != null) {
            for (String nextManifestJarPath : manifestJarPaths) {
                getFactory().addJarClassSource(classSource, nextManifestJarPath, nextManifestJarPath, ScanPolicy.EXTERNAL); // throws ClassSource_Exception
            }
        }

        if (rootClassLoader != null) {
            getFactory().addClassLoaderClassSource(classSource, "classloader", rootClassLoader);
        }
    }

    // TODO: This must recurse!

    public List<String> selectJars(String targetPath) {
        List<String> jarPaths = new ArrayList<String>();

        if (targetPath == null) {
            return jarPaths;
        }

        File targetDir = new File(targetPath);
        if (!UtilImpl_FileUtils.exists(targetDir)) {
            return jarPaths;
        }

        File[] targetFiles = UtilImpl_FileUtils.listFiles(targetDir);
        if (targetFiles != null) {
            for (File nextTargetFile : targetFiles) {
                String nextTargetName = nextTargetFile.getName();
                if (nextTargetName.toUpperCase().endsWith(".JAR")) {
                    String nextTargetPath = targetPath + "/" + nextTargetName;
                    jarPaths.add(nextTargetPath);
                }
            }
        }

        return jarPaths;
    }

    //

    @Override
    public abstract void log(TraceComponent logger);

    protected void logCommon(TraceComponent logger) {

        Tr.debug(logger, "  Immediate path [ " + getImmediatePath() + " ]");
        Tr.debug(logger, "  Application library path [ " + getApplicationLibraryPath() + " ]");

        List<String> useLibraryJarPaths = getApplicationLibraryJarPaths();

        if (useLibraryJarPaths != null) {
            for (String nextJarPath : useLibraryJarPaths) {
                Tr.debug(logger, "  Application library jar [ " + nextJarPath + " ]");
            }
        }

        List<String> useManifestJarPaths = getManifestJarPaths();
        if (useManifestJarPaths != null) {
            for (String nextJarPath : useManifestJarPaths) {
                Tr.debug(logger, "  Manifest jar [ " + nextJarPath + " ]");
            }
        }
    }
}
