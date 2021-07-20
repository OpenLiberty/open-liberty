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
package com.ibm.ws.javaee.ddmodel.ejbext;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;

@RunWith(Parameterized.class)
public class EJBJarExtHeaderTest extends EJBJarExtTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
        
    public EJBJarExtHeaderTest(boolean ejbInWar) {
        super(ejbInWar);
    }
        
    protected String ejbJarExtXMINoVersion() {
        return "<ejbext:EJBJarExtension" +
                   " xmlns:ejbext=\"ejbext.xmi\"" +
                   " xmlns:commonExt=\"commonExt.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   // " xmi:version=\"2.0\"" +
               ">" + "\n" +
                   "<ejbJar href=\"" + getEJBJarPath() + "#EJBJar_ID\"/>" + "\n" +
                   ejbExtBodyXMI() + "\n" +
               ejbExtXMITail;
    }

    protected static final String ejbJarExt11NoNamespace =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<ejb-jar-ext" +
            // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
            " version=\"1.1\"" +
        ">" + "\n" +
            ejbExtBodyXML + "\n" +
        ejbExtXMLTail;

    
    protected static final String ejbJarExt11NoSchemaInstance =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<ejb-jar-ext" +
            " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
            " version=\"1.1\"" +
        ">" + "\n" +
            ejbExtBodyXML + "\n" +
        ejbExtXMLTail;
    
    protected static final String ejbJarExt11NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;
    
    protected static final String ejbJarExt11NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;            
    
    protected static final String ejbJarExtXMLNoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
                // " version=\"1.1\"" +
            ">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;

    
    protected static final String ejbJarExtXML11VersionMismatch =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://junk\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;
    
    protected static final String ejbJarExtXMLNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext xmlns=\"http://websphere.ibm.com/xml/ns/javaee\">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;
    
    protected static final String ejbJarExtXML11VersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext version=\"1.1\">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;
    
    protected static final String ejbJarExtXMLBadNamespaceOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext xmlns=\"http://junk\">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;
    
    protected static final String ejbJarExtXMLBadVersionOnly =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext version=\"9.9\">" + "\n" +
                ejbExtBodyXML + "\n" +
            ejbExtXMLTail;

    @Test
    public void testXMIGetVersion() throws Exception {
        Assert.assertEquals("Incorrect ejb binding version",
                "XMI",
                parseEJBJarExtXMI( ejbJarExtXMI("", ""), getEJBJar21() ).getVersion());
    }

    @Test
    public void testXMLGetVersion10() throws Exception {
        Assert.assertEquals("Incorrect ejb binding version",
                "1.0",
                parseEJBJarExtXML( ejbJarExt10() ).getVersion());
    }
    
    @Test
    public void testXMLGetVersion11() throws Exception {    
        Assert.assertEquals("Incorrect ejb binding version",
                "1.1",
                parseEJBJarExtXML(ejbJarExt11()).getVersion());
    }
    
    //

    @Test
    public void testXMINoVersion() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXMI(ejbJarExtXMINoVersion(), getEJBJar21());
        Assert.assertEquals("XMI", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLNoNamespace11() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExt11NoNamespace);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLNoSchemaInstance11() throws Exception {
        parseEJBJarExtXML(ejbJarExt11NoSchemaInstance,
                          XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void testXMLNoSchemaLocation11() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExt11NoSchemaLocation);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());        
    }    

    @Test
    public void testXMLNoXSI11() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExt11NoXSI);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLNoVersion() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExtXMLNoVersion);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLVersionMismatch11() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExtXML11VersionMismatch);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLNamespaceOnly() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExtXMLNamespaceOnly);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLVersionOnly11() throws Exception {
        EJBJarExt ejbJarExt = parseEJBJarExtXML(ejbJarExtXML11VersionOnly);
        Assert.assertEquals("1.1", ejbJarExt.getVersion());
    }

    @Test
    public void testXMLBadNamespace() throws Exception {
        parseEJBJarExtXML(ejbJarExtXMLBadNamespaceOnly);
    }

    @Test
    public void testXMLBadVersion() throws Exception {
        parseEJBJarExtXML(ejbJarExtXMLBadVersionOnly,
                          UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                          UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
