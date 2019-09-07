/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.classsource;

/**
 * Options for class sources.
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
public interface ClassSource_Options extends com.ibm.wsspi.anno.classsource.ClassSource_Options {
    boolean USE_JANDEX_DEFAULT_VALUE = false;
    String USE_JANDEX_PROPERTY_NAME = "com.ibm.ws.jandex.enable";

    boolean getUseJandexDefault();

    boolean getIsSetUseJandex();
    boolean getUseJandex();

    void setUseJandex(boolean useJandex);
    void unsetUseJandex();

    //

    String JANDEX_PATH_DEFAULT_VALUE = "META-INF/jandex.idx";
    String JANDEX_PATH_PROPERTY_NAME = "com.ibm.ws.jandex.path";

    String getJandexPathDefault();

    boolean getIsSetJandexPath();
    String getJandexPath();

    void setJandexPath(String jandexPath);
    void unsetJandexPath();

    //

    int SCAN_THREADS_UNBOUNDED = -1;
    int SCAN_THREADS_MAX = 64;

    int SCAN_THREADS_DEFAULT_VALUE = 1;
    String SCAN_THREADS_PROPERTY_NAME = "anno.cache.scan.threads";

    int getScanThreadsDefault();

    boolean getIsSetScanThreads();
    int getScanThreads();

    void setScanThreads(int scanThreads);
    void unsetScanThreads();
}
