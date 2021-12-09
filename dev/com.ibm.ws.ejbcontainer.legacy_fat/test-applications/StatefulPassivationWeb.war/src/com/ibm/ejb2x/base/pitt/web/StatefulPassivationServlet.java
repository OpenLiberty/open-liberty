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

package com.ibm.ejb2x.base.pitt.web;

import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.pitt.ejb.BMTXSession;
import com.ibm.ejb2x.base.pitt.ejb.BMTXSessionHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>StatefulSessionPassivationTest
 *
 * <dt>Test Descriptions:
 * <dd>This class is designed as a Testable to test passivation of stateful session beans.
 *
 * <dt>Author:
 * <dd>(ported by) Jim Krueger
 *
 * <dt>Command options:
 * <dd>None
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>-option 1</TD>
 * <TD>Option 1 descriptions.</TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>subTest1 - test passivation of stateful session bean.
 ** </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings({ "serial" })
@WebServlet("/StatefulPassivationServlet")
public class StatefulPassivationServlet extends FATServlet {
    private static final String CLASS_NAME = StatefulPassivationServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    protected static BMTXSessionHome beanHome = null;

    @PostConstruct
    private void initHome() {
        try {
            beanHome = FATHelper.lookupRemoteHomeBinding("BMTXSession", BMTXSessionHome.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test passivation of stateful session bean.
     */
    @Test
    public void subTest1() throws Exception {
        Vector<BMTXSession> sfb = new Vector<BMTXSession>();
        int sum = 0;
        @SuppressWarnings("unused")
        int num = 0;
        boolean end = false;
        BMTXSession sb = null;

        try {
            while (!end) {
                for (int i = 0; i < 1000; i++) {
                    sum++;
                    sb = beanHome.create();
                    svLogger.info("Calling beforePassivation on :" + sb.toString());
                    sb.beforePassivation(sum);
                    sfb.addElement(sb);
                    svLogger.info("Created bean instance #" + sum);
                }

                for (int i = 1; i <= sum; i++) {
                    sb = sfb.elementAt(i - 1);
                    svLogger.info("Checking passivation state of bean instance #" + i);
                    if (sb.isPassivated()) {
                        svLogger.info("Bean instance #" + i + " is passivated");
                        num = i;
                        sb.testPassivationActivation();
                        // set flag and exit
                        end = true;
                        break;
                    }

                    svLogger.info("Checked passivation state up to " + sum);
                }
            }

        } finally {
            // clean up environment
            for (int i = 1; i <= sum; i++) {
                try {
                    sb = sfb.elementAt(i - 1);
                    sb.afterPassivation(i);
                    sb.remove();
                } catch (Exception e) {
                    // bean already timed out
                }
            }
        }
    }
}