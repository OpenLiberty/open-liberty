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
package com.ibm.ws.javaee.ddmodel.appbnd;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;

public class AppBndHeaderTest extends AppBndTestBase {
    
    protected static final String appBndXMINoVersion =
            "<applicationbnd:ApplicationBinding" +
                " xmlns:applicationbnd=\"applicationbnd.xmi\"" +
                " xmlns:commonbnd=\"commonbnd.xmi\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                // " xmi:version=\"2.0\"" +
            ">" + "\n" +
                "<application href=\"META-INF/application.xml#Application_ID\"/>" + "\n" +
            "</applicationbnd:ApplicationBinding>";            

    protected static final String appBndContents =
            "<security-role name=\"snooping\">\n" +
                "<special-subject type=\"ALL_AUTHENTICATED_USERS\" />\n" +
            "</security-role>";

    protected static final String appBnd11NoNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";

    protected static final String appBnd11NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";  

    protected static final String appBnd11NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";

    protected static final String appBnd11NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";                        

    protected static final String appBnd11NoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                // " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";

    protected static final String appBnd11VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" + 
                appBndContents + "\n" +
            "</application-bnd>";

    
    protected static final String appBndNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\">" + "\n" +
                appBndContents + "\n" +
            "</application-bnd>";
    
    
    protected static final String appBnd11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd version=\"1.1\">" + "\n" +
                appBndContents + "\n" +
            "</application-bnd>";                    
    
    protected static final String appBndBadNamespace=
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd xmlns=\"http://junk\">" + "\n" +
                appBndContents + "\n" +
            "</application-bnd>";                    
    
    protected static final String appBndBadVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd version=\"9.9\">" + "\n" +
                appBndContents + "\n" +
            "</application-bnd>";                    

    protected static final String appBndMinimal =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd>" + "\n" +
                appBndContents + "\n" +
            "</application-bnd>";     

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect application binding version",
                "XMI",
                parseAppBndXMI( appBndXMI("", ""), app14() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect application binding version",
                "1.0",
                parseAppBndXML(appBnd10()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect application binding version",
                "1.1",
                parseAppBndXML(appBnd11()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion12() throws Exception {        
        Assert.assertEquals("Incorrect application binding version",
                "1.2",
                parseAppBndXML(appBnd12()).getVersion());
    }

    //

    @Test
    public void testXMINoVersion() throws Exception {
        ApplicationBnd appBnd = parseAppBndXMI(appBndXMINoVersion, app14());
        Assert.assertEquals("XMI", appBnd.getVersion());
    }

    @Test
    public void test11NoNamespace() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11NoNamespace);
        Assert.assertEquals("1.1", appBnd.getVersion());
    }

    @Test
    public void test11NoSchemaInstance() throws Exception {
        parseAppBndXML(appBnd11NoSchemaInstance,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void test11NoSchemaLocation() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11NoSchemaLocation);
        Assert.assertEquals("1.1", appBnd.getVersion());        
    }    

    @Test
    public void test11NoXSI() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11NoXSI);
        Assert.assertEquals("1.1", appBnd.getVersion());
    }

    @Test
    public void test11NoVersion() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11NoVersion);
        Assert.assertEquals("1.2", appBnd.getVersion());
    }

    @Test
    public void test11VersionMismatch() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11VersionMismatch);
        Assert.assertEquals("1.1", appBnd.getVersion());
    }

    @Test
    public void testNamespaceOnly() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBndNamespaceOnly);
        Assert.assertEquals("1.2", appBnd.getVersion());
    }

    @Test
    public void test11VersionOnly() throws Exception {
        ApplicationBnd appBnd = parseAppBndXML(appBnd11VersionOnly);
        Assert.assertEquals("1.1", appBnd.getVersion());
    }

    @Test
    public void testBadNamespace() throws Exception {
        parseAppBndXML(appBndBadNamespace);
    }

    @Test
    public void testBadVersion() throws Exception {
        parseAppBndXML(appBndBadVersion,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
    
    @Test
    public void testMinimal() throws Exception {
        parseAppBndXML(appBndMinimal);
    }
}
