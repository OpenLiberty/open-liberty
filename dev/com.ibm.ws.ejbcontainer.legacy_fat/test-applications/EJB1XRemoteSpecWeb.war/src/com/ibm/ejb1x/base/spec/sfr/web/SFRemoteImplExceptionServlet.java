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
import java.rmi.RemoteException;

import javax.annotation.PostConstruct;
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
 * <dd>SFRemoteImplExceptionTest (formerly WSTestSFR_IETest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>I____ - Bean Implementation;
 * <li>IEX__ - Dealing with Exception.
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
 * <li>iex01 - testImplMethodThrowsRuntimeException - Throw RuntimeException -> does not exist -> access
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFRemoteImplExceptionServlet")
public class SFRemoteImplExceptionServlet extends FATServlet {

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private static SFRaHome fhome1;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (iex01) Test Stateful remote method that throws RuntimeException
     */
    @Test
    @ExpectedFFDC({ "org.omg.CORBA.OBJECT_NOT_EXIST", "java.lang.RuntimeException" })
    public void test1XSFImplMethodThrowsRuntimeException() throws Exception {
        SFRa ejb1 = fhome1.create();
        assertNotNull("Create EJB was null.", ejb1);
        try {
            ejb1.throwRuntimeException();
            fail("Exception did not occur as expected");
        } catch (RemoteException rex) {
            // Exception is expected
        }

        try {
            ejb1.remove();
            fail("Exception did not occur on remove as expected");
        } catch (NoSuchObjectException nsoex) {
            // Exception is expected; ejb was discarded above
        }
    }
}
