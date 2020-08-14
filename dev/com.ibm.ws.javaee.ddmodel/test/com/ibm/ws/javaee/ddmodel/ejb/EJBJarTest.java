/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class EJBJarTest extends EJBJarTestBase {

    @Test
    public void testGetVersionID() throws Exception {
        Assert.assertEquals(EJBJar.VERSION_1_1, parse(ejbJar11() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_2_0, parse(ejbJar20() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_2_1, parse(ejbJar21() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_3_0, parse(ejbJar30() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_3_1, parse(ejbJar31() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_3_2, parse(ejbJar32() + "</ejb-jar>").getVersionID());
        Assert.assertEquals(EJBJar.VERSION_4_0, parse(ejbJar40() + "</ejb-jar>").getVersionID());
    }

    // Tests that we get ParseException if ejb-jar.xml version is above feature level version
    @Test(expected = DDParser.ParseException.class)
    public void testMaxVersion31() throws Exception {
        parse(ejbJar32() + "</ejb-jar>", EJBJar.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testMaxVersion32() throws Exception {
        parse(ejbJar40() + "</ejb-jar>", EJBJar.VERSION_3_2);
    }

    @Test
    public void testGetModuleName() throws Exception {
        Assert.assertNull(parse(ejbJar31("") + "</ejb-jar>").getModuleName());
        Assert.assertEquals("test", parse(ejbJar31() + "<module-name>test</module-name></ejb-jar>").getModuleName());
    }

    @Test
    public void testIsMetadataComplete() throws Exception {
        Assert.assertFalse(parse(ejbJar11() + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar20() + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar21() + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar30() + "</ejb-jar>").isMetadataComplete());
        Assert.assertTrue(parse(ejbJar30("metadata-complete='true'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertTrue(parse(ejbJar30("metadata-complete=\"true\"") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar30("metadata-complete='false'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar30("metadata-complete=\"false\"") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar30() + "</ejb-jar>").isMetadataComplete());

        Assert.assertTrue(parse(ejbJar31("metadata-complete='true'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar31("metadata-complete='false'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar31() + "</ejb-jar>").isMetadataComplete());

        Assert.assertTrue(parse(ejbJar32("metadata-complete='true'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar32("metadata-complete='false'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar32() + "</ejb-jar>").isMetadataComplete());

        Assert.assertTrue(parse(ejbJar40("metadata-complete='true'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar40("metadata-complete='false'") + "</ejb-jar>").isMetadataComplete());
        Assert.assertFalse(parse(ejbJar40() + "</ejb-jar>").isMetadataComplete());
    }

    @Test
    public void testDescriptionGroup() throws Exception {
        DescriptionGroup dg = parse(ejbJar30() + "</ejb-jar>");
        Assert.assertEquals(Collections.emptyList(), dg.getDescriptions());
        Assert.assertEquals(Collections.emptyList(), dg.getDisplayNames());
        Assert.assertEquals(Collections.emptyList(), dg.getIcons());

        dg = parse(ejbJar30() +
                   "<description>d0</description>" +
                   "<description xml:lang=\"en\">d1</description>" +
                   "<display-name>dn0</display-name>" +
                   "<display-name xml:lang=\"en\">dn1</display-name>" +
                   "<icon/>" +
                   "<icon xml:lang=\"en\">" +
                   "  <small-icon>si</small-icon>" +
                   "  <large-icon>li</large-icon>" +
                   "</icon>" +
                   "</ejb-jar>");
        List<Description> ds = dg.getDescriptions();
        Assert.assertEquals(ds.toString(), 2, ds.size());
        Assert.assertNull(ds.get(0).getLang());
        Assert.assertEquals("d0", ds.get(0).getValue());
        Assert.assertEquals("en", ds.get(1).getLang());
        Assert.assertEquals("d1", ds.get(1).getValue());

        List<DisplayName> dns = dg.getDisplayNames();
        Assert.assertEquals(dns.toString(), 2, dns.size());
        Assert.assertNull(dns.get(0).getLang());
        Assert.assertEquals("dn0", dns.get(0).getValue());
        Assert.assertEquals("en", dns.get(1).getLang());
        Assert.assertEquals("dn1", dns.get(1).getValue());

        List<Icon> icons = dg.getIcons();
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
        DescriptionGroup dg = parse(ejbJar20() +
                                    "<small-icon>si</small-icon>" +
                                    "<large-icon>li</large-icon>" +
                                    "</ejb-jar>");
        List<Icon> icons = dg.getIcons();
        Assert.assertEquals(icons.toString(), 1, icons.size());
        Assert.assertNull(icons.get(0).getLang());
        Assert.assertEquals("si", icons.get(0).getSmallIcon());
        Assert.assertEquals("li", icons.get(0).getLargeIcon());
    }

    @Test
    public void testGetEnterpriseBeans() throws Exception {
        Assert.assertEquals(0, parse(ejbJar11() + "</ejb-jar>").getEnterpriseBeans().size());
    }

    //see InterceptorTest
    @Test
    public void testGetInterceptors() throws Exception {
        Assert.assertNull(parse(ejbJar30() + "</ejb-jar>").getInterceptors());
        Assert.assertNotNull(parse(ejbJar30() + "<interceptors/></ejb-jar>").getInterceptors());
    }

    //see AssemblyDescriptorTest
    @Test
    public void testGetAssemblyDescriptor() throws Exception {
        Assert.assertNull(parse(ejbJar11() + "</ejb-jar>").getAssemblyDescriptor());
        Assert.assertNotNull(parse(ejbJar11() + "<assembly-descriptor/></ejb-jar>").getAssemblyDescriptor());
    }

    //see RelationshipsTest
    @Test
    public void testRelationships() throws Exception {
        Assert.assertNull(parse(ejbJar21() + "</ejb-jar>").getRelationshipList());
        Assert.assertNotNull(parse(ejbJar21() + "<relationships></relationships></ejb-jar>").getRelationshipList());
    }

    @Test
    public void testGetEjbClientJar() throws Exception {
        Assert.assertNull(parse(ejbJar11() + "</ejb-jar>").getEjbClientJar());
        Assert.assertEquals("client.jar", parse(ejbJar11() + "<ejb-client-jar>client.jar</ejb-client-jar></ejb-jar>").getEjbClientJar());
    }

    @Test
    public void testDisplayNames() throws Exception {
        List<DisplayName> displayNameList = parse(ejbJar11() +
                                                  "<display-name>DisplayName0</display-name>" +
                                                  "<display-name>DisplayName1</display-name>" +
                                                  "<display-name xml:lang=\"en\">" +
                                                  "DisplayName2" +
                                                  "</display-name>" +
                                                  "<enterprise-beans>" +
                                                  "<session>" +
                                                  "<ejb-name>TestSession</ejb-name>" +
                                                  "</session>" +
                                                  "</enterprise-beans>" +
                                                  "</ejb-jar>").getDisplayNames();
        Assert.assertEquals(3, displayNameList.size());
        Assert.assertEquals("DisplayName0", displayNameList.get(0).getValue());
        Assert.assertEquals(null, displayNameList.get(0).getLang());
        Assert.assertEquals("DisplayName1", displayNameList.get(1).getValue());
        Assert.assertEquals("DisplayName2", displayNameList.get(2).getValue());
        Assert.assertEquals("en", displayNameList.get(2).getLang());
    }

    @Test
    public void testIcons() throws Exception {
        List<Icon> iconList = parse(ejbJar11() +
                                    "<icon>" +
                                    "<small-icon>MySmallIcon0</small-icon>" +
                                    "<large-icon>" +
                                    "MyLargeIcon0" +
                                    "</large-icon>" +
                                    "</icon>" +

                                    "<icon>" +
                                    "<small-icon>" +
                                    "MySmallIcon1" +
                                    "</small-icon>" +
                                    "</icon>" +

                                    "<icon xml:lang=\"fr\">" +
                                    "<large-icon> LargeIconWithLang </large-icon>" +
                                    "</icon>" +

                                    "<enterprise-beans>" +
                                    "<session>" +
                                    "<ejb-name>TestSession</ejb-name>" +
                                    "</session>" +
                                    "</enterprise-beans>" +
                                    "</ejb-jar>").getIcons();
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

    //TODO test exceptions
    @Test
    public void testError_001() throws Exception {
        try {
            List<EnterpriseBean> beans = parse(
                                               "<!DOCTYPE ejb-jar PUBLIC \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN\" \"http://java.sun.com/j2ee/dtds/ejb-jar_1_1.dtd\">"
                                               +
                                               "<ejb-NOT-jar>" + //invalid root name
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<local>com.ibm.example.Test1</local>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-NOT-jar>").getEnterpriseBeans();
        } catch (Exception pe) {
            String msg = pe.getMessage();
            Assert.assertTrue("Not expected exception. Got: " + pe.getMessage(),
                              msg.contains("CWWKC2252") &&
                                                                                 msg.contains("ejb-NOT-jar") &&
                                                                                 msg.contains("ejb-jar.xml"));
        }
    }
}
