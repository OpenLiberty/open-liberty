/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.wsstemplates;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfWssTemplatesTests;
import com.ibm.ws.wssecurity.fat.utils.common.PrepCommonSetup;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfWssTemplatesTestsWithWSDL extends CxfWssTemplatesTests {

    static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.wsstemplates";
    static private final Class<?> thisClass = CxfWssTemplatesTestsWithWSDL.class;
    @Server(serverName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");
        copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");
        //issue 18363
        setFeatureVersion("EE7");

        ShrinkHelper.defaultDropinApp(server, "wsstemplatesclient", "com.ibm.ws.wssecurity.fat.wsstemplatesclient", "test.wssecfvt.wsstemplates",
                                      "test.wssecfvt.wsstemplates.types");
        ShrinkHelper.defaultDropinApp(server, "wsstemplates", "com.ibm.ws.wssecurity.fat.wsstemplates");
        PrepCommonSetup serverObject = new PrepCommonSetup();
        serverObject.prepareSetup(server);

        commonSetUp(serverName, true, "/wsstemplatesclient/CxfWssTemplatesSvcClient");
    }

    public static void copyServerXml(String copyFromFile) throws Exception {

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "copyServerXml", "Copying: " + copyFromFile
                                                 + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

}
