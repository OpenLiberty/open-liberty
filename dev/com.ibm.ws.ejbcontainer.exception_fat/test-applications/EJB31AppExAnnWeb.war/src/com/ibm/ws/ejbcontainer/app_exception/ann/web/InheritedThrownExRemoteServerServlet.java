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

package com.ibm.ws.ejbcontainer.app_exception.ann.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.ResultObject;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.SubThrownAppExInheritFalse;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.SubThrownAppExInheritTrue;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.SubThrownException;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.ThrownAppExInheritFalse;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.ThrownAppExInheritTrue;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.ThrownExRemoteInterface;
import com.ibm.ws.ejbcontainer.app_exception.ann.ejb.ThrownException;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/InheritedThrownExRemoteServerServlet")
public class InheritedThrownExRemoteServerServlet extends FATServlet {

    protected String testCaseName = this.getClass().getName();

    /**
     * Test that the ApplicationException annotation behaves as expected per EJB
     * 3.1 Spec section 14.2.1.
     */

    private final static String CLASSNAME = InheritedThrownExRemoteServerServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private ThrownExRemoteInterface lookupRemoteBean(String beanName) throws NamingException {
        return (ThrownExRemoteInterface) FATHelper.lookupDefaultBindingsEJBRemoteInterface(ThrownExRemoteInterface.class, "EJB31AppExAnnApp", "EJB31AppExAnnBean",
                                                                                           beanName);
    }

    /**
     * Test that when exception A is on the throws clause is treated as an
     * Application Exception even though there are no annotations or XML defining
     * it as an Application Exception. Verify that by default the transaction is
     * NOT marked for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownException0() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(0);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + ThrownException.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), ThrownException.class);
    }

    /**
     * Test that an exception B, which extends exception A, is treated as an
     * Application Exception - even though there are no annotations or XML
     * defining either B or A as an Application Exception - when A is on the
     * throws clause. Also verify that by default the transaction is not marked
     * for rollback.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownException1() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test(1);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + SubThrownException.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), SubThrownException.class);
    }

    /**
     * Exception A is marked as an Application Exception with inherited = false
     * and rollback = true. Test that when exception A is on the throws clause is
     * treated as an Application Exception. Verify that the transaction is marked
     * for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownAppExceptionInheritFalse0() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test2(0);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + ThrownAppExInheritFalse.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), ThrownAppExInheritFalse.class);
    }

    /**
     * Test that an exception B, which extends exception A, is treated as an
     * Application Exception - even though A is marked with an
     * ApplicationException annotation with inherited = false and rollback=true -
     * when A is on the throws clause. Also verify that the transaction is not
     * marked for rollback.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownAppExceptionInheritFalse1() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test2(1);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + SubThrownAppExInheritFalse.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertFalse("The transaction was NOT marked for rollback. ResultObject = "
                    + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), SubThrownAppExInheritFalse.class);
    }

    /**
     * Exception A is marked as an Application Exception with inherited = true
     * and rollback = true. Test that when exception A is on the throws clause is
     * treated as an Application Exception. Verify that the transaction is marked
     * for rolled back.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownAppExceptionInheritTrue0() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test3(0);

        svLogger.info("--> INFO:  Expected rollback to be true and the exception class to be "
                      + ThrownAppExInheritTrue.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), ThrownAppExInheritTrue.class);
    }

    /**
     * Test that an exception B, which extends exception A, is treated as an
     * Application Exception - where A is marked with an ApplicationException
     * annotation with inherited = true and rollback=true - when A is on the
     * throws clause. Also verify that the transaction is marked for rollback.
     *
     * @throws Exception
     */
    @Test
    public void testAnnRemoteThrownAppExceptionInheritTrue1() throws Exception {
        ThrownExRemoteInterface bean = lookupRemoteBean("ThrownExceptionRemoteBean");
        svLogger.info("--> INFO:  Successful bean lookup.");
        ResultObject result = bean.test3(1);

        svLogger.info("--> INFO:  Expected rollback to be false and the exception class to be "
                      + SubThrownAppExInheritTrue.class);

        svLogger.info("--> INFO:  ResultObject = " + result);

        assertTrue("The transaction was marked for rollback. ResultObject = "
                   + result, result.isRolledBack);

        assertEquals("Received expected exception. The returned ResultObject = "
                     + result, result.t.getClass(), SubThrownAppExInheritTrue.class);
    }
}
