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
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteInterfaceRemoveTest (formerly WSTestSFR_BRTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>B____ - Business Interface / EJBObject;
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
@WebServlet("/SFRemoteInterfaceRemoveServlet")
public class SFRemoteInterfaceRemoveServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteInterfaceRemoveServlet.class.getName();
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
     * (brm01) Test Stateful remote interface EJBObject.remove.
     */
    @Test
    public void test1XSFEJBObjectRemove() throws Exception {
        SFRa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
    }

    /**
     * (brm02) Test Stateful remote interface EJBObject.remove on non-existing object.
     */
    @Test
    @ExpectedFFDC({ "org.omg.CORBA.OBJECT_NOT_EXIST" })
    public void test1XSFEJBObjectRemoveNonExisting() throws Exception {
        SFRa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
        svLogger.info("instance removal succeeded.");
        try {
            svLogger.info("remove the same instance again.");
            ejb1.remove();
            fail("Unexpected return from remove().");
        } catch (NoSuchObjectException nsoe) {
            // See ejb 2.0 spec pg 63 figure 3
            svLogger.info("Caught expected " + nsoe.getClass().getName());
        }
    }

    /**
     * (brm02) Test Stateful remote interface EJBObject.remove on enlisted object.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.BeanNotReentrantException", "org.omg.CORBA.portable.UnknownException" })
    public void test1XSFEJBObjectRemoveEnlisted() throws Exception {
        // d171551 Begins
        SFRa ejb2 = fhome2.create();
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
