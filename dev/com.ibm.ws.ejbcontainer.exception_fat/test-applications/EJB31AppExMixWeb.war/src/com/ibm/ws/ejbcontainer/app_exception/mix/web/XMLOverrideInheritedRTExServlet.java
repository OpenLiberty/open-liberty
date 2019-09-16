/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.app_exception.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.app_exception.mix.ejb.RTExLocalInterface;
import com.ibm.ws.ejbcontainer.app_exception.mix.ejb.ResultObject;
import com.ibm.ws.ejbcontainer.app_exception.mix.ejb.SubXMLOverridesFalse;
import com.ibm.ws.ejbcontainer.app_exception.mix.ejb.XMLOverridesFalse;
import com.ibm.ws.ejbcontainer.app_exception.mix.ejb.XMLOverridesTrue;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/XMLOverrideInheritedRTExServlet")
public class XMLOverrideInheritedRTExServlet extends FATServlet {

    /**
     * Test that the ApplicationException XML behaves as expected per EJB 3.1
     * Spec section 14.2.1.
     */

    private final static String CLASSNAME = XMLOverrideInheritedRTExServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private RTExLocalInterface lookupLocalBean(String beanName) throws NamingException {
        return (RTExLocalInterface) FATHelper.lookupJavaBinding("java:app/EJB31AppExMixBean/" + beanName + "!" + RTExLocalInterface.class.getName());
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true via annotation. It is also marked as an ApplicationException
     * in XML with both inherited and rollback set to false. The XML should
     * override the annotation. Test that the expected ApplicationException A is
     * thrown and the transaction is NOT marked for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testXMLOverridesTrueException() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(0);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + XMLOverridesTrue.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), XMLOverridesTrue.class);
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true via annotation. Exception A is also marked as an ApplicationException
     * in XML with both inherited and rollback set to false. The XML should
     * override the annotation. Exception B extends Exception A. Verify that an
     * EJBException (a System Exception) is returned when B is thrown and NOT an
     * ApplicationException. Also verify that the transaction is marked for
     * rollback.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.ejbcontainer.app_exception.mix.ejb.SubXMLOverridesTrue", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testXMLOverridesTrueException2() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(1);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be an instace of "
                      + EJBException.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertTrue("Received expected exception. The returned ResultObject = "
                   + result, result.t instanceof EJBException);
    }

    /**
     * Exception C is marked as an ApplicationException with inherited=false and
     * rollback=false via annotation. It is also marked as an ApplicationException
     * in XML with both inherited and rollback set to true. The XML should
     * override the annotation. Test that the expected ApplicationException C is
     * thrown and the transaction is marked for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testXMLOverridesFalseException() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(2);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + XMLOverridesFalse.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), XMLOverridesFalse.class);
    }

    /**
     * Exception C is marked as an ApplicationException with inherited=false and
     * rollback=false via annotation. Exception C is also marked as an ApplicationException
     * in XML with both inherited and rollback set to true. The XML should
     * override the annotation. Exception D extends Exception C. Verify that the expected
     * ApplicationException D is thrown and the transaction is marked for
     * rollback.
     *
     * @throws Exception
     */
    @Test
    public void testXMLOverridesFalseException2() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(3);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + SubXMLOverridesFalse.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), SubXMLOverridesFalse.class);
    }
}
