/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.ejb;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ejb.EJBJar;

@RunWith(Parameterized.class)
public class EJBJarHeaderTest extends EJBJarTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }

    public EJBJarHeaderTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    protected static final String ejbJarBody =
            "<display-name>EJBTest</display-name>" + '\n' +
            "<enterprise-beans>" + '\n' +
                "<session id=\"TestBean\">" + '\n' +
                    "<ejb-name>TestBean</ejb-name>" + '\n' +
                    "<home>ejbtest.ejb.ITestHome</home>" + '\n' +
                    "<remote>ejbtest.ejb.ITestRemote</remote>" + '\n' +
                    "<local-home>ejbtest.ejb.TestBeanLocalHome</local-home>" + '\n' +
                    "<local>ejbtest.ejb.TestBeanLocal</local>" + '\n' +
                    "<ejb-class>ejbtest.ejb.TestBean</ejb-class>" + '\n' +
                    "<session-type>Stateful</session-type>" + '\n' +
                    "<transaction-type>Bean</transaction-type>" + '\n' +
                "</session>" + '\n' +
            "</enterprise-beans>";

    // Cases:
    //
    //       xNS xSI xSL xXSI xV | oNS oV VV' | uNS UV
    // ================================================
    //  21   +   +   +   +    +  | +   +  -   |
    //  30   +   +   +   +    +  | +   +  +   |
    //  31   +   +   +   +    +  | -   +  +   |
    //  32   +   +   +   +    +  | -   +  +   |
    //  40   +   +   +   +    +  | +   +  +   |
    //  99                       |            | +   +
    // ================================================

    protected static String ejbJar21NoNamespace =
            ejbJarWithout(EJBJar.VERSION_2_1, HeaderLine.NS, ejbJarBody);
    
//            "<ejb-jar" +
//                // " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
//                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
//                " version=\"2.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar21NoSchemaInstance =
            ejbJarWithout(EJBJar.VERSION_2_1, HeaderLine.SI, ejbJarBody);
    
//            "<ejb-jar" +
//                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
//                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
//                " version=\"2.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar21NoSchemaLocation =
            ejbJarWithout(EJBJar.VERSION_2_1, HeaderLine.SL, ejbJarBody);
    
//            "<ejb-jar" +
//                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
//                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
//                " version=\"2.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";

    protected static String ejbJar21NoXSI =
            ejbJarWithout(EJBJar.VERSION_2_1, HeaderLine.XSI, ejbJarBody);
    
//            "<ejb-jar" +
//                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
//                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
//                " version=\"2.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";

    protected static String ejbJar21NoVersion =
            ejbJarWithout(EJBJar.VERSION_2_1, HeaderLine.V, ejbJarBody);
    
//            "<ejb-jar" +
//                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
//                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
//                // " version=\"2.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";

    //

    protected static String ejbJar30NoNamespace =
            ejbJarWithout(EJBJar.VERSION_3_0, HeaderLine.NS, ejbJarBody);
    
//            "<ejb-jar" +
//                // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
//                " version=\"3.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar30NoSchemaInstance =
            ejbJarWithout(EJBJar.VERSION_3_0, HeaderLine.SI, ejbJarBody);

//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
//                    " version=\"3.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar30NoSchemaLocation =
            ejbJarWithout(EJBJar.VERSION_3_0, HeaderLine.SL, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
//                    " version=\"3.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar30NoXSI =
            ejbJarWithout(EJBJar.VERSION_3_0, HeaderLine.XSI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
//                    " version=\"3.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar30NoVersion =
            ejbJarWithout(EJBJar.VERSION_3_0, HeaderLine.V, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
//                    // " version=\"3.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    //
    
    protected static String ejbJar31NoNamespace =
            ejbJarWithout(EJBJar.VERSION_3_1, HeaderLine.NS, ejbJarBody);
    
//            "<ejb-jar" +
//                    // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
//                    " version=\"3.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar31NoSchemaInstance =
            ejbJarWithout(EJBJar.VERSION_3_1, HeaderLine.SI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
//                    " version=\"3.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar31NoSchemaLocation =
            ejbJarWithout(EJBJar.VERSION_3_1, HeaderLine.SL, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
//                    " version=\"3.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar31NoXSI =
            ejbJarWithout(EJBJar.VERSION_3_1, HeaderLine.XSI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
//                    " version=\"3.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar31NoVersion =
            ejbJarWithout(EJBJar.VERSION_3_1, HeaderLine.V, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
//                    // " version=\"3.1\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    //

    protected static String ejbJar32NoNamespace =
            ejbJarWithout(EJBJar.VERSION_3_2, HeaderLine.NS, ejbJarBody);
    
//            "<ejb-jar" +
//                    // " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
//                    " version=\"3.2\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar32NoSchemaInstance =
            ejbJarWithout(EJBJar.VERSION_3_2, HeaderLine.SI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
//                    " version=\"3.2\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar32NoSchemaLocation =
            ejbJarWithout(EJBJar.VERSION_3_2, HeaderLine.SL, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
//                    " version=\"3.2\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar32NoXSI =
            ejbJarWithout(EJBJar.VERSION_3_2, HeaderLine.XSI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
//                    " version=\"3.2\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar32NoVersion =
            ejbJarWithout(EJBJar.VERSION_3_2, HeaderLine.V, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
//                    // " version=\"3.2\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    //
    
    protected static String ejbJar40NoNamespace =
            ejbJarWithout(EJBJar.VERSION_4_0, HeaderLine.NS, ejbJarBody);
    
//            "<ejb-jar" +
//                    // " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
//                    " version=\"4.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar40NoSchemaInstance =
            ejbJarWithout(EJBJar.VERSION_4_0, HeaderLine.SI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
//                    " version=\"4.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar40NoSchemaLocation =
            ejbJarWithout(EJBJar.VERSION_4_0, HeaderLine.SL, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
//                    " version=\"4.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar40NoXSI =
            ejbJarWithout(EJBJar.VERSION_4_0, HeaderLine.XSI, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
//                    // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    // " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
//                    " version=\"4.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
    
    protected static String ejbJar40NoVersion =
            ejbJarWithout(EJBJar.VERSION_4_0, HeaderLine.V, ejbJarBody);
    
//            "<ejb-jar" +
//                    " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
//                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
//                    " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
//                    // " version=\"4.0\"" +
//            ">" + '\n' +
//                ejbJarBody + '\n' +
//            "</ejb-jar>";
        
    //
    
    protected static String ejbJar21NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/j2ee\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar21VersionOnly =
            "<ejb-jar version=\"2.1\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";

    //

    protected static String ejbJar30NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar30VersionOnly =
            "<ejb-jar version=\"3.0\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar30VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"3.0\"" +
            ">" + '\n' +    
                ejbJarBody + '\n' +
            "</ejb-jar>";

    //
    
    protected static String ejbJar31VersionOnly =
            "<ejb-jar version=\"3.1\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar31VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"3.1\"" +
            ">" + '\n' +    
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    //
    
    protected static String ejbJar32VersionOnly =
            "<ejb-jar version=\"3.2\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar32VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"3.2\"" +
            ">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";

    //

    protected static String ejbJar40NamespaceOnly =
            "<ejb-jar xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJar40VersionOnly =
            "<ejb-jar version=\"4.0\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";

    protected static String ejbJar40VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"4.0\"" +
            ">" + '\n' +    
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    //
    
    protected static String ejbJarUnknownNamespace =
            "<ejb-jar xmlns=\"http://junk\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    protected static String ejbJarUnknownVersion =
            "<ejb-jar version=\"9.9\">" + '\n' +
                ejbJarBody + '\n' +
            "</ejb-jar>";
    
    //
    
    @Test
    public void testEJB21NoNamespace() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21NoNamespace);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoSchemaInstance() throws Exception {
        parseEJBJar(ejbJar21NoSchemaInstance, EJBJar.VERSION_3_0,
                    "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEJB21NoSchemaLocation() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21NoSchemaLocation);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoXSI() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21NoXSI);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoVersion() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21NoVersion);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    //
    
    @Test
    public void testEJB21NamespaceOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21NamespaceOnly);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar21VersionOnly);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    // The 30 namespace is common to EJB 30 and 3.1.
    //
    // The maximum supported specification version is adjusted
    // to a minimum of 3.1.
    //
    // That prevents the 30 namespace from selecting 3.0 as the
    // descriptor version, as 3.1 is always available.

    @Test
    public void testEJB30NamespaceOnlyAt30() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar30NamespaceOnly, EJBJar.VERSION_3_0);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB30NamespaceOnlyAt31() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar30NamespaceOnly, EJBJar.VERSION_3_1);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }    
    
    @Test
    public void testEJB30NamespaceOnlyAt40() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar30NamespaceOnly, EJBJar.VERSION_4_0);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }        

    @Test
    public void testEJB30VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar30VersionOnly);
        Assert.assertEquals( 30, ejbJar.getVersionID() );
    }

    //

    @Test
    public void testEJB31NoNamespace() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31NoNamespace);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB31NoSchemaInstance() throws Exception {
        parseEJBJar(ejbJar31NoSchemaInstance, EJBJar.VERSION_3_0,
                    "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEJB31NoSchemaLocation() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31NoSchemaLocation);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB31NoXSI() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31NoXSI);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB31NoVersion() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31NoVersion);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB31VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31VersionOnly);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }
    
    //

    @Test
    public void testEJB32NoNamespace() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32NoNamespace);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB32NoSchemaInstance() throws Exception {
        parseEJBJar(ejbJar32NoSchemaInstance, EJBJar.VERSION_3_0,
                    "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEJB32NoSchemaLocation() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32NoSchemaLocation);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB32NoXSI() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32NoXSI);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB32NoVersion() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32NoVersion);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB32VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32VersionOnly);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }
    
    //

    @Test
    public void testEJB40NoNamespace() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar40NoNamespace);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB40NoSchemaInstance() throws Exception {
        parseEJBJar(ejbJar40NoSchemaInstance, EJBJar.VERSION_3_0,
                    "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEJB40NoSchemaLocation() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar40NoSchemaLocation);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB40NoXSI() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar40NoXSI);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB40NoVersionAt40() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar40NoVersion, EJBJar.VERSION_4_0);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB40NamespaceOnlyAt40() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar40NamespaceOnly, EJBJar.VERSION_4_0);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }
    
    @Test
    public void testEJB40VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar40VersionOnly);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }
    
    //
    
    // A warning is issued, but otherwise the mismatch is ignored
    // and the version has precedence.

    @Test
    public void testEJB30VersionMismatch() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar30VersionMismatch);
        Assert.assertEquals( 30, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB31VersionMismatch() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar31VersionMismatch);
        Assert.assertEquals( 31, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB32VersionMismatch() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar32VersionMismatch);
        Assert.assertEquals( 32, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB40VersionMismatch() throws Exception {
        EJBJar ejbJar = parseEJBJarMax(ejbJar40VersionMismatch);
        Assert.assertEquals( 40, ejbJar.getVersionID() );
    }

    //

    @Test
    public void testEJBUnknownNamespace() throws Exception {
        parseEJBJarMax(ejbJarUnknownNamespace,
                       UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                       UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testEJBUnknownVersion() throws Exception {
        parseEJBJarMax(ejbJarUnknownVersion,
                       UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                       UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
