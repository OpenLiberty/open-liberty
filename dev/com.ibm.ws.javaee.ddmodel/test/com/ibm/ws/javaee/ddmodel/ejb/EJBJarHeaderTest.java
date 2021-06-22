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

    protected static String ejbJar21NoNamespace =
            "<ejb-jar" +
                // " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            "/>";
    
    protected static String ejbJar21NoSchemaInstance =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            "/>";
    
    protected static String ejbJar21NoSchemaLocation =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            "/>";

    protected static String ejbJar21NoXSI =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"2.1\"" +
            "/>";
    
    protected static String ejbJar21NoVersion =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                // " version=\"2.1\"" +
            "/>";
    
    //
    
    protected static String ejbJar21NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/j2ee\"/>";
    
    protected static String ejbJar21VersionOnly =
            "<ejb-jar version=\"2.1\"/>";
    
    protected static String ejbJar30NamespaceOnly =
            "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"/>";

    protected static String ejbJar30VersionOnly =
            "<ejb-jar version=\"3.0\"/>";

    //

    protected static String ejbJar30VersionMismatch =
            "<ejb-jar" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                " version=\"3.0\"" +
            "/>";    
    
    protected static String ejbJarUnknownNamespace =
            "<ejb-jar xmlns=\"http://junk\"/>";

    protected static String ejbJarUnknownVersion =
            "<ejb-jar version=\"9.9\"/>";
    
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
        parseEJBJar(ejbJarUnknownNamespace, EJBJar.VERSION_3_0, "unsupported.descriptor.namespace", "unknown");
    }

    @Test
    public void testEJBUnknownVersion() throws Exception {
        parseEJBJar(ejbJarUnknownVersion, EJBJar.VERSION_3_0, "unsupported.descriptor.version", "unknown");
    }
}
