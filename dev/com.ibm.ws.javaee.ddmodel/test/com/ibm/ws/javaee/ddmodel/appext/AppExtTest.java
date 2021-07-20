/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.appext;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appext.ApplicationExt;
import com.ibm.ws.javaee.dd.appext.ModuleExtension;

public class AppExtTest extends AppExtTestBase {
    @Test
    public void testGetVersion() throws Exception {
        Assert.assertEquals("XMI", parseAppExtXMI(appExtXMI(), app14()).getVersion());
        Assert.assertEquals("Version should be 1.0", "1.0", parseAppExtXML(appExt10() ).getVersion());
        Assert.assertEquals("Version should be 1.1", "1.1", parseAppExtXML(appExt11() ).getVersion());
    }

    @Test
    public void testClientMode() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt10());
        Assert.assertNull(appExt.getClientMode());
        Assert.assertFalse(appExt.isSetClientMode());

        appExt = parseAppExtXML(appExt10("client-mode=\"ISOLATED\"", "") );
        Assert.assertEquals(ApplicationExt.ClientModeEnum.ISOLATED, appExt.getClientMode());
        Assert.assertTrue(appExt.isSetClientMode());

        appExt = parseAppExtXML(appExt10("client-mode=\"FEDERATED\"", "") );
        Assert.assertEquals(ApplicationExt.ClientModeEnum.FEDERATED, appExt.getClientMode());
        Assert.assertTrue(appExt.isSetClientMode());

        appExt = parseAppExtXML(appExt10("client-mode=\"SERVER_DEPLOYED\"", "") );
        Assert.assertEquals(ApplicationExt.ClientModeEnum.SERVER_DEPLOYED, appExt.getClientMode());
        Assert.assertTrue(appExt.isSetClientMode());
    }

    @Test
    public void testModuleExtension() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt10() );
        Assert.assertEquals(Collections.emptyList(), appExt.getModuleExtensions());

        appExt = parseAppExtXML(appExt10("",
                "<module-extension/>" +
                    "<module-extension name=\"n\">" +
                    "<alt-bindings uri=\"ab\"/>" +
                    "<alt-extensions uri=\"ae\"/>" +
                    "<alt-root uri=\"ar\"/>" +
                    "<absolute-path path=\"ap\"/>" +
                "</module-extension>"));

        List<ModuleExtension> mes = appExt.getModuleExtensions();
        Assert.assertEquals(mes.toString(), 2, mes.size());

        ModuleExtension me = mes.get(0);
        Assert.assertNull(me.getName());
        Assert.assertNull(me.getAltBindings());
        Assert.assertFalse(me.isSetAltBindings());
        Assert.assertNull(me.getAltExtensions());
        Assert.assertFalse(me.isSetAltExtensions());
        Assert.assertNull(me.getAltRoot());
        Assert.assertFalse(me.isSetAltRoot());
        Assert.assertNull(me.getAbsolutePath());
        Assert.assertFalse(me.isSetAbsolutePath());

        me = mes.get(1);
        Assert.assertEquals("n", me.getName());
        Assert.assertEquals("ab", me.getAltBindings());
        Assert.assertTrue(me.isSetAltBindings());
        Assert.assertEquals("ae", me.getAltExtensions());
        Assert.assertTrue(me.isSetAltExtensions());
        Assert.assertEquals("ar", me.getAltRoot());
        Assert.assertTrue(me.isSetAltRoot());
        Assert.assertEquals("ap", me.getAbsolutePath());
        Assert.assertTrue(me.isSetAbsolutePath());
    }

    @Test
    public void testXMIModuleExtension() throws Exception {
        ApplicationExt appExt = parseAppExtXMI( appExtXMI(), app14() );

        Assert.assertEquals(Collections.emptyList(), appExt.getModuleExtensions());

        Application app = parseApp(
                app14Head + 
                    "<module id=\"Module_ID\">" +
                        "<web>" +
                            "<web-uri>n</web-uri>" +
                        "</web>" +
                    "</module>" +
                 appTail);

        appExt = parseAppExtXMI(appExtXMI(
                "xmi:id=\"ApplicationExtension_ID\"",
                "<moduleExtensions dependentClasspath=\"dc\">" +
                    "<applicationExtension href=\"META-INF/ibm-application-ext.xmi#ApplicationExtension_ID\"/>" +
                "</moduleExtensions>" +
                "<moduleExtensions altBindings=\"ab\" altExtensions=\"ae\" altRoot=\"ar\" absolutePath=\"ap\" dependentClasspath=\"dc\">" +
                    "<module href=\"META-INF/application.xml#Module_ID\"/>" +
                "</moduleExtensions>"),
                app);

        List<ModuleExtension> mes = appExt.getModuleExtensions();
        Assert.assertEquals(mes.toString(), 2, mes.size());

        ModuleExtension me = mes.get(0);
        Assert.assertNull(me.getName());
        Assert.assertNull(me.getAltBindings());
        Assert.assertFalse(me.isSetAltBindings());
        Assert.assertNull(me.getAltExtensions());
        Assert.assertFalse(me.isSetAltExtensions());
        Assert.assertNull(me.getAltRoot());
        Assert.assertFalse(me.isSetAltRoot());
        Assert.assertNull(me.getAbsolutePath());
        Assert.assertFalse(me.isSetAbsolutePath());

        me = mes.get(1);
        Assert.assertEquals("n", me.getName());
        Assert.assertEquals("ab", me.getAltBindings());
        Assert.assertTrue(me.isSetAltBindings());
        Assert.assertEquals("ae", me.getAltExtensions());
        Assert.assertTrue(me.isSetAltExtensions());
        Assert.assertEquals("ar", me.getAltRoot());
        Assert.assertTrue(me.isSetAltRoot());
        Assert.assertEquals("ap", me.getAbsolutePath());
        Assert.assertTrue(me.isSetAbsolutePath());
    }

    @Test
    public void testXMLReloadInterval() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt10() );
        Assert.assertFalse(appExt.isSetReloadInterval());

        appExt = parseAppExtXML(appExt10("", "<reload-interval value=\"1234\"/>"));
        Assert.assertTrue(appExt.isSetReloadInterval());
        Assert.assertEquals(1234, appExt.getReloadInterval());
    }

    @Test
    public void testXMIReloadInterval() throws Exception {
        ApplicationExt appExt = parseAppExtXMI(appExtXMI(), app14());
        Assert.assertFalse(appExt.isSetReloadInterval());

        appExt = parseAppExtXMI(appExtXMI("", "<reloadInterval xsi:nil=\"true\"/>"), app14());
        Assert.assertFalse(appExt.isSetReloadInterval());

        appExt = parseAppExtXMI(appExtXMI("reloadInterval=\"1234\"", ""), app14());
        Assert.assertTrue(appExt.isSetReloadInterval());
        Assert.assertEquals(1234, appExt.getReloadInterval());
    }

    @Test
    public void testXMLSharedSessionContext() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt10() );
        Assert.assertFalse(appExt.isSetSharedSessionContext());

        appExt = parseAppExtXML(appExt10("", "<shared-session-context value=\"true\"/>"));
        Assert.assertTrue(appExt.isSetSharedSessionContext());
        Assert.assertTrue(appExt.isSharedSessionContext());
    }

    @Test
    public void testXMISharedSessionContext() throws Exception {
        ApplicationExt appExt = parseAppExtXMI(appExtXMI(), app14());
        Assert.assertFalse(appExt.isSetSharedSessionContext());

        appExt = parseAppExtXMI(appExtXMI("sharedSessionContext=\"true\"", ""), app14());
        Assert.assertTrue(appExt.isSetSharedSessionContext());
        Assert.assertTrue(appExt.isSharedSessionContext());
    }
}
