/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package beanvalidation.web;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.validation.ValidationException;

import beanvalidation.ejbmodule.ejb.AValidationXMLTestBean;
import componenttest.app.FATServlet;

@WebServlet("/BeanValidationServlet")
@SuppressWarnings("serial")
public class BeanValidationServlet extends FATServlet {

    /**
     * This test method tries to do a simple lookup of this web module's ValidatorFactory.
     * It will be called when packaged with two ejb modules to test that the container
     * successfully tells the provider to ignore validation.xml even when this web module
     * doesn't have WEB-INF/validation.xml.
     *
     * Note - this behavior is only enabled for bval-1.1, and not bval-1.0
     */
    public void testLookupValidatorFactoryInServlet() throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            ctx.lookup("java:comp/ValidatorFactory");
        } catch (NamingException e) {
            throw new Exception("lookup of ValidatorFactory shouldn't throw any kind of exception", e);
        }
    }

    /**
     * This test method tries to do a simple lookup of this web module's ValidatorFactory.
     * If will be called when packaged with two ejb modules to test that the container
     * doesn't tell the provider to ignore validation.xml when the web module doesn't
     * have WEB-INF/validation.xml.
     *
     * Note - this behavior is only valid in bval-1.0
     */
    public void testLookupValidatorFactoryInServletFail() throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            ctx.lookup("java:comp/ValidatorFactory");
            fail("lookup of ValidatorFactory should fail");
        } catch (NamingException e) {
            if (e.getCause() == null || !(e.getCause() instanceof ValidationException)) {
                throw new Exception("expecting the root cause of this NamingException to be a ValidationException", e);
            }
        }
    }

    /**
     * Test to check if the ValidatorFactory is configured with the custom provided
     * message interpolator.
     */
    public void testCheckCustomMessageInterpolator() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean testBean = (AValidationXMLTestBean) ctx.lookup("java:app/EJBModule1EJB/AValidationXMLTestBean");
        testBean.checkCustomMessageInterpolator();
    }

    /**
     * Test that calling Validation.buildDefaultValidatorFactory works if this
     * web module is only packaged with one EJB module with a validation.xml.
     */
    public void testBuildDefaultValidatorFactory() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean testBean = (AValidationXMLTestBean) ctx.lookup("java:app/EJBModule1EJB/AValidationXMLTestBean");
        testBean.checkbuildDefaultValidatorFactory();
    }

    /**
     * Test that calling Validation.buildDefaultValidatorFactory DOESN'T work if this
     * web module is packaged with two EJB modules, each with a validation.xml.
     */
    public void testBuildDefaultValidatorFactoryFail() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean testBean = (AValidationXMLTestBean) ctx.lookup("java:app/EJBModule1EJB/AValidationXMLTestBean");
        testBean.checkbuildDefaultValidatorFactoryFail();
    }

    /**
     * Test that an EJB that can build the ValidatorFactory using the Validation API
     * can use that factory to get a Validator and validate the EJB
     */
    public void testUseBuildDefaultValidatorFactory() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean testBean = (AValidationXMLTestBean) ctx.lookup("java:app/EJBModule1EJB/AValidationXMLTestBean");
        assertTrue("should have been able to use a VF built by the EJB",
                   testBean.checkUsebuildDefaultValidatorFactory());
    }
}
