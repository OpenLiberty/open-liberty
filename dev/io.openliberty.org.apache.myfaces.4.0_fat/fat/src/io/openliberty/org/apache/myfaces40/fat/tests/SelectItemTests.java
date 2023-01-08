/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *  Verifies that the f:selectItemGroup and f:selectItemGroups output the correct selections.
 */
@RunWith(FATRunner.class)
public class SelectItemTests {

    protected static final Class<?> clazz = SelectItemTests.class;

    private static final String APP_NAME = "SelectItemTests";

    @Server("faces40_selectItemsServer")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", 
            "io.openliberty.org.apache.faces40.fat.selectitemgroup.beans",
            "io.openliberty.org.apache.faces40.fat.selectitemgroups.beans");


        server.startServer(SelectItemTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Validates each country in single destination list appear. (List found in SelectItemGroupBean)
     * Note: SelectItemGroupBean#Destination's countries variable is a String. (Each destination is a single country)
     */
    @Test
    public void testSelectItemGroup() throws Exception {

        try (WebClient webClient = new WebClient()) {

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/selectItemGroup.xhtml");
            
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(clazz, name.getMethodName(), page.asXml());

            String[] expected = {"optgroup label=\"Europe\"","Germany","France","optgroup label=\"Asia\"","Japan","South Korea"};

            String actual = page.asXml();

            for(String item : expected){
                assertTrue("Expected value was not found " + item, actual.contains(item));
            }
        }
    }

    /*
     * Validates each country in the two destinations lists appear. (List found in SelectItemGroupsBean)
     * Note: SelectItemGroupsBean#DestinationGroup's countries variable is an Arraylist.
     */
    @Test
    public void testSelectItemGroups() throws Exception {

        try (WebClient webClient = new WebClient()) {

            URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/selectItemGroups.xhtml");
            
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(clazz, name.getMethodName(), page.asXml());

            String[] expected = {"optgroup label=\"Africa\"","Egypt","Kenya","optgroup label=\"South America\"","Peru","Argentina"};

            String actual = page.asXml();

            for(String item : expected){
                assertTrue("Expected value was not found " + item, actual.contains(item));
            }
        }
    }
}
