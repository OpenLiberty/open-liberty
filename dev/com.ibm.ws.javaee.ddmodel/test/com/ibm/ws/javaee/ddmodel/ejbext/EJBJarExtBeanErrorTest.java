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
package com.ibm.ws.javaee.ddmodel.ejbext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EJBJarExtBeanErrorTest extends EJBJarExtTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }

    public EJBJarExtBeanErrorTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    //

    protected static final String ejbJarExtBadVersion() {
        return "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "\n" +
               "<ejb-jar-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
                   " version=\"9.9\"" +
               "/>";
    }

    @Test
    public void testGetVersionError() throws Exception {
        parseEJBJarExtXML(ejbJarExtBadVersion(), "unsupported.descriptor.version", "CWWKC2261");
    }

    @Test
    public void testEnterpriseBeanNoName() throws Exception {
        parseEJBJarExtXML(ejbJarExt11("<session/>"), // No name
                         "required.attribute.missing", "CWWKC2251");
    }

    @Test
    public void testEnterpriseBeanDuplicateName() throws Exception {
        parseEJBJarExtXML(ejbJarExt11(
                             "<session name=\"duplicate\"/>" +
                             "<session name=\"duplicate\"/>"),
                         "found.duplicate.ejbname", "CWWKC2269");
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoMethods() throws Exception {
        //run-as-mode must have 1 or more methods.
        parseEJBJarExtXML(ejbJarExt11(
                             "<session name=\"session0\">" +
                                 "<run-as-mode mode='CALLER_IDENTITY' description='description0'>" +
                                 "</run-as-mode>" +
                             "</session>"),
                         "required.method.element.missing", "CWWKC2267");                          
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoSpecifiedIdentityRole() throws Exception {
        //role is required in SpecifiedIdentity
        parseEJBJarExtXML(ejbJarExt11(
                              "<session name=\"session0\">" +
                                  "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                                      "<specified-identity>" + //no role
                                      "</specified-identity>" +
                                      "<method name='method0'/>" +
                                      "<method name='method1'/>" +
                                  "</run-as-mode>" +
                              "</session>"),
                         "required.attribute.missing", "CWWKC2251");                         
    }

    @Test
    public void testEnterpriseBeanSpecifiedIdentityError() throws Exception {
        //specified-identity element is required when mode is SPECIFIED_IDENTITY
        parseEJBJarExtXML(ejbJarExt11(
                              "<session name=\"session0\">" +
                                  "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                                  //getting exception because no specified-identity element here
                                  "</run-as-mode>" +
                              "</session>"),
                         "runasmode.missing.specifiedID.element", "CWWKC2268");
    }

    @Test
    public void testEnterpriseBeanRunAsModeNoNode() throws Exception {
        //mode is required in <run-as-mode>
        parseEJBJarExtXML(ejbJarExt11(
                              "<session name=\"session0\">" +
                                  "<run-as-mode>" +
                                      "<method name='method0'/>" +
                                      "<method name='method1'/>" +
                                  "</run-as-mode>" +
                              "</session>"),
                          "required.attribute.missing", "CWWKC2251");
    }

    @Test
    public void testEnterpriseStartAtAppStartNoValue() throws Exception {
        //StartAtAppStart requires value
        parseEJBJarExtXML(ejbJarExt11(
                              "<session name='session0'>" +
                                  "<start-at-app-start/>" +
                              "</session>"),
                         "required.attribute.missing", "CWWKC2251");
    }
}
