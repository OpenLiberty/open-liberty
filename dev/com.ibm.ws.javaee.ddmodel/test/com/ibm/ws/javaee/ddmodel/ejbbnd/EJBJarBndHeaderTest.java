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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;

@RunWith(Parameterized.class)
public class EJBJarBndHeaderTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
        
    public EJBJarBndHeaderTest(boolean ejbInWar) {
        super(ejbInWar);
    }
        
    protected static final String ejbJarBndXMINoVersion =
            "<EJBJarBnd:EJBJarBinding" +
                " xmlns:EJBJarBnd=\"EJBJarBnd.xmi\"" +
                " xmlns:commonbnd=\"commonbnd.xmi\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                // " xmi:version=\"2.0\"" +
            "/>";

    protected static final String ejbJarBnd11NoNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            "/>";    
    
    protected static final String ejbJarBnd11NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            "/>";    
    
    protected static final String ejbJarBnd11NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            "/>";        
    
    protected static final String ejbJarBnd11NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            "/>";            
    
    protected static final String ejbJarBnd11NoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                // " version=\"1.1\"" +
            "/>";                
    
    protected static final String ejbJarBnd11VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            "/>";               
    
    protected static final String ejbJarBndNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"/>";
    
    protected static final String ejbJarBnd11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd version=\"1.1\"/>";                   
    
    protected static final String ejbJarBndBadNamespace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd xmlns=\"http://junk\"/>";
    
    protected static final String ejbJarBndBadVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-bnd version=\"9.9\"/>";     

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect ejb binding version",
                "XMI",
                parseEJBJarBndXMI( ejbJarBndXMI("", ""), getEJBJar21() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect ejb binding version",
                "1.0",
                parseEJBJarBndXML(ejbJarBnd10()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect ejb binding version",
                "1.1",
                parseEJBJarBndXML(ejbJarBnd11()).getVersion());
    }
    
    @Test
    public void testXMLGetVersion12() throws Exception {        
        Assert.assertEquals("Incorrect ejb binding version",
                "1.2",
                parseEJBJarBndXML(ejbJarBnd12()).getVersion());
    }

    //

    @Test
    public void testXMINoVersion() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXMI(ejbJarBndXMINoVersion, getEJBJar21());
        Assert.assertEquals("XMI", ejbJarBnd.getVersion());
    }

    @Test
    public void test11NoNamespace() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11NoNamespace);
        Assert.assertEquals("1.1", ejbJarBnd.getVersion());
    }

    @Test
    public void test11NoSchemaInstance() throws Exception {
        parseEJBJarBndXML(ejbJarBnd11NoSchemaInstance,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void test11NoSchemaLocation() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11NoSchemaLocation);
        Assert.assertEquals("1.1", ejbJarBnd.getVersion());        
    }    

    @Test
    public void test11NoXSI() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11NoXSI);
        Assert.assertEquals("1.1", ejbJarBnd.getVersion());
    }

    @Test
    public void test11NoVersion() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11NoVersion);
        Assert.assertEquals("1.2", ejbJarBnd.getVersion());
    }

    @Test
    public void test11VersionMismatch() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11VersionMismatch);
        Assert.assertEquals("1.1", ejbJarBnd.getVersion());
    }

    @Test
    public void testNamespaceOnly() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBndNamespaceOnly);
        Assert.assertEquals("1.2", ejbJarBnd.getVersion());
    }

    @Test
    public void test11VersionOnly() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11VersionOnly);
        Assert.assertEquals("1.1", ejbJarBnd.getVersion());
    }

    @Test
    public void testBadNamespace() throws Exception {
        parseEJBJarBndXML(ejbJarBndBadNamespace);
    }

    @Test
    public void testBadVersion() throws Exception {
        parseEJBJarBndXML(ejbJarBndBadVersion,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
