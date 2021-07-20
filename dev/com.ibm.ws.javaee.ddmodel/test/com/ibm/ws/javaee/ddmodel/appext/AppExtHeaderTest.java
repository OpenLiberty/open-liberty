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
package com.ibm.ws.javaee.ddmodel.appext;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.appext.ApplicationExt;

public class AppExtHeaderTest extends AppExtTestBase {
    
    protected static final String appExtXMINoVersion =
            "<applicationExt:ApplicationExtension" +
                " xmlns:applicationExt=\"applicationExt.xmi\"" +
                " xmlns:commonExt=\"commonExt.xmi\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                // " xmi:version=\"2.0\"" +
            ">" + "\n" +
                "<application href=\"META-INF/application.xml#Application_ID\"/>" + "\n" +
            "</applicationExt:ApplicationExtension>";            

    protected static final String appExtBody =
        "<shared-session-context value=\"true\"/>";
    
    protected static final String appExt11NoNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExt11NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +    
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExt11NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +        
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExt11NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +            
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExt11NoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                // " version=\"1.1\"" +
            ">" + "\n" +                
                appExtBody + "\n" +
            "</application-ext>";

    protected static final String appExt11VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +               
                appExtBody + "\n" +
            "</application-ext>";

    protected static final String appExtNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext xmlns=\"http://websphere.ibm.com/xml/ns/javaee\">" + "\n" +
                appExtBody + "\n" +
            "</application-ext>";

    protected static final String appExt11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext version=\"1.1\">" + "\n" +                   
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExtBadNamespace=
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext xmlns=\"http://junk\">" + "\n" +
                appExtBody + "\n" +
            "</application-ext>";
    
    protected static final String appExtBadVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-ext version=\"9.9\">" + "\n" +
                appExtBody + "\n" +
            "</application-ext>";

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect application extension version",
                "XMI",
                parseAppExtXMI( appExtXMI(), app14() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect application extension version",
                "1.0",
                parseAppExtXML(appExt10()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect application extension version",
                "1.1",
                parseAppExtXML(appExt11()).getVersion());
    }
    
    //

    @Test
    public void testXMINoVersion() throws Exception {
        ApplicationExt appExt = parseAppExtXMI(appExtXMINoVersion, app14());
        Assert.assertEquals("XMI", appExt.getVersion());
    }

    @Test
    public void test11NoNamespace() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11NoNamespace);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void test11NoSchemaInstance() throws Exception {
        parseAppExtXML(appExt11NoSchemaInstance,
                       XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void test11NoSchemaLocation() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11NoSchemaLocation);
        Assert.assertEquals("1.1", appExt.getVersion());        
    }    

    @Test
    public void test11NoXSI() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11NoXSI);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void test11NoVersion() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11NoVersion);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void test11VersionMismatch() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11VersionMismatch);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void testNamespaceOnly() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExtNamespaceOnly);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void test11VersionOnly() throws Exception {
        ApplicationExt appExt = parseAppExtXML(appExt11VersionOnly);
        Assert.assertEquals("1.1", appExt.getVersion());
    }

    @Test
    public void testBadNamespace() throws Exception {
        parseAppExtXML(appExtBadNamespace);
    }

    @Test
    public void testBadVersion() throws Exception {
        parseAppExtXML(appExtBadVersion,
                       UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                       UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
