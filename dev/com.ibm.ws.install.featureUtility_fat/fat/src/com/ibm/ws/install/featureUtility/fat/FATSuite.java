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
package com.ibm.ws.install.featureUtility.fat;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({

                InstallFeatureTest.class, InstallServerTest.class

})
public class FATSuite {

    /**
     * Start of FAT suite processing.
     *
     * @throws Exception
     */
    private static final Class<?> c = FATSuite.class;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // TODO
    	
        LibertyServer serverTest = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");

        File jpa20FeatureMF = new File(serverTest.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(c, "isEnabled", "Does the jpa-2.0 feature exist? " + jpa20FeatureMF.exists());
 
        
    }
    
    @AfterClass
    public static void afterSuite() throws Exception {
        // TODO
    	
        LibertyServer serverTest = LibertyServerFactory.getLibertyServer("com.ibm.ws.install.featureUtility_fat");

        File jpa20FeatureMF = new File(serverTest.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(c, "isEnabled", "Does the jpa-2.0 feature exist? " + jpa20FeatureMF.exists());
 
        
    }
}
