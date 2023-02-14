/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JAXBToolsTest extends FATServletClient {

    private static final Class<?> c = JAXBToolsTest.class;

    // This server doesn't ever get started, we just have it here to get a hold of files from the Liberty install
    @Server("com.ibm.ws.jaxb.tools.TestServer")
    public static LibertyServer server;

    private static File xjcSourceDir;
    private static File xjcTargetDir;
    private static File schemagenSourceDir;
    private static File schemagenTargetDir;
    private static File jakartaSchemagenSourceDir;
    private static File jakartaSchemagenTargetDir;

    private static Machine machine;
    private static String installRoot;

    @BeforeClass
    public static void setup() throws Exception {

        String serverRoot = server.getServerRoot();
        xjcSourceDir = new File(serverRoot + "/temp/xjcSourceDir");
        xjcTargetDir = new File(serverRoot + "/temp/xjcTargetDir");
        schemagenSourceDir = new File(serverRoot + "/temp/schemagenSourceDir");
        schemagenTargetDir = new File(serverRoot + "/temp/schemagenTargetDir");

        jakartaSchemagenSourceDir = new File(serverRoot + "/temp/jakartaSchemagenSourceDir");
        jakartaSchemagenTargetDir = new File(serverRoot + "/temp/jakartaSchemagenTargetDir");

        xjcTargetDir.mkdirs();
        schemagenTargetDir.mkdirs();
        jakartaSchemagenTargetDir.mkdirs();

        installRoot = server.getInstallRoot();
        machine = server.getMachine();
    }

    @Test
    public void testXJCToolWithoutTarget() throws Exception {
        String[] xjcArgs = { "-p",
                             "po",
                             "-d",
                             xjcTargetDir.getAbsolutePath(),
                             xjcSourceDir.getAbsolutePath() + File.separator + "purchaseOrder.xsd" };
        RemoteFile xjc;
        RemoteFile xjcBat;
        if (JakartaEE9Action.isActive()) {
            xjc = server.getFileFromLibertyInstallRoot("bin/xmlBinding/xjc");
            xjcBat = server.getFileFromLibertyInstallRoot("bin/xmlBinding/xjc.bat");

            assertTrue("The file bin/xmlBinding/xjc does not exist.", xjc.exists());
            assertTrue("The file bin/xmlBinding/xjc.bat does not exist.", xjcBat.exists());
        } else {
            xjc = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc");
            xjcBat = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc.bat");

            assertTrue("The file bin/jaxb/xjc does not exist.", xjc.exists());
            assertTrue("The file bin/jaxb/xjc.bat does not exist.", xjcBat.exists());
        }

        ProgramOutput po = machine.execute(xjc.getAbsolutePath(), xjcArgs, installRoot);

        Log.info(c, "testXJCToolWithoutTarget", "Executed " + xjc.getAbsolutePath() + " command:" + po.getCommand());
        assertTrue("The output should contain the error id 'CWWKW1400E', 'CWWKW1401E', or 'CWWKW1402E', but does not.\nActual output:\n" + po.getStdout(),
                   ((po.getStdout().indexOf("CWWKW1400E") >= 0) || (po.getStdout().indexOf("CWWKW1401E") >= 0) || (po.getStdout().indexOf("CWWKW1402E") >= 0)));
    }

    @Test
    public void testXJCTool() throws Exception {
        String[] xjcArgs;
        RemoteFile xjc;
        RemoteFile xjcBat;
        if (JakartaEE9Action.isActive()) {
            xjcArgs = new String[] { "-p",
                                     "po",
                                     "-d",
                                     xjcTargetDir.getAbsolutePath(),
                                     "-target 3.0",
                                     xjcSourceDir.getAbsolutePath() + File.separator + "purchaseOrder.xsd" };
            xjc = server.getFileFromLibertyInstallRoot("bin/xmlBinding/xjc");
            xjcBat = server.getFileFromLibertyInstallRoot("bin/xmlBinding/xjc.bat");

            assertTrue("The file bin/xmlBinding/xjc does not exist.", xjc.exists());
            assertTrue("The file bin/xmlBinding/xjc.bat does not exist.", xjcBat.exists());
        } else {
            xjcArgs = new String[] { "-p",
                                     "po",
                                     "-d",
                                     xjcTargetDir.getAbsolutePath(),
                                     "-target 2.2",
                                     xjcSourceDir.getAbsolutePath() + File.separator + "purchaseOrder.xsd" };
            xjc = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc");
            xjcBat = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc.bat");

            assertTrue("The file bin/jaxb/xjc does not exist.", xjc.exists());
            assertTrue("The file bin/jaxb/xjc.bat does not exist.", xjcBat.exists());
        }

        ProgramOutput po = machine.execute(xjc.getAbsolutePath(), xjcArgs, installRoot);

        Log.info(c, "testXJCTool", "Executed command:" + po.getCommand());

        RemoteFile itemsJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po/Items.java");
        RemoteFile purchaseOrderTypeJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po" + File.separator
                                                                                   + "PurchaseOrderType.java");
        RemoteFile shippingAddressJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po" + File.separator
                                                                                 + "ShippingAddress.java");
        assertTrue("Items.java does not exist.", itemsJavaFile.exists());
        assertTrue("PurchaseOrderType.java does not exist.", purchaseOrderTypeJavaFile.exists());
        assertTrue("ShippingAddress.java does not exist.", shippingAddressJavaFile.exists());
    }

    @Test
    public void testSchemaGenTool() throws Exception {
        String[] schemagenArgs;
        RemoteFile schemagen;
        RemoteFile schemagenBat;
        if (JakartaEE9Action.isActive()) {
            schemagenArgs = new String[] { "-d",
                                           jakartaSchemagenTargetDir.getAbsolutePath(),
                                           jakartaSchemagenSourceDir.getAbsolutePath() + File.separator + "Items.java",
                                           jakartaSchemagenSourceDir.getAbsolutePath() + File.separator + "PurchaseOrderType.java",
                                           jakartaSchemagenSourceDir.getAbsolutePath() + File.separator + "ShippingAddress.java" };
            schemagen = server.getFileFromLibertyInstallRoot("bin/xmlBinding/schemagen");
            schemagenBat = server.getFileFromLibertyInstallRoot("bin/xmlBinding/schemagen.bat");

            assertTrue("The file bin/xmlBinding/schemagen does not exist.", schemagen.exists());
            assertTrue("The file bin/xmlBinding/schemagen.bat does not exist.", schemagenBat.exists());
        } else {
            schemagenArgs = new String[] { "-d",
                                           schemagenTargetDir.getAbsolutePath(),
                                           schemagenSourceDir.getAbsolutePath() + File.separator + "Items.java",
                                           schemagenSourceDir.getAbsolutePath() + File.separator + "PurchaseOrderType.java",
                                           schemagenSourceDir.getAbsolutePath() + File.separator + "ShippingAddress.java" };
            schemagen = server.getFileFromLibertyInstallRoot("bin/jaxb/schemagen");
            schemagenBat = server.getFileFromLibertyInstallRoot("bin/jaxb/schemagen.bat");

            assertTrue("The file bin/jaxb/schemagen does not exist.", schemagen.exists());
            assertTrue("The file bin/jaxb/schemagen.bat does not exist.", schemagenBat.exists());
        }

        machine.execute(schemagen.getAbsolutePath(), schemagenArgs, installRoot);

        RemoteFile xsdFile;
        RemoteFile itemsClassFile;
        RemoteFile purchaseOrderTypeClassFile;
        RemoteFile shippingAddressClassFile;

        if (JakartaEE9Action.isActive()) {
            xsdFile = server.getFileFromLibertyServerRoot("temp/jakartaSchemagenTargetDir/schema1.xsd");
            itemsClassFile = server.getFileFromLibertyServerRoot("temp/jakartaSchemagenTargetDir/po/Items.class");
            purchaseOrderTypeClassFile = server.getFileFromLibertyServerRoot("temp/jakartaSchemagenTargetDir/po/PurchaseOrderType.class");
            shippingAddressClassFile = server.getFileFromLibertyServerRoot("temp/jakartaSchemagenTargetDir/po/ShippingAddress.class");
        } else {
            xsdFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/schema1.xsd");
            itemsClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/Items.class");
            purchaseOrderTypeClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/PurchaseOrderType.class");
            shippingAddressClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/ShippingAddress.class");
        }
        assertTrue("schema1.xsd does not exist.", xsdFile.exists());
        assertTrue("Items.class does not exist.", itemsClassFile.exists());
        assertTrue("PurchaseOrderType.class does not exist.", purchaseOrderTypeClassFile.exists());
        assertTrue("ShippingAddress.class does not exist.", shippingAddressClassFile.exists());
    }
}
