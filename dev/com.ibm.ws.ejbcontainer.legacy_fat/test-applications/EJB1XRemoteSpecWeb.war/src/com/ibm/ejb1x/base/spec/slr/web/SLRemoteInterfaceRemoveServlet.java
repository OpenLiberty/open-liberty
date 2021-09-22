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

package com.ibm.ejb1x.base.spec.slr.web;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.slr.ejb.SLRa;
import com.ibm.ejb1x.base.spec.slr.ejb.SLRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLRemoteInterfaceRemoveTest (formerly WSTestSLR_BRTest)
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
@WebServlet("/SLRemoteInterfaceRemoveServlet")
public class SLRemoteInterfaceRemoveServlet extends FATServlet {
    private final static String CLASS_NAME = SLRemoteInterfaceRemoveServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/slr/ejb/SLRaBMTHome";
    private static SLRaHome fhome1;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SLRaHome.class);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (brm01) Test Stateless remote interface EJBObject.remove.
     */
    @Test
    public void test1XSLEJBObjectRemove() throws Exception {
        SLRa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
    }

    /**
     * (brm02) Test Stateless remote interface EJBObject.remove on non-existing object.
     */
    @Test
    public void test1XSLEJBObjectRemoveNonExisting() throws Exception {
        SLRa ejb1 = null;
        ejb1 = fhome1.create();
        assertNotNull("Remove test, EJB instance creation was null.", ejb1);
        ejb1.remove();
        assertNotNull("Instance removed should not be null.", ejb1);
        svLogger.info("Remove the same instance again.");
        ejb1.remove();
        //Stateless session remove() returns the instance back to pool, so it appears to be removed.
        assertNotNull("Instance removed (2nd time) should not be null", ejb1);
    }

    /**
     * (brm02) Test Stateless remote interface EJBObject.remove on enlisted object.
     */
    //@Test
    public void test1XSLEJBObjectRemoveEnlisted() throws Exception {
        // Stateless beans are never enlisted in a transaction
        svLogger.info("This test does not apply to Stateless beans.");
    }
}
