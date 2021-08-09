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

package com.ibm.ejb2x.base.spec.sfl.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaFakeKey;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalHomeRemoveTest
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
@WebServlet("/SFLocalHomeRemoveServlet")
public class SFLocalHomeRemoveServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalHomeRemoveServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaCMTHome";
    private SFLaHome fhome1;
    private SFLaHome fhome2;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            fhome2 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName2);
            //InitialContext cntx = new InitialContext();
            //fhome1 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMT");
            //fhome2 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaCMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (hrk01) Test Stateful home remove with primary key
     */
    @Test
    public void testSFLocalHomeRemoveWithKey() throws Exception {
        final String ks1 = "hrk01";
        SFLaFakeKey pk1 = new SFLaFakeKey(ks1);
        try {
            fhome1.remove(pk1);
            fail("Unexpected return from remove(pkey).");
        } catch (RemoveException ise) { // See ejb 2.0 spec 6.3.2.pg 59
            svLogger.info("Caught an expected " + ise.getClass().getName());
        }
    }

    /**
     * (hrk02) Test Stateful home remove with unknown primary key
     */
    //@Test
    public void testSFLocalHomeRemoveWithNonExistKey() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hrk03) Test Stateful home remove with enlisted primary key
     */
    @Test
    public void testSFLocalHomeRemoveWithEnlistedKey() throws Exception {
        // d171551 Begins
        SFLa ejb2 = fhome2.create();
        assertNotNull("remove test, EJB instance creation was null.", ejb2);

        try {
            ejb2.homeRemovePKeyInTransaction();
            fail("Unexpected return from remove( ejb ).");
        } catch (RemoveException re) { // See ejb 2.0 spec 6.3.2.pg 59
            svLogger.info("Caught expected " + re.getClass().getName());
        }
        // d171551 Ends
    }

    /**
     * (hrh01) Test Stateful home remove with handle
     */
    //@Test
    public void testSFLocalHomeRemoveWithHandle() throws Exception {
        svLogger.info("This test does not apply to Local Stateful beans.");
    }

    /**
     * (hrh02) Test Stateful home remove with unknown handle
     */
    //@Test
    public void testSFLocalHomeRemoveWithNonExistHandle() throws Exception {
        svLogger.info("This test does not apply to Local Stateful beans.");
    }

    /**
     * (hrh03) Test Stateful home remove with enlisted handle
     */
    //@Test
    public void testSFLocalHomeRemoveWithEnlistedHandle() throws Exception {
        svLogger.info("This test does not apply to Local Stateful beans.");
    }
}
