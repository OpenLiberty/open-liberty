/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.tx.statlesmixsec.web;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.tx.statlesmixsec.ejb.AsmDescSecRolesLocal;

import componenttest.app.FATServlet;

/**
 * This test class was created to verify the fix for APAR PK93643. Please refer
 * to the APAR for a more detailed description of the problem. Here is a brief
 * synopsis of what the APAR was opened for:
 *
 * In WebSphere ApplicationServer version 7.0.0.3 test server within Rational
 * Application Developer version 7.5.3 environment, Security Roles that are
 * defined within the Assembly Descriptor section of the ejb-jar.xml Deployment
 * Descriptor file where the corresponding EJB are defined via Java Annotations
 * are not being processed correctly. Transaction attributes defined in the same
 * way are being ignored.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/AsmDescSecRolesServlet")
public class AsmDescSecRolesServlet extends FATServlet {
    private static final String CLASS_NAME = AsmDescSecRolesServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    AsmDescSecRolesLocal bean;

    /**
     * Test to verify that if a bean is only defined via annotations (i.e. no
     * enterprise-beans defined in XML) and a security-role is defined in the
     * assembly-descriptor section of the ejb-jar.xml for a specific method, the
     * security role will be picked up and enforced for that method.
     *
     * @throws Exception
     */
    @Test
    public void testAsmDescSecRole() throws Exception {
        try {
            String result = bean.secRoleMethod();
            fail("--> " + result);
        } catch (EJBAccessException ex) {
            svLogger.info("--> Test caught the expected Security Exception." + ex);
        }
    }

    /**
     * If a bean is only defined via annotations (i.e. no enterprise-beans
     * defined in XML) and a security-role is defined in the assembly-descriptor
     * section of the ejb-jar.xml for a specific method, verify that a different
     * method on the same bean is not associated with the security-role.
     *
     * @throws Exception
     */
    @Test
    public void testAsmDescUnsecureMethod() throws Exception {
        try {
            String result = bean.unSecureMethod();
            svLogger.info("--> " + result);
        } catch (Throwable t) {
            svLogger.info("--> Test caught an unexpected exception." + t);
            fail("--> Test caught an unexpected exception." + t);
        }
    }
}