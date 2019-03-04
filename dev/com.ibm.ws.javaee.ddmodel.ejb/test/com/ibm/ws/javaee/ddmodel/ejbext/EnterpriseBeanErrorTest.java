/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.ejbext.Session;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class EnterpriseBeanErrorTest extends EJBJarExtTestBase {

    static final String ejbJarExt20() {
        return "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
               " version=\"2.0\"" +
               ">";
    }

    List<Session> sessionBeans;

    @Test
    public void testGetVersionError() throws Exception {
        try {
            //1.0 & 1.1 are the only valid versions
            Assert.assertEquals("Version should be 2.0", "2.0", parse(ejbJarExt20() + "</ejb-jar-ext>").getVersion());
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(), e.getMessage().contains("CWWKC2263"));
        }
    }

    @Test
    public void testEnterpriseBeanNoName() throws Exception {
        try {
            //name is required in EnterpriseBean
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session>" + //no name
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2251"));
        }
    }

    @Test
    public void testEnterpriseBeanDuplicateName() throws Exception {
        try {
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name=\"duplicate\">" +
                         "</session>" +
                         "<session name=\"duplicate\">" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2269"));
        }
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoMethods() throws Exception {
        try {
            //run-as-mode must have 1 or more methods.
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name=\"session0\">" +
                         "<run-as-mode mode='CALLER_IDENTITY' description='description0'>" +
                         "</run-as-mode>" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2267"));
        }
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoSpecifiedIdentityRole() throws Exception {
        try {
            //role is required in SpecifiedIdentity
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name=\"session0\">" +
                         "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                         "<specified-identity>" + //no role
                         "</specified-identity>" +
                         "<method name='method0'/>" +
                         "<method name='method1'/>" +
                         "</run-as-mode>" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2251"));
        }
    }

    @Test
    public void testEnterpriseBeanSpecifiedIdentityError() throws Exception {
        try {
            //specified-identity element is required when mode is SPECIFIED_IDENTITY
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name=\"session0\">" +
                         "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                         //getting exception because no specified-identity element here
                         "</run-as-mode>" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2268"));
        }
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoNode() throws Exception {
        try {
            //mode is required in <run-as-mode>
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name=\"session0\">" +
                         "<run-as-mode>" +
                         "<method name='method0'/>" +
                         "<method name='method1'/>" +
                         "</run-as-mode>" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2251"));
        }
    }

    @Test
    public void testEnterpriseStartAtAppStartNoValue() throws Exception {
        try {
            //StartAtAppStart requires value
            getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                         "<session name='session0'>" +
                         "<start-at-app-start/>" +
                         "</session>" +
                         "</ejb-jar-ext>");
            Assert.fail("An exception should have been thrown but was not.");
        } catch (DDParser.ParseException e) {
            Assert.assertTrue("Should get specific msg. Got: " + e.getMessage(),
                              e.getMessage().contains("CWWKC2251"));
        }
    }
}
