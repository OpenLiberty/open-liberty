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

package com.ibm.wsspi.annocache.classsource;

import java.io.InputStream;

/**
 * <p>Call back type for class source processing.</p>
 */
public interface ClassSource_Streamer extends com.ibm.wsspi.anno.classsource.ClassSource_Streamer {
    /**
     * <p>Tell if a specified class is to be scanned.</p>
     * 
     * @param className The name of the class to test.
     *
     * @return True if the class is to be processed. Otherwise, false.
     */
    boolean doProcess(String className);

    /**
     * <p>Process the data for the specified class.</p>
     *
     * @param i_className The interned name of the class to process.
     * @param inputStream The stream containing the class data.
     * 
     * @return True if the class was processed. Otherwise, false.
     *
     * @throws ClassSource_Exception Thrown if an error occurred while
     *             testing the specified class.
     */
    boolean process(String i_className, InputStream inputStream)
        throws ClassSource_Exception;

    // Jandex added APIs ...

    /**
     * <p>Tell if this streamer supports the processing of Jandex class information.</p>
     *
     * @return True or false telling if this streamer supports the processing of Jandex
     *     class information.
     */
    boolean supportsJandex();

    /**
     * <p>Tell if a Jandex index is to be created then used for processing.</p>
     * 
     * <p>Requires that {@link #supportsJandex()} answer true.</p>
     * 
     * <p>The created index must be supplied to a call to {@link #storeJandex}.</p>
     *
     * @return True or false telling if a Jandex index is to be created.
     */
    boolean createJandex();

    /**
     * <p>Store a Jandex index as needed.</p>
     * 
     * @param index A Jandex index which is to be stored.
     */
    void storeJandex(Object jandexIndex);

    /**
     * <p>Tell the streamer that processing is using data from jandex.</p>
     */
    void processJandex();

    /**
     * <p>Process the data for the specified class.  The data was obtained from
     * the full Jandex reader.</p>
     *
     * <p>Entry point preserved to enable comparisons between full and sparse
     * processing.</p>
     *
     * @param classInfo Full Jandex class information for the class.
     *     Typed as {#link Object} to avoid exposing <code>org.jboss.jandex.ClassInfo</code>
     *     in SPI.
     *
     * @return True if the class was processed. Otherwise, false.
     *
     * @throws ClassSource_Exception Thrown if an error occurred while
     *     testing the specified class.
     */
    boolean processJandex(Object classInfo) throws ClassSource_Exception;

    /**
     * <p>Process the data for the specified class.  The data was obtained
     * from the sparse reader.</p>
     *
     * @param sparseClassInfo Sparse Jandex class information for the class.
     *     Typed as {#link Object} to avoid exposing <code>org.jboss.jandex.ClassInfo</code>
     *     in SPI.
     * 
     * @return True if the class was processed. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown if an error occurred while
     *     testing the specified class.
     */
    boolean processSparseJandex(Object sparseClassInfo) throws ClassSource_Exception;
}
