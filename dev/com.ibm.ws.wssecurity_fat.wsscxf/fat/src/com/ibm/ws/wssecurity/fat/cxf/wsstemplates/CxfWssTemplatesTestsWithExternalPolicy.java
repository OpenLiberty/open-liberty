/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.wsstemplates;

//import java.io.File;

import org.junit.BeforeClass;
//Added 10/2020
import org.junit.runner.RunWith;

//Added 10/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfWssTemplatesTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

//Added 10/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfWssTemplatesTestsWithExternalPolicy extends CxfWssTemplatesTests {

    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wsstemplateswithep";

    //Added 10/2020
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "wsstemplatesclientwithep", "com.ibm.ws.wssecurity.fat.wsstemplatesclientwithep", "test.wssecfvt.wsstemplates",
                                      "test.wssecfvt.wsstemplates.types");
        ShrinkHelper.defaultDropinApp(server, "wsstemplateswithep", "com.ibm.ws.wssecurity.fat.wsstemplateswithep");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        //orig from CL
        //commonSetUp(serverName, true, "/wsstemplatesclient/CxfWssTemplatesSvcClient");
        commonSetUp(serverName, true, "/wsstemplatesclientwithep/CxfWssTemplatesSvcClientWithep");
    }

}
