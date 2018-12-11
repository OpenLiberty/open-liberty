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
package com.ibm.wsspi.anno.classsource;

/**
 * Options for class sources.
 * 
 * See {@link ClassSource_Factory#createAggregateClassSource(String, ClassSource_Options)}.
 * 
 * Only one option is supported: Whether Jandex indexes are to be used when populating
 * annotations targets tables.
 * 
 * That is, when processing a non-aggregate class source, when Jandex use is enabled,
 * steps to populate annotation targets first check for a Jandex index in a standard
 * location.  That is 'META-INF/jandex.idx'.
 * 
 * An adjustment is made when scanning a container mapped class source: If the container
 * is not a root container and has the relative path 'WEB-INF/classes', the location
 * of the Jandex index is adjusted to "../../META-INF/jandex.idx', which shifts the
 * location outside of the immediate container and back to being relative to the
 * container root.
 */
public interface ClassSource_Options {
    // JANDEX usage:

    boolean JANDEX_ENABLE_DEFAULT_VALUE = false;
    String JANDEX_ENABLE_PROPERTY_NAME = "com.ibm.ws.jandex.enable";

    boolean getUseJandexDefault();

    boolean getIsSetUseJandex();
    boolean getUseJandex();

    void setUseJandex(boolean useJandex);
    void unsetUseJandex();

    // Full JANDEX usage:

    boolean JANDEX_ENABLE_FULL_DEFAULT_VALUE = false;
    String JANDEX_ENABLE_FULL_PROPERTY_NAME = "com.ibm.ws.jandex.enable.full";

    boolean getUseJandexFullDefault();

    boolean getIsSetUseJandexFull();
    boolean getUseJandexFull();

    void setUseJandexFull(boolean useJandexFull);
    void unsetUseJandexFull();

    // Jandex path ...

    String JANDEX_PATH_DEFAULT_VALUE = "META-INF/jandex.idx";
    String JANDEX_PATH_PROPERTY_NAME = "com.ibm.ws.jandex.path";

    String getJandexPathDefault();

    boolean getIsSetJandexPath();
    String getJandexPath();

    void setJandexPath(String jandexPath);
    void unsetJandexPath();

    // Scan threading:

    int SCAN_THREADS_UNBOUNDED = -1;
    int SCAN_THREADS_MAX = 64;

    int SCAN_THREADS_DEFAULT_VALUE = 8;
    String SCAN_THREADS_PROPERTY_NAME = "com.ibm.ws.anno.scan.threads";

    int getScanThreadsDefault();

    boolean getIsSetScanThreads();
    int getScanThreads();

    void setScanThreads(int scanThreads);
    void unsetScanThreads();
}
