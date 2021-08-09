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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public class ClassSourceImpl_Specification_Direct_WAR
                extends ClassSourceImpl_Specification_Direct
                implements ClassSource_Specification_Direct_WAR {

    public ClassSourceImpl_Specification_Direct_WAR(ClassSourceImpl_Factory factory) {
        super(factory);
    }

    //

    protected String warClassesPath;

    @Override
    public String getWARClassesPath() {
        return warClassesPath;
    }

    @Override
    public void setWARClassesPath(String warClassesPath) {
        this.warClassesPath = warClassesPath;
    }

    //

    protected String warLibraryPath;

    @Override
    public String getWARLibraryPath() {
        return warLibraryPath;
    }

    @Override
    public void setWARLibraryPath(String warLibraryPath) {
        this.warLibraryPath = warLibraryPath;
    }

    //

    protected boolean useWARLibraryJarPaths;

    @Override
    public boolean getUseWARLibraryJarPaths() {
        return useWARLibraryJarPaths;
    }

    @Override
    public void setUseWARLibraryJarPaths(boolean useWARLibraryJarPaths) {
        this.useWARLibraryJarPaths = useWARLibraryJarPaths;
    }

    protected List<String> warLibraryJarPaths;

    @Override
    public List<String> getWARLibraryJarPaths() {
        return warLibraryJarPaths;
    }

    @Override
    public void addWARLibraryJarPath(String warLibraryJarPath) {
        if (warLibraryJarPaths == null) {
            warLibraryJarPaths = new ArrayList<String>();
        }

        warLibraryJarPaths.add(warLibraryJarPath);
    }

    @Override
    public void addWARLibraryJarPaths(List<String> warLibraryJarPaths) {
        if (this.warLibraryJarPaths == null) {
            this.warLibraryJarPaths = new ArrayList<String>();
        }

        this.warLibraryJarPaths.addAll(warLibraryJarPaths);
    }

    protected Set<String> warIncludedJarPaths;

    @Override
    public Set<String> getWARIncludedJarPaths() {
        return warIncludedJarPaths;
    }

    @Override
    public void addWARIncludedJarPath(String warIncludedJarPath) {
        if (warIncludedJarPaths == null) {
            warIncludedJarPaths = new HashSet<String>();
        }

        warIncludedJarPaths.add(warIncludedJarPath);
    }

    @Override
    public void addWARIncludedJarPaths(Set<String> warIncludedJarPaths) {
        if (this.warIncludedJarPaths == null) {
            this.warIncludedJarPaths = new HashSet<String>();
        }

        this.warIncludedJarPaths.addAll(warIncludedJarPaths);
    }

    //

    @Override
    public ClassSource_Aggregate createClassSource(String targetName, ClassLoader rootClassLoader)
                    throws ClassSource_Exception {

        ClassSourceImpl_Aggregate classSource = createAggregateClassSource(targetName);

        String useWARClassesPath = getWARClassesPath();
        if (useWARClassesPath == null) {
            useWARClassesPath = getImmediatePath() + "/" + "WEB-INF/classes";
        }

        getFactory().addDirectoryClassSource(classSource, targetName, useWARClassesPath, ScanPolicy.SEED); // throws ClassSource_Exception

        List<String> warLibJarPaths;

        if (getUseWARLibraryJarPaths()) {
            warLibJarPaths = getWARLibraryJarPaths();

        } else {
            String useWARLibPath = getWARLibraryPath();

            if (useWARLibPath == null) {
                useWARLibPath = getImmediatePath() + "/" + "WEB-INF/lib";
            }

            warLibJarPaths = selectJars(useWARLibPath);
        }

        Set<String> useWARIncludedJarPaths = getWARIncludedJarPaths();

        if (warLibJarPaths != null) {
            for (String nextJarPath : warLibJarPaths) {
                boolean isSeed = ((useWARIncludedJarPaths == null) || useWARIncludedJarPaths.contains(nextJarPath));
                ScanPolicy scanPolicy = (isSeed ? ScanPolicy.SEED : ScanPolicy.EXTERNAL);
                getFactory().addJarClassSource(classSource, nextJarPath, nextJarPath, scanPolicy); // throws ClassSource_Exception
            }
        }

        addStandardClassSources(targetName, rootClassLoader, classSource);

        return classSource;
    }

    //

    @Override
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("Class source specification [ {0} ]", getHashText()));

        logLocations(logger);
        logCommon(logger);
    }

    protected void logLocations(TraceComponent logger) {

        String useWARClassesPath = getWARClassesPath();
        if (useWARClassesPath == null) {
            useWARClassesPath = getImmediatePath() + "/" + "WEB-INF/classes";
        }

        Tr.debug(logger, "Classes path [ " + useWARClassesPath + " ]");

        List<String> warLibJarPaths;

        if (getUseWARLibraryJarPaths()) {
            warLibJarPaths = getWARLibraryJarPaths();

            if (warLibJarPaths != null) {
                for (String nextJarPath : warLibJarPaths) {
                    Tr.debug(logger, "WAR library jar [ " + nextJarPath + " ]");
                }
            }

        } else {
            String useWARLibPath = getWARLibraryPath();

            if (useWARLibPath == null) {
                useWARLibPath = getImmediatePath() + "/" + "WEB-INF/lib";
            }

            Tr.debug(logger, "WAR library path [ " + useWARLibPath + " ]");
        }
    }
}
