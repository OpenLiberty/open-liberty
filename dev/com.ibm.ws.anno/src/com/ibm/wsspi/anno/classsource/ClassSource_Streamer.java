/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.classsource;

import java.io.InputStream;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;

/**
 * <p>Call back type for class source processing.</p>
 */
public interface ClassSource_Streamer {
    /**
     * <p>Tell if a specified class is to be scanned.</p>
     * 
     * @param className The name of the class to test.
     * @param scanPolicy The policy to test against.
     * 
     * @return True if the class is to be processed. Otherwise, false.
     */
    boolean doProcess(String className, ScanPolicy scanPolicy);

    /**
     * <p>Process the data for the specified class.</p>
     * 
     * @param classSourceName The name of the class source which contains the class.
     * @param className The name of the class to process.
     * @param inputStream The stream containing the class data.
     * @param scanPolicy The policy active on the class.
     * 
     * @return True if the class was processed. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown if an error occurred while
     *             testing the specified class.
     */
    boolean process(String classSourceName,
            String className, InputStream inputStream,
            ScanPolicy scanPolicy) throws ClassSource_Exception;

    //

    /**
     * <p>Tell if this streamer supports the processing of JANDEX class information.</p>
     *
     * @return True or false telling if this streamer supports the processing of JANDEX
     *     class information.
     */
    boolean supportsJandex();

    /**
     * <p>Process the data for the specified class.</p>
     *
     * @param classSourceName The name of the class source which contains the class.
     * @param className The name of the class to process.
     * @param jandexClassInfo JANDEX class information for the class.
     * @param scanPolicy The policy active on the class.
     * 
     * @return True if the class was processed. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown if an error occurred while
     *             testing the specified class.
     */
    boolean process(
        String classSourceName,
        org.jboss.jandex.ClassInfo jandexClassInfo,
        ScanPolicy scanPolicy) throws ClassSource_Exception;
}
