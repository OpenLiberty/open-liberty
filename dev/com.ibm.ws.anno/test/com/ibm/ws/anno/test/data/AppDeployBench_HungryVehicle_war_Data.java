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

package com.ibm.ws.anno.test.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class AppDeployBench_HungryVehicle_war_Data {
    public static final String EAR_NAME = "AppDeployBench.ear.unpacked";
    public static final String EAR_LIB_PATH = File.separator + "lib";
    public static final String WAR_NAME = "HungryVehicle.war.unpacked";
    public static final List<String> WAR_MANIFEST_PATHS;

    static {
        WAR_MANIFEST_PATHS = new ArrayList<String>();

        // TODO: These are not present in AppDeployBench.ear

        // WAR_MANIFEST_PATHS.add("FailureLog_Manager.jar");
        // WAR_MANIFEST_PATHS.add("Sequence_Manager.jar");
        // WAR_MANIFEST_PATHS.add("StationBusinessEJBs.jar");
        // WAR_MANIFEST_PATHS.add("Station_Manager.jar");
        // WAR_MANIFEST_PATHS.add("Store_Manager.jar");
        // WAR_MANIFEST_PATHS.add("JCAViolationManager.jar");
    }

    //

    public static ClassSource_Specification_Direct_WAR createClassSourceSpecification(ClassSource_Factory classSourceFactory,
                                                                                      String projectPath, String dataPath) {

        ClassSource_Specification_Direct_WAR warSpecification = classSourceFactory.newWARSpecification();

        warSpecification.setImmediatePath(Common_Data.putIntoPath(projectPath, dataPath, WAR_NAME));

        // Leave the application library unspecified: No application library directory is available
        // for AppDeployBench.
        //        
        // warSpecification.setApplicationLibraryPath( Common_Data.putInProjectData(projectPath, dataPath, EAR_LIB_PATH) );

        warSpecification.addManifestJarPaths(Common_Data.putInPath(projectPath, dataPath, WAR_MANIFEST_PATHS));

        return warSpecification;
    }
}
