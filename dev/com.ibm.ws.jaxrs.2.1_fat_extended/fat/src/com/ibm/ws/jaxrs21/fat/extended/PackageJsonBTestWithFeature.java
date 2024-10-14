/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.fat.extended;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21.fat.jsonb.JsonBTestServlet;

@SkipForRepeat({SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.EE11_FEATURES}) //JSON-B Provider implementation seems to be broken in Jakarta package space...
@RunWith(FATRunner.class)
public class PackageJsonBTestWithFeature extends FATServletClient {

    private static final String appName = "jsonbapp";

    @Server("jaxrs21.fat.packageJsonBWithFeature")
    @TestServlet(servlet = JsonBTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            Files.newDirectoryStream(Paths.get("publish/shared/resources/johnzon"))
                 .forEach(path -> {
                     Path newPath = Paths.get("publish/shared/resources/johnzon/" + path.getFileName() + ".jakarta.jar");
                     System.out.println("transforming " + path + " to " + newPath);
                     JakartaEEAction.transformApp(path, newPath);
                 });
        }
        ShrinkHelper.defaultDropinApp(server, appName, "jaxrs21.fat.jsonb");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}