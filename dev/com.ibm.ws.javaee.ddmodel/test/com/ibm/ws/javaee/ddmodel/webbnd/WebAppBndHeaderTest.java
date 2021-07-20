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

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.webbnd.WebBnd;

public class WebAppBndHeaderTest extends WebAppBndTestBase {
        
    protected static final String webBndXMINoVersion =
            "<webappbnd:WebAppBinding" +
                " xmlns:webappbnd=\"webappbnd.xmi\"" +
                " xmlns:commonbnd=\"commonbnd.xmi\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xmi:version=\"2.0\"" +
            ">" + "\n" +
                webBndXMIBody() + "\n" +
            webBndTailXMI;
    
    protected static final String webBndXML10NoNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML10NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML10NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML10NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXMLNoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                // " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML10VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXMLNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML10VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " version=\"1.0\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " version=\"1.1\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXML12VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " version=\"1.2\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXMLBadNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " xmlns=\"http://junk\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    
    protected static final String webBndXMLBadVersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<web-bnd" +
                " version=\"9.9\"" +
            ">" + "\n" +
                webBndXMLBody() + "\n" +
            webBndTailXML;
    //

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect web application binding version",
                "XMI",
                parseWebBndXMI( webBndXMI20(), getWebApp24() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect web application binding version",
                "1.0",
                parseWebBndXML(webBndXML10()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect web application binding version",
                "1.1",
                parseWebBndXML(webBndXML11()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion12() throws Exception {    
        Assert.assertEquals("Incorrect web application binding version",
                "1.2",
                parseWebBndXML(webBndXML12()).getVersion());
    }    

    //

    @Test
    public void testXMINoVersion() throws Exception {
        WebBnd webBnd = parseWebBndXMI(webBndXMINoVersion, getWebApp24());
        Assert.assertEquals("XMI", webBnd.getVersion());
    }

    @Test
    public void testXML10NoNamespace() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML10NoNamespace);
        Assert.assertEquals("1.0", webBnd.getVersion());
    }

    @Test
    public void testXML10NoSchemaInstance() throws Exception {
        parseWebBndXML(webBndXML10NoSchemaInstance,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void testXML10NoSchemaLocation() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML10NoSchemaLocation);
        Assert.assertEquals("1.0", webBnd.getVersion());        
    }    

    @Test
    public void testXML10NoXSI() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML10NoXSI);
        Assert.assertEquals("1.0", webBnd.getVersion());
    }

    @Test
    public void testXMLNoVersion() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXMLNoVersion);
        Assert.assertEquals("1.2", webBnd.getVersion());
    }

    @Test
    public void testXML10VersionMismatch() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML10VersionMismatch);
        Assert.assertEquals("1.0", webBnd.getVersion());
    }

    @Test
    public void testXMLNamespaceOnly() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXMLNamespaceOnly);
        Assert.assertEquals("1.2", webBnd.getVersion());
    }

    @Test
    public void testXML10VersionOnly() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML10VersionOnly);
        Assert.assertEquals("1.0", webBnd.getVersion());
    }

    @Test
    public void testXML11VersionOnly() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML11VersionOnly);
        Assert.assertEquals("1.1", webBnd.getVersion());
    }

    @Test
    public void testXML12VersionOnly() throws Exception {
        WebBnd webBnd = parseWebBndXML(webBndXML12VersionOnly);
        Assert.assertEquals("1.2", webBnd.getVersion());
    }
    
    @Test
    public void testXMLBadNamespaceOnly() throws Exception {
        parseWebBndXML(webBndXMLBadNamespaceOnly);
    }

    @Test
    public void testXMLBadVersionOnly() throws Exception {
        parseWebBndXML(webBndXMLBadVersionOnly,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
