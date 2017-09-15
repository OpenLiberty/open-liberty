/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.info.PackageInfo;

public class PackageInfoImpl extends InfoImpl implements PackageInfo {

    private static final TraceComponent tc = Tr.register(PackageInfoImpl.class);
    public static final String CLASS_NAME = PackageInfoImpl.class.getName();

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

    public PackageInfoImpl(String name, int modifiers, InfoStoreImpl infoStore) {
        super(name, modifiers, infoStore);

        this.isArtificial = false;
        this.forFailedLoad = false;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "<init> Created [ {0} ]", getHashText());
        }
    }

    @Override
    protected String internName(String name) {
        return getInfoStore().internPackageName(name);
    }

    @Override
    public String getQualifiedName() {
        return getName();
    }

    //

    @Override
    @Trivial
    public void log(TraceComponent logger) {

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format(" Package [ {0} ]", getHashText()));
            Tr.debug(logger, MessageFormat.format("  isArtifical [ {0} ]", Boolean.valueOf(getIsArtificial())));
            Tr.debug(logger, MessageFormat.format("  forFailedLoad [ {0} ]", Boolean.valueOf(getForFailedLoad())));
            logAnnotations(logger);
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
