/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.classsource.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;

/**
 * Standard implementation of class source options.
 */
public class ClassSourceImpl_Options implements ClassSource_Options {
    private static final String CLASS_NAME = ClassSourceImpl_Options.class.getSimpleName();

    //

    private static final boolean HAS_JANDEX_ENABLE_OVERRIDE =
        AnnotationServiceImpl_Logging.hasProperty(JANDEX_ENABLE_PROPERTY_NAME);

    @Trivial
    public static boolean getHasJandexEnableOverride() {
        return HAS_JANDEX_ENABLE_OVERRIDE;
    }

    private static final boolean JANDEX_ENABLE_OVERRIDE =
        AnnotationServiceImpl_Logging.getProperty(
            AnnotationServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            JANDEX_ENABLE_PROPERTY_NAME, JANDEX_ENABLE_DEFAULT_VALUE);

    @Trivial
    public static boolean getJandexEnableOverride() {
        return JANDEX_ENABLE_OVERRIDE;
    }

    //

    private static final boolean HAS_JANDEX_ENABLE_FULL_OVERRIDE =
        AnnotationServiceImpl_Logging.hasProperty(JANDEX_ENABLE_FULL_PROPERTY_NAME);

    @Trivial
    public static boolean getHasJandexEnableFullOverride() {
        return HAS_JANDEX_ENABLE_FULL_OVERRIDE;
    }

    private static final boolean JANDEX_ENABLE_FULL_OVERRIDE =
        AnnotationServiceImpl_Logging.getProperty(
            AnnotationServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            JANDEX_ENABLE_FULL_PROPERTY_NAME, JANDEX_ENABLE_FULL_DEFAULT_VALUE);

    @Trivial
    public static boolean getJandexEnableFullOverride() {
        return JANDEX_ENABLE_FULL_OVERRIDE;
    }

    //

    private static final boolean HAS_JANDEX_PATH_OVERRIDE =
        AnnotationServiceImpl_Logging.hasProperty(JANDEX_PATH_PROPERTY_NAME);

    @Trivial
    public static boolean getHasJandexPathOverride() {
        return HAS_JANDEX_PATH_OVERRIDE;
    }

    private static final String JANDEX_PATH_OVERRIDE =
        AnnotationServiceImpl_Logging.getProperty(
            AnnotationServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            JANDEX_PATH_PROPERTY_NAME, JANDEX_PATH_DEFAULT_VALUE);

    @Trivial
    public static String getJandexPathOverride() {
        return JANDEX_PATH_OVERRIDE;
    }

    //

    private static final boolean HAS_SCAN_THREADS_OVERRIDE =
        AnnotationServiceImpl_Logging.hasProperty(SCAN_THREADS_PROPERTY_NAME);

    @Trivial
    public static boolean getHasScanThreadsOverride() {
        return HAS_SCAN_THREADS_OVERRIDE;
    }

    private static final int SCAN_THREADS_OVERRIDE =
        AnnotationServiceImpl_Logging.getProperty(
            AnnotationServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            SCAN_THREADS_PROPERTY_NAME, SCAN_THREADS_DEFAULT_VALUE);

    @Trivial
    public static int getScanThreadsOverride() {
        return SCAN_THREADS_OVERRIDE;
    }

    //

    @Trivial
    public ClassSourceImpl_Options() {
        if ( getHasJandexEnableOverride() ) {
            this.isSetUseJandex = true;
            this.useJandex = getJandexEnableOverride();
        } else {
            this.isSetUseJandex = false;
            this.useJandex = JANDEX_ENABLE_DEFAULT_VALUE;
        }

        if ( getHasJandexEnableFullOverride() ) {
            this.isSetUseJandexFull = true;
            this.useJandexFull = getJandexEnableFullOverride();
        } else {
            this.isSetUseJandexFull = false;
            this.useJandexFull = JANDEX_ENABLE_FULL_DEFAULT_VALUE;
        }

        if ( getHasJandexPathOverride() ) {
            this.isSetJandexPath = true;
            this.jandexPath = getJandexPathOverride();
        } else {
            this.isSetJandexPath = false;
            this.jandexPath = JANDEX_PATH_DEFAULT_VALUE;
        }
        
        if ( getHasScanThreadsOverride() ) {
            this.isSetScanThreads = true;
            this.scanThreads = getScanThreadsOverride();
        } else {
            this.isSetScanThreads = false;
            this.scanThreads = SCAN_THREADS_DEFAULT_VALUE;
        }
        
    }

    @Override
    @Trivial
    public String toString() {
        return super.toString() +
            "(" +
                (Boolean.toString(useJandex) + (isSetUseJandex ? "[Set]" : "[Unset]")) +
                "," +
                (Boolean.toString(useJandexFull) + (isSetUseJandexFull ? "[Set(Full)]" : "[Unset(Full)]")) +
                "," +
                ('"' + jandexPath + '"' + (isSetJandexPath ? "[Set]" : "[Unset]")) +
                "," +
                (Integer.toString(scanThreads)+ (isSetScanThreads ? "[Set]" : "[Unset]")) +
            ")";
    }

    //

    @Override
    @Trivial
    public boolean getUseJandexDefault() {
        return JANDEX_ENABLE_DEFAULT_VALUE;
    }

    private boolean isSetUseJandex;
    private boolean useJandex;

    @Override
    @Trivial
    public boolean getIsSetUseJandex() {
        return isSetUseJandex;
    }

    @Override
    @Trivial
    public boolean getUseJandex() {
        return useJandex;
    }

    @Override
    public void setUseJandex(boolean useJandex) {
        this.isSetUseJandex = true;
        this.useJandex = useJandex;
    }

    @Override
    public void unsetUseJandex() {
        this.useJandex = JANDEX_ENABLE_FULL_DEFAULT_VALUE;
        this.isSetUseJandex = false;
    }

    //

    @Override
    @Trivial
    public boolean getUseJandexFullDefault() {
        return JANDEX_ENABLE_FULL_DEFAULT_VALUE;
    }

    private boolean isSetUseJandexFull;
    private boolean useJandexFull;

    @Override
    @Trivial
    public boolean getIsSetUseJandexFull() {
        return isSetUseJandexFull;
    }

    @Override
    @Trivial
    public boolean getUseJandexFull() {
        return useJandexFull;
    }

    @Override
    public void setUseJandexFull(boolean useJandexFull) {
        this.isSetUseJandexFull = true;
        this.useJandexFull = useJandexFull;
    }

    @Override
    public void unsetUseJandexFull() {
        this.useJandexFull = JANDEX_ENABLE_FULL_DEFAULT_VALUE;
        this.isSetUseJandexFull = false;
    }

    //

    @Override
    public String getJandexPathDefault() {
        return JANDEX_PATH_DEFAULT_VALUE;
    }

    private boolean isSetJandexPath;
    private String jandexPath;

    @Override
    @Trivial
    public boolean getIsSetJandexPath() {
        return isSetJandexPath;
    }

    @Override
    @Trivial    
    public String getJandexPath() {
        return jandexPath;
    }

    @Override
    public void setJandexPath(String jandexPath) {
        this.jandexPath = jandexPath;
        this.isSetJandexPath = true;
    }

    @Override
    public void unsetJandexPath() {
        this.jandexPath = JANDEX_PATH_DEFAULT_VALUE;
        this.isSetJandexPath = false;
    }

    //

    @Override
    @Trivial
    public int getScanThreadsDefault() {
        return SCAN_THREADS_DEFAULT_VALUE;
    }

    private boolean isSetScanThreads;
    private int scanThreads;

    @Override
    @Trivial
    public boolean getIsSetScanThreads() {
        return isSetScanThreads;
    }

    @Override
    @Trivial
    public int getScanThreads() {
        return scanThreads;
    }

    @Override
    public void setScanThreads(int scanThreads) {
        this.isSetScanThreads = true;
        this.scanThreads = scanThreads; 
    }

    @Override
    public void unsetScanThreads() {
        this.isSetScanThreads = false;
        this.scanThreads = SCAN_THREADS_DEFAULT_VALUE;
    }
}
