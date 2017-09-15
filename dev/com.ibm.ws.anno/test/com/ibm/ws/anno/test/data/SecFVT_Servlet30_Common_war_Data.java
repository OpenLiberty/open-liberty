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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class SecFVT_Servlet30_Common_war_Data {
    public static final String EAR_NAME = "secfvt_servlet30.ear.unpacked";

    public static final String EJBJAR_NAME = "SecFVTS1EJB.jar";

    // Changed to a parameter:
    // public static final String WAR_NAME = ""; 

    public static final String WAR_ANNMIXED_NAME = "Servlet30AnnMixed.war.unpacked";
    public static final String WAR_ANNPURE_NAME = "Servlet30AnnPure.war.unpacked";
    public static final String WAR_ANNWEBXML_NAME = "Servlet30AnnWebXML.war.unpacked";
    public static final String WAR_DYNCONFLICT_NAME = "Servlet30DynConflict.war.unpacked";
    public static final String WAR_DYNPURE_NAME = "Servlet30DynPure.war.unpacked";
    public static final String WAR_INHERIT_NAME = "Servlet30Inherit.war.unpacked";
    public static final String WAR_API_NAME = "Servlet30api.war.unpacked";
    public static final String WAR_APIFL_NAME = "Servlet30apiFL.war.unpacked";

    // An EAR library directory is not available.
    // public static final String EAR_LIB_PATH = File.separator + "lib";

    public static final List<String> WAR_MANIFEST_PATHS;

    static {
        WAR_MANIFEST_PATHS = new ArrayList<String>();
        WAR_MANIFEST_PATHS.add(EJBJAR_NAME);
    }

    public static ClassSource_Specification_Direct_WAR createClassSourceSpecification(ClassSource_Factory classSourceFactory,
                                                                                      String projectPath,
                                                                                      String dataPath,
                                                                                      String warName) {
        ClassSource_Specification_Direct_WAR warSpecification = classSourceFactory.newWARSpecification();

        warSpecification.setImmediatePath(Common_Data.putIntoPath(projectPath, dataPath, warName));

        // Leave the application library unspecified: No application library directory is available.
        //        
        // ejbSpecification.setApplicationLibraryPath( Common_Data.putInProjectData(projectPath, dataPath, EAR_LIB_PATH) );

        warSpecification.addManifestJarPaths(Common_Data.putInPath(projectPath, dataPath, WAR_MANIFEST_PATHS));

        return warSpecification;
    }
}
