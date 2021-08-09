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

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalHomeCreateTest (formerly WSTestSFL_HCTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>H____ - Home Interface / EJBHome / EJBLocalHome;
 * <li>HCS__ - Home create( short-arg );
 * <li>HCL__ - Home create( long-arg ).
 * </ul>
 *
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
 * <li>hcs01 - testHomeCreate - create()
 * <li>hcs02 - testHomeCreateWithKey - create( pk )
 * <li>hcs03 - testHomeCreateWithDuplicateKey - create( pk ) - Duplicate key
 * <li>hcl01 - testHomeCreateWithMultiArgs - create( long-args )
 * <li>hcl02 - testHomeCreateWithDuplicateMultiArgs - create( long-args ) - Duplicate key
 * </ul>
 * <br>Data Sources
 * </dl>
 */

@SuppressWarnings("serial")
@WebServlet("/SFLocalHomeCreateServlet")
public class SFLocalHomeCreateServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalHomeCreateServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTHome";
    private SFLaHome fhome1;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            //fhome1 = (SFLaHome) new InitialContext().lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (hcs01) Test Stateful local home create (no parameters)
     */
    @Test
    public void testSFLocalHomeCreate() throws Exception {
        SFLa ejb1 = fhome1.create();
        assertNotNull("Create EJB was null.", ejb1);
        ejb1.remove();
    }

    /**
     * (hcs02) Test Stateful local home create (with primary key)
     */
    //@Test
    public void testSFLocalHomeCreateWithKey() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hcs03) Test Stateful local home create (with duplicate primary key)
     */
    //@Test
    public void testSFLocalHomeCreateWithDuplicateKey() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (hcl01) Test Stateful local home create (with multiple args)
     */
    @Test
    public void testSFLocalHomeCreateWithMultiArgs() throws Exception {
        SFLa ejb1 = null;
        try {
            ejb1 = fhome1.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
            assertNotNull("Create EJB returned null.", ejb1);
        } finally {
            if (ejb1 != null) {
                ejb1.remove();
                svLogger.info("Cleanup completed, EJB removed");
            }
        }
    }

    /**
     * (hcl02) Test Stateful local home create (with duplicate multiple args)
     */
    //@Test
    public void testSFLocalHomeCreateWithDuplicateMultiArgs() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }
}
