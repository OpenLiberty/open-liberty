/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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
package com.ibm.ws.annocache.classsource.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;

/**
 * Standard implementation of class source options.
 */
public class ClassSourceImpl_Options implements ClassSource_Options {
    private static final String CLASS_NAME = ClassSourceImpl_Options.class.getSimpleName();

    //

    private static final Boolean USE_JANDEX_OVERRIDE;

    static {
        String override = AnnotationCacheServiceImpl_Logging.getProperty(
            AnnotationCacheServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            USE_JANDEX_PROPERTY_NAME, null);
        USE_JANDEX_OVERRIDE = ( override == null ? null : Boolean.valueOf(override) );
    }

    //

    private static final boolean HAS_JANDEX_PATH_OVERRIDE =
            AnnotationCacheServiceImpl_Logging.hasProperty(JANDEX_PATH_PROPERTY_NAME);

    @Trivial
    public static boolean getHasJandexPathOverride() {
        return HAS_JANDEX_PATH_OVERRIDE;
    }

    private static final String JANDEX_PATH_OVERRIDE =
        AnnotationCacheServiceImpl_Logging.getProperty(
            AnnotationCacheServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            JANDEX_PATH_PROPERTY_NAME, JANDEX_PATH_DEFAULT_VALUE);

    @Trivial
    public static String getJandexPathOverride() {
        return JANDEX_PATH_OVERRIDE;
    }
    
    //

    private static final boolean HAS_READ_WEB_INF_JANDEX_OVERRIDE =
        AnnotationCacheServiceImpl_Logging.hasProperty(READ_WEB_INF_JANDEX_PROPERTY_NAME);

    @Trivial
    public static boolean getHasJandexReadWebInfOverride() {
        return HAS_READ_WEB_INF_JANDEX_OVERRIDE;
    }

    private static final Boolean JANDEX_READ_WEB_INF_OVERRIDE;

    static {
        String override = AnnotationCacheServiceImpl_Logging.getProperty(
            AnnotationCacheServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            READ_WEB_INF_JANDEX_PROPERTY_NAME, null);
        JANDEX_READ_WEB_INF_OVERRIDE = ( override == null ? null : Boolean.valueOf(override) );
    }

    @Trivial
    public static Boolean getJandexReadWebInfOverride() {
        return JANDEX_READ_WEB_INF_OVERRIDE;
    }

    //

    private static final boolean HAS_SCAN_THREADS_OVERRIDE =
        AnnotationCacheServiceImpl_Logging.hasProperty(SCAN_THREADS_PROPERTY_NAME);

    @Trivial
    public static boolean getHasScanThreadsOverride() {
        return HAS_SCAN_THREADS_OVERRIDE;
    }

    private static final int SCAN_THREADS_OVERRIDE =
        AnnotationCacheServiceImpl_Logging.getProperty(
            AnnotationCacheServiceImpl_Logging.ANNO_LOGGER,
            CLASS_NAME, "<static init>",
            SCAN_THREADS_PROPERTY_NAME, SCAN_THREADS_DEFAULT_VALUE);

    @Trivial
    public static int getScanThreadsOverride() {
        return SCAN_THREADS_OVERRIDE;
    }

    //

    @Trivial
    public ClassSourceImpl_Options() {
        this.useJandex = null;

        if ( getHasJandexPathOverride() ) {
            this.isSetJandexPath = true;
            this.jandexPath = getJandexPathOverride();
        } else {
            this.isSetJandexPath = false;
            this.jandexPath = JANDEX_PATH_DEFAULT_VALUE;
        }
        
        if ( getHasJandexReadWebInfOverride() ) {
            this.isSetJandexReadWebInf = true;
            this.jandexReadWebInf = getJandexReadWebInfOverride();
        } else {
            this.isSetJandexReadWebInf = false;
            this.jandexReadWebInf = READ_WEB_INF_JANDEX_DEFAULT_VALUE;
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
                " UseJandex " + useJandex + " - " + USE_JANDEX_OVERRIDE + ", " +
                " JandexPath " + ('"' + jandexPath + '"' + (isSetJandexPath ? "[Set]" : "[Unset]")) + ", " +
                " JandexEnableWebInf " + jandexReadWebInf + " - " + HAS_READ_WEB_INF_JANDEX_OVERRIDE + ", " +
                " ScanThreads " + (Integer.toString(scanThreads)+ (isSetScanThreads ? "[Set]" : "[Unset]")) +
            ")";
    }

    //

    @Override
    @Trivial
    public boolean getUseJandexDefault() {
        return USE_JANDEX_DEFAULT_VALUE;
    }

    private Boolean useJandex;

    @Override
    @Trivial
    public boolean getIsSetUseJandex() {
        return ( useJandex != null );
    }

    @Override
    public void setUseJandex(boolean useJandex) {
        this.useJandex = Boolean.valueOf(useJandex);
    }

    @Override
    public void unsetUseJandex() {
        this.useJandex = null;
    }

    @Override
    @Trivial
    public boolean getUseJandex() {
    	if ( USE_JANDEX_OVERRIDE != null ) {
    		return USE_JANDEX_OVERRIDE.booleanValue();
    	} else if ( useJandex != null ) {
    		return useJandex.booleanValue();
    	} else {
    		return USE_JANDEX_DEFAULT_VALUE;
    	}
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
    public boolean getJandexReadWebInfDefault() {
        return READ_WEB_INF_JANDEX_DEFAULT_VALUE;
    }

    private boolean isSetJandexReadWebInf;
    private boolean jandexReadWebInf = READ_WEB_INF_JANDEX_DEFAULT_VALUE;

    @Override
    @Trivial
    public boolean getIsSetReadWebInfJandex() {
        return isSetJandexReadWebInf;
    }

    @Override
    @Trivial
    public boolean getReadWebInfJandex() {
        return jandexReadWebInf;
    }

    @Override
    public void setReadWebInfJandex(boolean jandexReadWebInf) {
        this.jandexReadWebInf = jandexReadWebInf;
        this.isSetJandexReadWebInf = true;
    }

    @Override
    public void unsetReadWebInfJandex() {
        this.jandexReadWebInf = READ_WEB_INF_JANDEX_DEFAULT_VALUE;
        this.isSetJandexReadWebInf = false;
    }

    @Override
    @Trivial    
    public String getJandexWebInfPath() {
        return ClassSource_Options.JANDEX_PATH_WEB_INF;
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
