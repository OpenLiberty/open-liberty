/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;

public class JSFAppHeaderTest extends JSFAppTestBase {
 
    protected static final String jsf20NoSchema =
            "<faces-config" +
                // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
                " version=\"2.0\"/>";    

    protected static final String jsf20NoSchemaInstance =
            "<faces-config" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
                " version=\"2.0\"/>";    
    
    protected static final String jsf20NoSchemaLocation =
            "<faces-config" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
                " version=\"2.0\"/>";    
    
    protected static final String jsf20NoXSI =
            "<faces-config" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
                " version=\"2.0\"/>";
    
    protected static final String jsf20NoVersion =
            "<faces-config" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_2_0.xsd\"" +
                // " version=\"2.0\"" + 
            "/>";
    
    //
    
    protected static final String jsf12NamespaceOnly = 
            "<faces-config xmlns=\"http://java.sun.com/xml/ns/javaee\"/>";

    protected static final String jsf20VersionOnly =
            "<faces-config version=\"2.0\"/>";

    protected static final String jsf22NamespaceOnly =
            "<faces-config xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"/>";
    
    //
    
    protected static final String jsf20VersionMismatch =
            "<faces-config" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " version=\"2.0\"/>";
    
    protected static final String jsfNamespaceOnlyUnknown =
            "<faces-config xmlns=\"junk\"/>";

    protected static final String jsfVersionOnlyUnknown =
            "<faces-config version=\"9.9\"/>";

    //

    @Test
    public void testJSF20_NoSchema() throws Exception {
        FacesConfig facesConfig = parse(jsf20NoSchema, FacesConfig.VERSION_2_3 );
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF20_NoSchemaInstance() throws Exception {
        parse(jsf20NoSchemaInstance, FacesConfig.VERSION_2_3, "CWWKC2272E", "xml.error");
    }

    @Test
    public void testJSF20_NoSchemaLocation() throws Exception {
        FacesConfig facesConfig = parse(jsf20NoSchemaLocation, FacesConfig.VERSION_2_3 );
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF20_NoXSI() throws Exception {
        FacesConfig facesConfig = parse(jsf20NoXSI, FacesConfig.VERSION_2_3 );
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF20_NoVersion() throws Exception {
        FacesConfig facesConfig = parse(jsf20NoVersion, FacesConfig.VERSION_2_3 );
         // 2.1 is the highest provisioned schema which matches
        Assert.assertEquals( "2.1", facesConfig.getVersion() );
    }

    //

    // 1.2, 2.0, and 2.1 all use the sun J2EE namespace
    // 2.2 and 2.3 use the JSP JavaEE namespace

    @Test
    public void testJSF12_NamespaceOnly() throws Exception {
        FacesConfig facesConfig = parse(jsf12NamespaceOnly, FacesConfig.VERSION_2_3 );
         // 2.1 is the highest provisioned schema which matches
        Assert.assertEquals( "2.1", facesConfig.getVersion() );
    }

    @Test
    public void testJSF20_VersionOnly() throws Exception {
        FacesConfig facesConfig = parse(jsf20VersionOnly, FacesConfig.VERSION_2_3 );
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF22_NamespaceOnly_At22() throws Exception {
        FacesConfig facesConfig = parse(jsf22NamespaceOnly, FacesConfig.VERSION_2_2 );
        Assert.assertEquals( "2.2", facesConfig.getVersion() );
    }

    @Test
    public void testJSF22_NamespaceOnly_At23() throws Exception {
        FacesConfig facesConfig = parse(jsf22NamespaceOnly, FacesConfig.VERSION_2_3 );
        Assert.assertEquals( "2.3", facesConfig.getVersion() );
    }

    //

    @Test
    public void testJSF20_VersionMismatch() throws Exception {
        FacesConfig facesConfig = parse(jsf20VersionMismatch, FacesConfig.VERSION_2_3);
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF_NamespaceOnlyUnknown() throws Exception {
        parse(jsfNamespaceOnlyUnknown, FacesConfig.VERSION_2_3,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testJSF_VersionOnlyUnknown() throws Exception {
        parse(jsfVersionOnlyUnknown, FacesConfig.VERSION_2_3,
              UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}

