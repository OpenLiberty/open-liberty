/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sll.web;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLaFakeKey;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalHomeRemoveTest
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>H____ - Home Interface / EJBHome / EJBLocalHome;
 * <li>HRK__ - Home remove( pkey );
 * <li>HRH__ - Home remove( handle ).
 * </ul>
 *
 * <dt>Command options:
 * <dd>
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>None</TD>
 * <TD></TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>hrk01 - testHomeRemoveWithKey - remove( pkey )
 * <li>hrk02 - testHomeRemoveWithNoExistKey - remove( non-exist-object-pkey )
 * <li>hrk03 - testHomeRemoveWithEnlistedKey - remove( pkey ) when instance is in transaction
 * <li>hrh01 - testHomeRemoveWithHandle - remove( handle )
 * <li>hrh02 - testHomeRemoveWithNoExistHandle - remove( non-exist-object-handle )
 * <li>hrh03 - testHomeRemoveWithEnlistedHandle - remove( handle ) when instance is in transaction
 * </ul>
 * <br>Data Sources
 * </dl>
 */

@SuppressWarnings("serial")
@WebServlet("/SLLocalHomeRemoveServlet")
public class SLLocalHomeRemoveServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalHomeRemoveServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaBMTHome";
    private SLLaHome fhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            //fhome1 = (SLLaHome) new InitialContext().lookup("java:app/EJB2XSLLocalSpecEJB/SLLaBMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (hrk01) Test Stateless home remove with primary key
     */
    @Test
    public void testSLLocalHomeRemoveWithKey() throws Exception {
        final String ks1 = "hrk01";
        SLLaFakeKey pk1 = new SLLaFakeKey(ks1);
        try {
            fhome1.remove(pk1);
            fail("Unexpected return from remove(pkey).");
        } catch (RemoveException ise) {
            svLogger.info("Caught an expected " + ise.getClass().getName());
        }
    }

    /**
     * (hrk02) Test Stateless home remove with unknown primary key
     */
    //@Test
    public void testSLLocalHomeRemoveWithNonExistKey() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (hrk03) Test Stateless home remove with enlisted primary key
     */
    //@Test
    public void testSLLocalHomeRemoveWithEnlistedKey() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (hrh01) Test Stateless home remove with handle
     */
    //@Test
    public void testSLLocalHomeRemoveWithHandle() throws Exception {
        svLogger.info("This test does not apply to Local Stateless beans.");
    }

    /**
     * (hrh02) Test Stateless home remove with unknown handle
     */
    //@Test
    public void testSLLocalHomeRemoveWithNonExistHandle() throws Exception {
        svLogger.info("This test does not apply to Local Stateless beans.");
    }

    /**
     * (hrh03) Test Stateless home remove with enlisted handle
     */
    //@Test
    public void testSLLocalHomeRemoveWithEnlistedHandle() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }
}
