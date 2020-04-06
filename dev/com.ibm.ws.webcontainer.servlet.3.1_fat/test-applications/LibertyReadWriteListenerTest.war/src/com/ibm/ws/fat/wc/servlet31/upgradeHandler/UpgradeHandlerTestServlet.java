/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.servlet31.upgradeHandler;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Upgrade Handler Servlet, which is called when the client sends a  upgrade request.
 * The servlet in turn calls the handler that executes the different client
 * tests.
 */
@WebServlet(urlPatterns = "/UpgradeHandlerTestServlet")
public class UpgradeHandlerTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(UpgradeHandlerTestServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

        // check if upgrade is requested.
        if ("TestUpgrade".equals(req.getHeader("Upgrade"))) {

            LOG.info("upgraded to use TestHttpUpgradeHandler in doPost of  UpgradeHandlerTestServlet ");
            TestHttpUpgradeHandler handler = req.upgrade(TestHttpUpgradeHandler.class); //call the upgradeHandler class
            String testType = req.getHeader("TestType");
            // depending on test called, appropriate code will be executed in the handler
            if (testType.contains("Read")) {
                LOG.info("\n [RUNNING TEST: In Servlet UpgradeHandlerTestServlet for Read*******" + testType + " ***********************]");
                doWorkForReadTests(req, testType);
            }
            else {
                LOG.info("\n [RUNNING TEST: In Servlet UpgradeHandlerTestServlet for Write*********" + testType + " ************]");
                doWorkForWriteTests(req, handler, testType);
            }

            res.setStatus(101); //set 101 header
            res.setHeader("Upgrade", "TestUpgrade"); //set Upgrade header
            res.setHeader("Connection", "Upgrade");

            LOG.info("******************************UpgradeHandlerTestServlet doPost is done for " + testType + " **********");

        }
        else {
            // execute if no upgrade is requested.The test " testUpgradeNoUpgrade" will call this section of the code.
            LOG.info("\n [RUNNING TEST: testNoUpgrade******************************]");
            LOG.info("no upgrade mechanism in place, printing alphabetNoUpgrade ");
            ServletOutputStream out = res.getOutputStream();
            out.print("NoUpgrade");
            LOG.info("******************************UpgradeHandlerTestServlet doPost is done");

        }

    }

    /**
     * @param req
     * @param handler
     * @param testType
     */
    private void doWorkForWriteTests(HttpServletRequest req, TestHttpUpgradeHandler handler, String testType) {
        if (testType.equals("test_SmallData_UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_SmallData_UpgradeWL");
        }

        else if (testType.equals("test_SingleWriteLargeData1000000__UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_SingleWriteLargeData1000000__UpgradeWL");
            handler.setContentSize(req.getHeader("ContentSizeSent")); //send contentdataSize, received from the client for testWriteListnerUpgradeLargeData
        }

        else if (testType.equals("test_LargeDataInChunks_UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_LargeDataInChunks_UpgradeWL");
            handler.setContentSize(req.getHeader("ContentSizeSent")); //send contentdataSize, received from the client for testWriteListnerUpgradeLargeData
        }

        else if (testType.equals("test_Close_WebConnection_Container_UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_Close_WebConnection_Container_UpgradeWL");
        }

        else if (testType.equals("test_SmallDataInHandler_NoWriteListener")) {
            TestHttpUpgradeHandler.setTestSet("test_SmallDataInHandler_NoWriteListener");
        }

        else if (testType.equals("test_Timeout_UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_Timeout_UpgradeWL");
        }

        else if (testType.equals("test_ContextTransferProperly_UpgradeWL")) {
            TestHttpUpgradeHandler.setTestSet("test_ContextTransferProperly_UpgradeWL");
        }

        else if (testType.equals("TestWrite_DontCheckisRedy_fromUpgradeWL")
                 || testType.equals("TestWriteFromHandler_AftersetWL")
                 || testType.equals("TestUpgrade_ISE_setSecondWriteListener")
                 || testType.equals("TestUpgrade_NPE_setNullWriteListener")) {
            TestHttpUpgradeHandler.setTestSet(testType);
            handler.setContentSize(req.getHeader("ContentSizeSent")); //send contentdataSize,
        }

    }

    /**
     * @param req
     * @param testType
     */
    private void doWorkForReadTests(HttpServletRequest req, String testType) {
        if (req.getHeader("TestType").equals("testUpgradeReadListenerLargeData"))
        {
            TestHttpUpgradeHandler.setTestSet("testUpgradeReadListenerLargeData");
        }
        else if (req.getHeader("TestType").equals("testUpgradeReadListenerSmallData"))
        {
            TestHttpUpgradeHandler.setTestSet("testUpgradeReadListenerSmallData");
        }
        else if (req.getHeader("TestType").equals("testUpgradeReadSmallDataInHandler"))
        {
            TestHttpUpgradeHandler.setTestSet("testUpgradeReadSmallDataInHandler");
        } else if (req.getHeader("TestType").equals("testUpgradeReadListenerTimeout"))
        {
            TestHttpUpgradeHandler.setTestSet("testUpgradeReadListenerTimeout");
        } else if (req.getHeader("TestType").equals("testRead_ContextTransferProperly_WhenUpgradeRLSet"))
        {
            TestHttpUpgradeHandler.setTestSet("testRead_ContextTransferProperly_WhenUpgradeRLSet");
        }
        else if (req.getHeader("TestType").equals("testUpgrade_WriteListener_From_ReadListener"))
        {
            TestHttpUpgradeHandler.setTestSet("testUpgrade_WriteListener_From_ReadListener");
        }

    }

}
