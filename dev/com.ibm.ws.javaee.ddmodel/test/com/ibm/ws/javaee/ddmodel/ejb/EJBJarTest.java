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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.ejb.EJBJar;

public class EJBJarTest extends EJBJarTestBase {
    protected static final String invalidRootElement =
        "<!DOCTYPE ejb-jar PUBLIC" +
            " \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN\" \"http://java.sun.com/j2ee/dtds/ejb-jar_1_1.dtd\">" +
        "<ejb-NOT-jar>" +
            "<enterprise-beans>" +
                "<session>" +
                    "<ejb-name>TestSession1</ejb-name>" +
                    "<local>com.ibm.example.Test1</local>" +
                "</session>" +
            "</enterprise-beans>" +
        "</ejb-NOT-jar>";

    protected static final String iconsXML =
        "<icon>" +
            "<small-icon>MySmallIcon0</small-icon>" +
            "<large-icon>MyLargeIcon0</large-icon>" +
        "</icon>" +
            
        "<icon>" +
            "<small-icon>MySmallIcon1</small-icon>" +
        "</icon>" +

        "<icon xml:lang=\"fr\">" +
            "<large-icon> LargeIconWithLang </large-icon>" +
        "</icon>" +

        "<enterprise-beans>" +
            "<session>" +
                "<ejb-name>TestSession</ejb-name>" +
            "</session>" +
        "</enterprise-beans>";

    protected static final String displayNamesXML =
        "<display-name>DisplayName0</display-name>" +
        "<display-name>DisplayName1</display-name>" +
        "<display-name xml:lang=\"en\">" +
            "DisplayName2" +
        "</display-name>" +
        "<enterprise-beans>" +
            "<session>" +
                "<ejb-name>TestSession</ejb-name>" +
            "</session>" +
        "</enterprise-beans>";

    protected static final String descriptionGroupXML =
        "<description>d0</description>" +
        "<description xml:lang=\"en\">d1</description>" +
        "<display-name>dn0</display-name>" +
        "<display-name xml:lang=\"en\">dn1</display-name>" +
        "<icon/>" +
        "<icon xml:lang=\"en\">" +
            "<small-icon>si</small-icon>" +
            "<large-icon>li</large-icon>" +
        "</icon>";

    //
    
    @Test
    public void testGetVersionID() throws Exception {
        String[] unsupportedSchemaMessages =
            { "CWWKC2262E", "unprovisioned.descriptor.version" };        
        
        for ( int schemaVersion : EJBJar.VERSIONS ) {
            for ( int maxSchemaVersion : EJBJar.VERSIONS ) {
                // The EJB parser uses a maximum schema
                // version of "max(version, EJBJAR.VERSION_3_1)".
                // Adjust the message expectations accordingly.
                //
                // See:
                // com.ibm.ws.javaee.ddmodel.ejb.EJBJarDDParser

                int effectiveMax;
                if ( maxSchemaVersion < EJBJar.VERSION_3_1 ) {
                    effectiveMax = EJBJar.VERSION_3_1;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String[] expectedMessages;
                if ( schemaVersion > effectiveMax ) {
                    expectedMessages = unsupportedSchemaMessages;
                } else {
                    expectedMessages = null;
                }

                // System.out.println("Schema [ " + schemaVersion + " ]");
                // System.out.println("Maximum [ " + effectiveMax + " ] [ " + maxSchemaVersion + " ]");
                // if ( expectedMessages != null ) {
                //     System.out.println("Messages [ " + Arrays.toString(expectedMessages) + " ]");
                // }

                EJBJar ejbJar = parse(
                    ejbJar(schemaVersion, "", ""),
                    maxSchemaVersion, expectedMessages );

                if ( schemaVersion <= effectiveMax ) {
                    Assert.assertEquals( schemaVersion, ejbJar.getVersionID() );
                }
            }
        }
    }

    @Test
    public void testGetModuleName() throws Exception {
        Assert.assertNull(
            parse( ejbJar31("", ""), EJBJar.VERSION_4_0)
                .getModuleName() );
        Assert.assertEquals("test",
            parse(ejbJar31("", "<module-name>test</module-name>"), EJBJar.VERSION_4_0)
                .getModuleName());
    }

    /**
     * For all schema versions, metadata-complete defaults
     * to "false" when the attribute is not specified.
     * 
     * This is not consistent with the pre-annotations enabled
     * semantics.  In pre-annotations schema versions, the
     * effect metadata-complete value is "true".
     */
    @Test
    public void testIsMetadataComplete() throws Exception {
        for ( int version : EJBJar.VERSIONS ) {
            Assert.assertFalse(
                parse( ejbJar(version, "", ""), EJBJar.VERSION_4_0)
                    .isMetadataComplete() );
        }

        for ( int version : EJBJar.ANNOTATION_ENABLED_VERSIONS ) {
            Assert.assertTrue(
                parse( ejbJar(version, "metadata-complete=\"true\"", ""), EJBJar.VERSION_4_0)
                    .isMetadataComplete() );
            Assert.assertFalse(
                parse( ejbJar(version, "metadata-complete=\"false\"", ""), EJBJar.VERSION_4_0)
                    .isMetadataComplete() );

            // XML allows attributes to use both single quotes and double quotes.
            //
            // TFB: Not sure why this case is important to test.  Low level
            //      parsing is not the target of these unit tests.

            Assert.assertTrue(
                    parse( ejbJar(version, "metadata-complete='true'", ""), EJBJar.VERSION_4_0)
                        .isMetadataComplete() );
                Assert.assertFalse(
                    parse( ejbJar(version, "metadata-complete='false'", ""), EJBJar.VERSION_4_0)
                        .isMetadataComplete() );            
        }
    }

    @Test
    public void testDescriptionGroup() throws Exception {
        DescriptionGroup dg0 = parse( ejbJar30("", ""), EJBJar.VERSION_4_0 );

        Assert.assertEquals(Collections.emptyList(), dg0.getDescriptions());
        Assert.assertEquals(Collections.emptyList(), dg0.getDisplayNames());
        Assert.assertEquals(Collections.emptyList(), dg0.getIcons());

        DescriptionGroup dg1 = parse( ejbJar30("", descriptionGroupXML), EJBJar.VERSION_4_0 );

        List<Description> ds = dg1.getDescriptions();
        Assert.assertEquals(ds.toString(), 2, ds.size());
        Assert.assertNull(ds.get(0).getLang());
        Assert.assertEquals("d0", ds.get(0).getValue());
        Assert.assertEquals("en", ds.get(1).getLang());
        Assert.assertEquals("d1", ds.get(1).getValue());

        List<DisplayName> dns = dg1.getDisplayNames();
        Assert.assertEquals(dns.toString(), 2, dns.size());
        Assert.assertNull(dns.get(0).getLang());
        Assert.assertEquals("dn0", dns.get(0).getValue());
        Assert.assertEquals("en", dns.get(1).getLang());
        Assert.assertEquals("dn1", dns.get(1).getValue());

        List<Icon> icons = dg1.getIcons();
        Assert.assertEquals(icons.toString(), 2, icons.size());
        Assert.assertNull(icons.get(0).getLang());
        Assert.assertNull(icons.get(0).getSmallIcon());
        Assert.assertNull(icons.get(0).getLargeIcon());
        Assert.assertEquals("en", icons.get(1).getLang());
        Assert.assertEquals("si", icons.get(1).getSmallIcon());
        Assert.assertEquals("li", icons.get(1).getLargeIcon());
    }

    @Test
    public void testIconEE13() throws Exception {
        DescriptionGroup dg = parse(
            ejbJar20( "<small-icon>si</small-icon>" +
                      "<large-icon>li</large-icon>" ),
            EJBJar.VERSION_4_0);

        List<Icon> icons = dg.getIcons();
        Assert.assertEquals(icons.toString(), 1, icons.size());
        Assert.assertNull(icons.get(0).getLang());
        Assert.assertEquals("si", icons.get(0).getSmallIcon());
        Assert.assertEquals("li", icons.get(0).getLargeIcon());
    }

    @Test
    public void testGetEnterpriseBeans() throws Exception {
        Assert.assertEquals(0,
            parse( ejbJar11(""), EJBJar.VERSION_4_0)
                .getEnterpriseBeans().size() );
    }

    @Test
    public void testGetInterceptors() throws Exception {
        Assert.assertNull(
            parse(ejbJar30("", ""), EJBJar.VERSION_4_0)
                .getInterceptors());
        Assert.assertNotNull(
            parse(ejbJar30("", "<interceptors/>"), EJBJar.VERSION_4_0)
                .getInterceptors() );
    }

    @Test
    public void testGetAssemblyDescriptor() throws Exception {
        Assert.assertNull(
            parse(ejbJar11(""), EJBJar.VERSION_4_0)
                .getAssemblyDescriptor() );

        Assert.assertNotNull(
            parse(ejbJar11("<assembly-descriptor/>"), EJBJar.VERSION_4_0)
                .getAssemblyDescriptor() );
    }

    @Test
    public void testRelationships() throws Exception {
        Assert.assertNull(
            parse(ejbJar21(""), EJBJar.VERSION_4_0)
                .getRelationshipList() );
        Assert.assertNotNull(
            parse( ejbJar21("<relationships></relationships>"), EJBJar.VERSION_4_0)
                .getRelationshipList() );
    }

    @Test
    public void testGetEjbClientJar() throws Exception {
        Assert.assertNull(
            parse(ejbJar11(""), EJBJar.VERSION_4_0)
                .getEjbClientJar() );

        Assert.assertEquals("client.jar",
            parse(ejbJar11("<ejb-client-jar>client.jar</ejb-client-jar>"), EJBJar.VERSION_4_0)
                .getEjbClientJar());
    }

    @Test
    public void testDisplayNames() throws Exception {
        List<DisplayName> displayNameList =
            parse(ejbJar11( displayNamesXML), EJBJar.VERSION_4_0).getDisplayNames();

        Assert.assertEquals(3, displayNameList.size());
        Assert.assertEquals("DisplayName0", displayNameList.get(0).getValue());
        Assert.assertEquals(null, displayNameList.get(0).getLang());
        Assert.assertEquals("DisplayName1", displayNameList.get(1).getValue());
        Assert.assertEquals("DisplayName2", displayNameList.get(2).getValue());
        Assert.assertEquals("en", displayNameList.get(2).getLang());
    }

    @Test
    public void testIcons() throws Exception {
        List<Icon> iconList =
            parse( ejbJar11(iconsXML), EJBJar.VERSION_4_0 ).getIcons();

        Assert.assertEquals(3, iconList.size());
        Assert.assertEquals("MySmallIcon0", iconList.get(0).getSmallIcon());
        Assert.assertEquals("MyLargeIcon0", iconList.get(0).getLargeIcon());
        Assert.assertEquals("MySmallIcon1", iconList.get(1).getSmallIcon());
        Assert.assertEquals(null, iconList.get(1).getLargeIcon());
        Assert.assertEquals(null, iconList.get(1).getLang());

        Assert.assertEquals(null, iconList.get(2).getSmallIcon());
        Assert.assertEquals("LargeIconWithLang", iconList.get(2).getLargeIcon());
        Assert.assertEquals("fr", iconList.get(2).getLang());
    }

    @Test
    public void testInvalidRootElement() throws Exception {
        try {
            parse(invalidRootElement, EJBJar.VERSION_4_0);
            Assert.fail("Expected parse exception did not occur");
        } catch ( Exception e ) {
            String msg = e.getMessage();
            Assert.assertTrue("Unexpected exception [ " + e.getMessage() + " ]",
                    msg.contains("invalid.root.element") ||
                    (msg.contains("CWWKC2252") &&
                     msg.contains("ejb-NOT-jar") &&
                     msg.contains("ejb-jar.xml")));
        }
    }
}
