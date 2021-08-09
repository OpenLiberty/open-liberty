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

package com.ibm.ws.anno.test.cases;

import java.io.PrintWriter;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.anno.test.data.Common_Data;
import com.ibm.ws.anno.test.data.SCITest_Data;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

public class SCITest_SCIAbsolute_war_Test extends AnnotationTest_BaseClass {
    public static final String EAR_NAME = SCITest_Data.EAR_NAME;
    public static final String WAR_NAME = SCITest_Data.WAR_NAME_ABSOLUTE;

    @Override
    public ClassSource_Specification_Direct_WAR createClassSourceSpecification() {
        return null; // Not in use
    }

    public ClassSource_Aggregate createAggregateClassSource(String name) throws ClassSource_Exception {
        return getClassSourceFactory().createAggregateClassSource(name);
    }

    @Override
    public ClassSource_Aggregate createClassSource() throws ClassSource_Exception {
        String fullDataPath = Common_Data.putIntoPath(projectPath, dataPath);
        String fullWARPath = Common_Data.putIntoPath(fullDataPath, WAR_NAME);

        ClassSource_Aggregate classSource = createAggregateClassSource(WAR_NAME);

        String useWARClassesPath = fullWARPath + "/" + "WEB-INF/classes";
        getClassSourceFactory().addDirectoryClassSource(classSource, WAR_NAME + " classes",
                                                        useWARClassesPath,
                                                        ScanPolicy.SEED); // throws ClassSource_Exception

        String warLibPath = fullWARPath + "/" + "WEB-INF/lib";

        String sci4JarPath = warLibPath + "/" + SCITest_Data.JAR_NAME_SCI4;
        getClassSourceFactory().addJarClassSource(classSource,
                                                  sci4JarPath, sci4JarPath,
                                                  ScanPolicy.SEED);
        // throws ClassSource_Exception

        String sciWithListenerPath = warLibPath + "/" + SCITest_Data.JAR_NAME_SCI_WITH_LISTENER;
        getClassSourceFactory().addJarClassSource(classSource,
                                                  sciWithListenerPath, sciWithListenerPath,
                                                  ScanPolicy.SEED);

        getClassSourceFactory().addClassLoaderClassSource(classSource, WAR_NAME + " classloader", getClass().getClassLoader());

        return classSource;
    }

    //

    public static final String LOG_NAME = EAR_NAME + " _" + WAR_NAME + ".log";

    //

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return WAR_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    //

    /*
     * @Override
     * public boolean getSeedStorage() {
     * return true;
     * }
     */

    //

    @Test
    public void testSCI_SCIAbsolute_detail() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }
}
