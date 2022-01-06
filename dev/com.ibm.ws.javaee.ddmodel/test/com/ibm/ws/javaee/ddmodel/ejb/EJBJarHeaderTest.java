/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            "<display-name>EJBTest</display-name>" + "\n" +
            "<enterprise-beans>" + "\n" +
                "<session id=\"TestBean\">" + "\n" +
                    "<ejb-name>TestBean</ejb-name>" + "\n" +
                    "<home>ejbtest.ejb.ITestHome</home>" + "\n" +
                    "<remote>ejbtest.ejb.ITestRemote</remote>" + "\n" +
                    "<local-home>ejbtest.ejb.TestBeanLocalHome</local-home>" + "\n" +
                    "<local>ejbtest.ejb.TestBeanLocal</local>" + "\n" +
                    "<ejb-class>ejbtest.ejb.TestBean</ejb-class>" + "\n" +
                    "<session-type>Stateful</session-type>" + "\n" +
                    "<transaction-type>Bean</transaction-type>" + "\n" +
                "</session>" + "\n" +
            "</enterprise-beans>";
    
    protected static String ejbJar21NoNamespace =
            "<ejb-jar" +
                // " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            ">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar21NoSchemaInstance =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            ">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar21NoSchemaLocation =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            ">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar21NoXSI =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            ">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar21NoVersion =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                // " version=\"2.1\"" +
            ">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    //
    
    protected static String ejbJar21NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/j2ee\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar21VersionOnly =
            "<ejb-jar version=\"2.1\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar30NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJar30VersionOnly =
            "<ejb-jar version=\"3.0\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    //

    protected static String ejbJar30VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"3.0\"" +
            ">" + "\n" +    
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJarUnknownNamespace =
            "<ejb-jar xmlns=\"http://junk\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    protected static String ejbJarUnknownVersion =
            "<ejb-jar version=\"9.9\">" + "\n" +
                ejbJarBody + "\n" +
            "</ejb-jar>";
    
    //
    
    @Test
    public void testEJB21NoNamespace() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21NoNamespace, EJBJar.VERSION_3_0);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoSchemaInstance() throws Exception {
        parseEJBJar(ejbJar21NoSchemaInstance, EJBJar.VERSION_3_0, "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEJB21NoSchemaLocation() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21NoSchemaLocation, EJBJar.VERSION_3_0);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoXSI() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21NoXSI, EJBJar.VERSION_3_0);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21NoVersion() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21NoVersion, EJBJar.VERSION_3_0);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    //
    
    @Test
    public void testEJB21NamespaceOnly() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21NamespaceOnly, EJBJar.VERSION_3_0);
        Assert.assertEquals( 21, ejbJar.getVersionID() );
    }

    @Test
    public void testEJB21VersionOnly() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21VersionOnly, EJBJar.VERSION_3_0);
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
        EJBJar ejbJar = parseEJBJar(ejbJar30VersionOnly, EJBJar.VERSION_3_0);
        Assert.assertEquals( 30, ejbJar.getVersionID() );
    }

    //

    // A warning is issued, but otherwise the mismatch is ignored
    // and the version has precedence.

    @Test
    public void testEJB30VersionMismatch() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar30VersionMismatch, EJBJar.VERSION_3_0);
        Assert.assertEquals( 30, ejbJar.getVersionID() );
    }

    @Test
    public void testEJBUnknownNamespace() throws Exception {
        parseEJBJar(ejbJarUnknownNamespace, EJBJar.VERSION_3_0,
                    UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                    UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testEJBUnknownVersion() throws Exception {
        parseEJBJar(ejbJarUnknownVersion, EJBJar.VERSION_3_0,
                    UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                    UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
