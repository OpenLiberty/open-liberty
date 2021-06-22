/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.webbnd;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.commonbnd.JASPIRef;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestination;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;

public class WebAppBndTest extends WebAppBndTestBase {
    
    private WebApp webApp24;

    protected WebApp getWebApp24() throws Exception {
        if ( webApp24 == null ) {
            webApp24 = parseWebApp(webApp24Head() + "</web-app>");
        }
        return webApp24;
    }
    
    
    @Test
    public void testGetVersion() throws Exception {
        Assert.assertEquals(
                "XMI",
                parseWebAppBndXMI(webAppBinding("") + "</webappbnd:WebAppBinding>",
                                  getWebApp24()).getVersion());

        Assert.assertEquals("Version should be 1.0", "1.0", parseWebAppBndXML(webBnd10() + "</web-bnd>").getVersion());
        Assert.assertEquals("Version should be 1.1", "1.1", parseWebAppBndXML(webBnd11() + "</web-bnd>").getVersion());
        Assert.assertEquals("Version should be 1.2", "1.2", parseWebAppBndXML(webBnd12() + "</web-bnd>").getVersion());
    }

    public void testXMIInXMLError() throws Exception {
        parseWebAppBndXML(
                webAppBinding("") + "</webappbnd:WebAppBinding>",
                "xml.error", "unknown");
    }

    @Test
    public void testVirtualHostDefault() throws Exception {
        WebBnd bnd = parseWebAppBndXML(webBnd10() + "</web-bnd>");
        Assert.assertNull(bnd.getVirtualHost());
    }

    @Test
    public void testVirtualHost() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() +
                    "<virtual-host name=\"vhost0\"/>" +
                "</web-bnd>");

        Assert.assertEquals("vhost0", bnd.getVirtualHost().getName());
    }

    @Test
    public void testVirtualHostXMIDefault() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") + "</webappbnd:WebAppBinding>",
                getWebApp24() );

        Assert.assertNull(bnd.getVirtualHost());
    }

    @Test
    public void testVirtualHostXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding(" virtualHostName=\"vhost0\"") + "</webappbnd:WebAppBinding>",
                getWebApp24() );

        Assert.assertEquals("vhost0", bnd.getVirtualHost().getName());
    }

    @Test
    public void testVirtualHostXMINil() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<virtualHostName xsi:nil=\"true\"/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24());

        Assert.assertNull(bnd.getVirtualHost().getName());
    }

    @Test
    public void testMessageDestination() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() +
                    "<message-destination name=\"md0\" binding-name=\"mdb0\"/>" +
                    "<message-destination name=\"md1\" binding-name=\"mdb1\"/>" +
                "</web-bnd>");

        List<MessageDestination> mds = bnd.getMessageDestinations();
        Assert.assertEquals(mds.toString(), 2, mds.size());
        Assert.assertEquals("md0", mds.get(0).getName());
        Assert.assertEquals("mdb0", mds.get(0).getBindingName());
        Assert.assertEquals("md1", mds.get(1).getName());
        Assert.assertEquals("mdb1", mds.get(1).getBindingName());
    }

    @Test
    public void testMessageDestinationXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<messageDestinations name=\"md0\"/>" +
                    "<messageDestinations name=\"md1\"/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24() );

        List<MessageDestination> mds = bnd.getMessageDestinations();
        Assert.assertTrue(mds.toString(), mds.isEmpty());
    }

    @Test
    public void testJASPIRefDefault() throws Exception {
        WebBnd bnd = parseWebAppBndXML(webBnd10() + "<jaspi-ref/>" + "</web-bnd>");

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefXMIDefault() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<jaspiRefBinding/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24() );

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefProviderName() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() + "<jaspi-ref provider-name=\"pn0\"/>" + "</web-bnd>");

        JASPIRef jr = bnd.getJASPIRef();
        Assert.assertEquals("pn0", jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefProviderNameXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<jaspiRefBinding providerName=\"pn0\"/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24());

        JASPIRef jr = bnd.getJASPIRef();
        Assert.assertEquals("pn0", jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPIYes() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() + "<jaspi-ref use-jaspi=\"yes\"/>" + "</web-bnd>");

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.yes, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPIYesXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<jaspiRefBinding useJaspi=\"yes\"/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24() );

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.yes, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPINo() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() + "<jaspi-ref use-jaspi=\"no\"/>" + "</web-bnd>");

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.no, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPINoXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
                webAppBinding("") +
                    "<jaspiRefBinding useJaspi=\"no\"/>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24() );

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.no, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPIInherit() throws Exception {
        WebBnd bnd = parseWebAppBndXML(
                webBnd10() + "<jaspi-ref use-jaspi=\"inherit\"/>" + "</web-bnd>");

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testJASPIRefUseJASPIInheritXMI() throws Exception {
        WebBnd bnd = parseWebAppBndXMI(
            webAppBinding("") +
                "<jaspiRefBinding useJaspi=\"inherit\"/>" +
            "</webappbnd:WebAppBinding>",
            getWebApp24());

        JASPIRef jr = bnd.getJASPIRef();
        //Assert.assertNull(jr.getProviderName());
        Assert.assertEquals(JASPIRef.UseJASPIEnum.inherit, jr.getUseJASPI());
    }

    @Test
    public void testServiceRefXMI() throws Exception {
        parseWebAppBndXMI(
                webAppBinding("") +
                    "<serviceRefBindings jndiName=\"sr0\">" +
                        "<bindingServiceRef href=\"WEB-INF/web.xml#sr0\"/>" +
                    "</serviceRefBindings>" +
                    "<serviceRefBindings jndiName=\"sr1\">" +
                        "<bindingServiceRef href=\"WEB-INF/web.xml#sr1\"/>" +
                    "</serviceRefBindings>" +
                "</webappbnd:WebAppBinding>",
                getWebApp24());
    }
}
