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

package com.ibm.ws.ejbcontainer.app_exception.xml.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExLocalInterface;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionA;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionB;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionC;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionDefaults;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionDefaultsSub;
import com.ibm.ws.ejbcontainer.app_exception.xml.ejb.ResultObject;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/XMLInheritedRTExServlet")
public class XMLInheritedRTExServlet extends FATServlet {

    /**
     * Test that the ApplicationException XML behaves as expected per EJB 3.1
     * Spec section 14.2.1.
     */

    private final static String CLASSNAME = XMLInheritedRTExServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private RTExLocalInterface lookupLocalBean(String beanName) throws NamingException {
        return (RTExLocalInterface) FATHelper.lookupJavaBinding("java:app/EJB31AppExXmlBean/" + beanName + "!" + RTExLocalInterface.class.getName());
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true. Test that the expected ApplicationException is thrown and
     * the transaction is marked for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testXmlRTException0() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(0);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + RTExceptionA.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), RTExceptionA.class);
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true. Exception B extends Exception A. Verify that the expected
     * ApplicationException B is thrown and the transaction is marked for
     * rollback.
     *
     * @throws Exception
     */
    @Test
    public void testXmlRTException1() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(1);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + RTExceptionB.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), RTExceptionB.class);
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true. Exception C extends Exception B which extends Exception A.
     * Exception C is marked as an ApplicationException with inherited=false and
     * rollback=false. Verify that the expected ApplicationException C is thrown
     * and the transaction is NOT marked for rollback.
     *
     * @throws Exception
     */
    @Test
    public void testXmlRTException2() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(2);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + RTExceptionC.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), RTExceptionC.class);
    }

    /**
     * Exception A is marked as an ApplicationException with inherited=true and
     * rollback=true. Exception C extends Exception B which extends Exception A.
     * Exception C is marked as an ApplicationException with inherited=false and
     * rollback=false. Exception D extends Exception C. Verify that an
     * EJBException (a System Exception) is thrown and NOT an
     * ApplicationException. Also verify that the transaction is marked for
     * rollback.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.ejbcontainer.app_exception.xml.ejb.RTExceptionD", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testXmlRTException3() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(3);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be an instace of "
                      + EJBException.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertTrue("Received expected exception. The returned ResultObject = "
                   + result, result.t instanceof EJBException);
    }

    /**
     * Exception Defaults is marked as an ApplicationException without inherited or
     * rollback (i.e. Defaults). Verify that the expected ApplicationException Defaults
     * is thrown and the transaction is NOT marked for rollback.
     *
     * Defaults: inherited=true; rollback=false
     */
    @Test
    public void testXmlRTException4() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(4);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + RTExceptionDefaults.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), RTExceptionDefaults.class);
    }

    /**
     * Exception Defaults is marked as an ApplicationException without inherited or
     * rollback (i.e. Defaults). Exception DefaultsSub extends Defaults.
     * Verify that the expected ApplicationException DefaultsSub is thrown and the
     * transaction is NOT marked for rollback.
     *
     * Defaults: inherited=true; rollback=false
     */
    @Test
    public void testXmlRTException5() throws Exception {
        RTExLocalInterface bean = lookupLocalBean("RTExceptionBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(5);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + RTExceptionDefaultsSub.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), RTExceptionDefaultsSub.class);
    }
}
