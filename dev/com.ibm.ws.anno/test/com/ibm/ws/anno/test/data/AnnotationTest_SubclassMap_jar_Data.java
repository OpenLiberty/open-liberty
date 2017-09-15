/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class AnnotationTest_SubclassMap_jar_Data {
    public static final String EAR_NAME = "SubclassMapTest.ear.unpacked";
    public static final String EAR_LIB_PATH = File.separator + "lib";
    public static final String EJBJAR_NAME = "SubclassMapTest.jar";

    public static final List<String> EJBJAR_MANIFEST_PATHS;

    static {
        EJBJAR_MANIFEST_PATHS = new ArrayList<String>();

    }

    public static ClassSource_Specification_Direct_EJB createClassSourceSpecification(ClassSource_Factory classSourceFactory,
                                                                                      String projectPath,
                                                                                      String dataPath) {

        ClassSource_Specification_Direct_EJB ejbSpecification = classSourceFactory.newEJBSpecification();

        ejbSpecification.setImmediatePath(Common_Data.putIntoPath(projectPath, dataPath, EJBJAR_NAME));

        // Leave the application library unspecified

        ejbSpecification.addManifestJarPaths(Common_Data.putInPath(projectPath, dataPath, EJBJAR_MANIFEST_PATHS));

        return ejbSpecification;
    }

}
