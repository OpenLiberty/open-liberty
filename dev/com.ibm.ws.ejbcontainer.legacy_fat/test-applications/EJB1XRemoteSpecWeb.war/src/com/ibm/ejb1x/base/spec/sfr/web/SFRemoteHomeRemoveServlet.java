/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.NoSuchObjectException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaFakeKey;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteHomeRemoveTest
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>H____ - Home Interface / EJBHome;
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
@WebServlet("/SFRemoteHomeRemoveServlet")
public class SFRemoteHomeRemoveServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteHomeRemoveServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaCMTHome";
    private static SFRaHome fhome1;
    private static SFRaHome fhome2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);
            fhome2 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SFRaHome.class);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (hrk01) Test Stateful home remove with primary key
     */
    @Test
    public void test1XSFHomeRemoveWithKey() throws Exception {
        final String ks1 = "hrk01";
        SFRaFakeKey pk1 = new SFRaFakeKey(ks1);
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
    public void test1XSFHomeRemoveWithNonExistKey() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hrk03) Test Stateful home remove with enlisted primary key
     */
    @Test
    public void test1XSFHomeRemoveWithEnlistedKey() throws Exception {
        // d171551 Begins
        SFRa ejb2 = fhome2.create();
        assertNotNull("       remove test, EJB instance creation.", ejb2);

        try {
            ejb2.homeRemovePKeyInTransaction();
            fail("       Unexpected return from remove( ejb ).");
        } catch (RemoveException re) { // See ejb 2.0 spec 6.3.2.pg 59
            svLogger.info("Caught expected " + re.getClass().getName());
        }
        // d171551 Ends
    }

    /**
     * (hrh01) Test Stateful home remove with handle
     */
    @Test
    public void test1XSFHomeRemoveWithHandle() throws Exception {
        SFRa ejb1 = null;
        try {
            ejb1 = fhome1.create();
            assertNotNull("Create EJB was null.", ejb1);

            svLogger.info("       Get handle of EJB.");
            Handle hd = ejb1.getHandle();
            assertNotNull("Handle from object was null.", hd);

            svLogger.info("Now execute home.remove(handle)");
            fhome1.remove(hd);
            svLogger.info("Returned from remove(handle) successfully.");
            ejb1 = null;
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
                svLogger.info("Cleanup completed, EJB removed");
            }
        }
    }

    /**
     * (hrh02) Test Stateful home remove with unknown handle
     */
    @Test
    @ExpectedFFDC({ "org.omg.CORBA.OBJECT_NOT_EXIST", "java.rmi.NoSuchObjectException" })
    public void test1XSFHomeRemoveWithNonExistHandle() throws Exception {
        SFRa ejb1 = null;
        try {
            ejb1 = fhome1.create();
            assertNotNull("Create EJB was null.", ejb1);

            svLogger.info("Get handle of EJB.");
            Handle hd = ejb1.getHandle();
            assertNotNull("Handle from object was null.", hd);

            svLogger.info("Now ejb.remvove() ejb.");
            ejb1.remove();
            svLogger.info("Returned from ejb.remove() successfully.");
            ejb1 = null;

            svLogger.info("Call home.remove(handle) again after it has been removed.");
            try {
                fhome1.remove(hd);
                fail("Unexpected return from remove().");
            } catch (NoSuchObjectException nsoe) {
                svLogger.info("Caught expected " + nsoe.getClass().getName());
            }
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
                svLogger.info("Cleanup completed, EJB removed");
            }
        }
    }

    /**
     * (hrh03) Test Stateful home remove with enlisted handle
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.BeanNotReentrantException", "org.omg.CORBA.portable.UnknownException" })
    public void test1XSFHomeRemoveWithEnlistedHandle() throws Exception {
        // d171551 Begins
        SFRa ejb2 = fhome2.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb2);

        try {
            ejb2.homeRemoveHandleInTransaction();
            fail("Unexpected return from remove( ejb.getHandle() ).");
        } catch (RemoveException re) { // See ejb 2.0 spec 7.6 pg 79
            svLogger.info("Caught expected " + re.getClass().getName());
        }
        // d171551 Ends
    }
}
