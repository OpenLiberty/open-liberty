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
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalInterfaceRemoveTest (formerly WSTestSFL_BRTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>B____ - Business Interface / EJBObject / EJBLocalObject;
 * <li>BRM__ - EJB remove.
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
 * <li>brm01 - testEJBObjectRemove - remove()
 * <li>brm02 - testEJBObjectRemoveNonExisting - remove() non-exist-object
 * <li>brm03 - testEJBObjectRemoveEnlisted - remove() when instance is in transaction
 * </ul>
 * <br>Data Sources
 * </dl>
 */

@SuppressWarnings("serial")
@WebServlet("/SFLocalInterfaceRemoveServlet")
public class SFLocalInterfaceRemoveServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalInterfaceRemoveServlet.class.getName();
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
     * (brm01) Test Stateful local interface EJBObject.remove.
     */
    @Test
    public void testSFLocalEJBObjectRemove() throws Exception {
        SFLa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
    }

    /**
     * (brm02) Test Stateful local interface EJBObject.remove on non-existing object.
     */
    @Test
    @ExpectedFFDC("javax.ejb.NoSuchObjectLocalException")
    public void testSFLocalEJBObjectRemoveNonExisting() throws Exception {
        SFLa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
        assertNotNull("Instance removal did not succeed.", ejb1);
        try {
            svLogger.info("Remove the same instance again.");
            ejb1.remove();
            fail("Unexpected return from 2nd remove().");
        } catch (NoSuchObjectLocalException nsoe) {
            // See ejb 2.0 spec pg 63 figure 3
            svLogger.info("Caught expected " + nsoe.getClass().getName());
        }
    }

    /**
     * (brm02) Test Stateful local interface EJBObject.remove on enlisted object.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.BeanNotReentrantException", "javax.ejb.EJBException" })
    public void testSFLocalEJBObjectRemoveEnlisted() throws Exception {
        // d171551 Begins
        SFLa ejb2 = fhome2.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb2);

        try {
            ejb2.beanRemoveInTransaction();
            fail("Unexpected return from remove().");
        } catch (RemoveException re) {
            // See ejb 2.0 spec 7.6 pg 79
            svLogger.info("Caught expected " + re.getClass().getName());
        }
        // d171551 Ends
    }
}
