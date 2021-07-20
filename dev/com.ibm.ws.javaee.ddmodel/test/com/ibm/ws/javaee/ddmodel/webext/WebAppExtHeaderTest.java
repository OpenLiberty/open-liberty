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
package com.ibm.ws.javaee.ddmodel.webext;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.webext.WebExt;

public class WebAppExtHeaderTest extends WebAppExtTestBase {
        
    protected static final String webExtXMINoVersion =
            "<webappext:WebAppExtension" +
                " xmlns:webappext=\"webappext.xmi\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xmi:version=\"2.0\"" +
            ">" + "\n" +
                webExtBodyXMI + "\n" +
            webExtTailXMI;
    
    protected static final String webExtXML10NoNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML10NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML10NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML10NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXMLNoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                // " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML10VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXMLNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML10VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXML11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " version=\"1.1\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXMLBadNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " xmlns=\"http://junk\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    protected static final String webExtXMLBadVersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-ext" +
                " version=\"9.9\"" +
            ">" + "\n" +
                webExtBodyXML + "\n" +
            webExtTailXML;
    
    //

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect web application binding version",
                "XMI",
                parseWebExtXMI( webExtXMI20(), getWebApp24() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect web application binding version",
                "1.0",
                parseWebExtXML(webExtXML10()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect web application binding version",
                "1.1",
                parseWebExtXML(webExtXML11()).getVersion());
    }

    //

    @Test
    public void testXMINoVersion() throws Exception {
        WebExt webExt = parseWebExtXMI(webExtXMINoVersion, getWebApp24());
        Assert.assertEquals("XMI", webExt.getVersion());
    }

    @Test
    public void testXML10NoNamespace() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML10NoNamespace);
        Assert.assertEquals("1.0", webExt.getVersion());
    }

    @Test
    public void testXML10NoSchemaInstance() throws Exception {
        parseWebExtXML(webExtXML10NoSchemaInstance,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void testXML10NoSchemaLocation() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML10NoSchemaLocation);
        Assert.assertEquals("1.0", webExt.getVersion());        
    }    

    @Test
    public void testXML10NoXSI() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML10NoXSI);
        Assert.assertEquals("1.0", webExt.getVersion());
    }

    @Test
    public void testXMLNoVersion() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXMLNoVersion);
        Assert.assertEquals("1.1", webExt.getVersion());
    }

    @Test
    public void testXML10VersionMismatch() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML10VersionMismatch);
        Assert.assertEquals("1.0", webExt.getVersion());
    }

    @Test
    public void testXMLNamespaceOnly() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXMLNamespaceOnly);
        Assert.assertEquals("1.1", webExt.getVersion());
    }

    @Test
    public void testXML10VersionOnly() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML10VersionOnly);
        Assert.assertEquals("1.0", webExt.getVersion());
    }

    @Test
    public void testXML11VersionOnly() throws Exception {
        WebExt webExt = parseWebExtXML(webExtXML11VersionOnly);
        Assert.assertEquals("1.1", webExt.getVersion());
    }
    
    @Test
    public void testXMLBadNamespaceOnly() throws Exception {
        parseWebExtXML(webExtXMLBadNamespaceOnly);
    }

    @Test
    public void testXMLBadVersionOnly() throws Exception {
        parseWebExtXML(webExtXMLBadVersionOnly,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
