/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.info.PackageInfo;

public class PackageInfoImpl extends InfoImpl implements PackageInfo {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
    private static final String CLASS_NAME = PackageInfoImpl.class.getSimpleName();

    //

    public static final String PACKAGE_INFO_CLASS_NAME = "package-info";

    public static boolean isPackageName(String className) {
        return (className.endsWith(PACKAGE_INFO_CLASS_NAME));
    }

    public static String stripPackageNameFromClassName(String className) {
        return (className.substring(0, className.length() - (PACKAGE_INFO_CLASS_NAME.length() + 1)));
    }

    public static String addClassNameToPackageName(String packageName) {
        return packageName + "." + PACKAGE_INFO_CLASS_NAME;
    }

    //

    private static String getHashSuffix(String name) {
        return name;
    }

    public PackageInfoImpl(String name, int modifiers, InfoStoreImpl infoStore) {
        super( name, modifiers, infoStore, getHashSuffix(name) );

        String methodName = "<init>";
        
        this.isArtificial = false;
        this.forFailedLoad = false;

        if (logger.isLoggable(Level.FINER)) { 
            logger.logp(Level.FINER, CLASS_NAME, methodName, "Created [ {0} ]", getHashText());
        }
    }

    @Override
    protected String internName(String packageName) {
        return getInfoStore().internPackageName(packageName);
    }

    @Override
    public String getQualifiedName() {
        return getName();
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }
        
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, " Package [ {0} ]", getHashText());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  isArtifical [ {0} ]", Boolean.valueOf(getIsArtificial()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  forFailedLoad [ {0} ]", Boolean.valueOf(getForFailedLoad()));

        logAnnotations(useLogger);
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent tc) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" Package [ {0} ]", getHashText()));
            Tr.debug(tc, MessageFormat.format("  isArtifical [ {0} ]", Boolean.valueOf(getIsArtificial())));
            Tr.debug(tc, MessageFormat.format("  forFailedLoad [ {0} ]", Boolean.valueOf(getForFailedLoad())));
            logAnnotations(tc);
        }
    }

    //

    protected boolean isArtificial;
    protected boolean forFailedLoad;

    @Override
    public boolean getIsArtificial() {
        return this.isArtificial;
    }

    protected void setIsArtificial(boolean isArtificial) {
        this.isArtificial = isArtificial;
    }

    @Override
    public boolean getForFailedLoad() {
        return this.forFailedLoad;
    }

    public void setForFailedLoad(boolean forFailedLoad) {
        this.forFailedLoad = forFailedLoad;
    }
}
