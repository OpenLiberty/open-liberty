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

package com.ibm.ws.annocache.test.scan.samples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class AppDeployBench_GSEJB_jar_Data {
    public static final String EAR_NAME = "AppDeployBench.ear";
    public static final String EAR_SIMPLE_NAME = "AppDeployBench";
    public static final String EAR_LIB_PATH = File.separator + "lib";
    
    public static final String EJBJAR_NAME = "GSEJB.jar";
    public static final String EJBJAR_SIMPLE_NAME = "GSEJB";

    public static final List<String> EJBJAR_MANIFEST_PATHS;

    static {
        EJBJAR_MANIFEST_PATHS = new ArrayList<String>();

        EJBJAR_MANIFEST_PATHS.add("GarageSaleUtils.jar");
        // EJBJAR_MANIFEST_PATHS.add("WSNClient.jar");
    }

    public static ClassSourceImpl_Specification_Direct_EJB createClassSourceSpecification(ClassSourceImpl_Factory factory) {

    	ClassSourceImpl_Specification_Direct_EJB ejbSpecification =
            factory.newEJBDirectSpecification(EAR_SIMPLE_NAME, EJBJAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        String earPath = TestLocalization.putIntoData(EAR_NAME) + '/';

        String ejbJarPath = TestLocalization.putInto(earPath, EJBJAR_NAME) + '/';
        ejbSpecification.setModulePath(ejbJarPath);

        ejbSpecification.addManifestPaths( TestLocalization.putInto(earPath, EJBJAR_MANIFEST_PATHS) );

        ejbSpecification.setRootClassLoader( AppDeployBench_GSEJB_jar_Data.class.getClassLoader() );

        return ejbSpecification;
    }

}
