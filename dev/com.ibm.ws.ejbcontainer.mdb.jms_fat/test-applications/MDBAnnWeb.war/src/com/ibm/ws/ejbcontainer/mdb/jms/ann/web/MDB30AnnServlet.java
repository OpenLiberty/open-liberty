/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.ann.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.BMTBeanIA;
import com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.BMTBeanNoCommit;
import com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.CMTBeanRequired;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>MDB30AnnTest
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function test cases. These test cases are the same as other test cases in
 * package com.ibm.ws.ejbcontainer.mdb.jms. However, they are modified to use EJB 3 POJO MDB
 * instead of MDB 2.x beans.
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
 * <li> testCMTIA - testing the EJB containter's behavior for CMT MDB
 * <li> - MIA01 - ejbCreate: getCallerPrincipal
 * <li> - MIA02 - ejbCreate: isCallerInRole
 * <li> - MIA03 - ejbCreate: getEJBHome
 * <li> - MIA04 - ejbCreate: getEJBLocalHome
 * <li> - MIA05 - ejbCreate: getUserTransaction
 * <li> - MIA06 - ejbCreate: getRollbackOnly
 * <li> - MIA07 - setMsgDrivenCtxt: JNDI
 * <li> - MIA08 - onMsg: getCallerPrincipal
 * <li> - MIA09 - onMsg: isCallerInRole
 * <li> - MIA10 - onMsg: getEJBHome
 * <li> - MIA11 - onMsg: getEJBLocalHome
 * <li> - MIA12 - onMsg: getUserTransaction
 * <li> - MIA13 - onMsg: getRollbackOnly
 * <li> - MCM04 - MDB => SL local home
 * <li> - MIA05 - MDB => SL local object method
 * <li> - MIA06 - MDB => SL remote home
 * <li> - MIA07 - MDB => SL remote object method
 * <li> - MCM08 - MDB => CMP remote home
 * <li> - MIA09 - MDB => CMP remote object method
 * <li> - MIA10 - MDB => BMP local home
 * <li> - MIA11 - MDB => BMP local object method
 * <li> - MIA12 - MDB => BMP remote home
 * <li> - MIA13 - MDB => BMP remote object method
 * <li>
 * <li> testBMTIA - testing the EJB containter's behavior for BMT MDB
 * <li> - MIA14 - ejbCreate: getCallerPrincipal
 * <li> - MIA15 - ejbCreate: isCallerInRole
 * <li> - MIA16 - ejbCreate: getEJBHome
 * <li> - MIA17 - ejbCreate: getEJBLocalHome
 * <li> - MIA18 - ejbCreate: getUserTransaction
 * <li> - MIA19 - ejbCreate: getRollbackOnly
 * <li> - MIA20 - ejbCreate: setRollbackOnly
 * <li> - MIA21 - setMsgDrivenCtxt: JNDI
 * <li> - MIA22 - onMsg: getCallerPrincipal
 * <li> - MIA23 - onMsg: isCallerInRole
 * <li> - MIA24 - onMsg: getEJBHome
 * <li> - MIA25 - onMsg: getEJBLocalHome
 * <li> - MIA26 - onMsg: getUserTransaction
 * <li> - MIA27 - onMsg: getRollbackOnly
 * <li> - MIA28 - onMsg: setRollbackOnly
 * <li> - MIA29 - onMsg: SF local home
 * <li> - MIA30 - onMsg: SF local object's method
 * <li> - MIA31 - onMsg: SF remote home
 * <li> - MIA32 - onMsg: SF remote object's method
 * <li> - MTX01 - set isolation level
 * <li> - MTX05 - onMsg is not part of a Tx
 * <li> - MTX06 - NotSupportedException: UserTransaction.begin() before commiting the previous transaction
 * <li>
 * <li> testMessageSelector - MCM01 - Make an MDB using the msg selector
 * <li>
 * <li> testNonDurableTopic - MCM02 - Test non-durable is the default settings for Topic
 * <li>
 * <li> testTxSupport - MTX02 - rollback
 * <li> - MTX03 - commit before onMsg returns
 * <li> - MTX04 - no commit before onMsg returns
 * <li>
 * <li> testCMTNotSupported - MTX07 - CMT's onMsg() with 'NotSupported' invoking getGlobalTransaction to see onMsg is not part of any transaction context
 * <li> - MTX08 - CMT: onMsg with Tx attribute 'NotSupported' access a CMTD SLL with T attribute 'supports', check no Tx context is passed to SL
 * <li> - MTX14 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking setRollbackOnly
 * <li> - MTX15 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking getRollbackOnly
 * <li>
 * <li> testCMTRequired - MTX09 - CMT's onMsg() with 'Required' getting GlobalTransaction to see a transaction context, and accessing a CMTD SLL with T attribute 'supports', check
 * the Tx context is passed to SL
 * <li> - MTX10 - CMT: onMsg with Tx attribute 'NotSupported' access a CMTD SLL with T attribute 'supports', check the Tx context is passed to SL
 * <li> - MTX13 - CMT onMsg with 'Required' invoking getRollbackOnly
 * <li> - MTX11 - CMT onMsg with 'Required', the Tx is committed (DB update is commited)
 * <li>
 * <li> testCMTRequiredRollback- MTX12 - CMT's onMsg() with 'Required', the Tx is not committed (DB update is not commited)
 * <li>
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@WebServlet("/MDB30AnnServlet")
@SuppressWarnings("serial")
public class MDB30AnnServlet extends FATServlet {
    private final static String CLASSNAME = MDB30AnnServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // JNDI for JMS Resources
    String qcfName = "jms/TestQCF";
    String testResultQueueName = "jms/TestResultQueue";
    String bmtRequestQueueName = "jms/BMTReqQueue";
    String bmtNoCommitRequestQueueName = "jms/BMTNoCommitReqQueue";
    String cmtRequestQueueName = "jms/CMTReqQueue";
    String cmtNotSupportedRequestQueueName = "jms/CMTNotSupportedReqQueue";
    String cmtRequiredRequestQueueName = "jms/CMTRequiredReqQueue";
    String tcfName = "jms/TestTCF";
    String tnews = "jms/news";
    String tselect = "jms/select";
    String tsports = "jms/sports";

    /*
     * testBMTIA()
     *
     * MIA02 - Illegal access for BMT
     * MTX14 - BMT: UserTransaction.begin() before committing the previous transaction
     */
    public void testAnnBMTIA() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("Request test results", qcfName, bmtRequestQueueName);
        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }

        svLogger.info("checking data for BMTTxCommit ...");
        svLogger.info("Test Point: BMTTxCommit getIntValue: " + BMTBeanIA.commitBean.getIntValue());
        assertTrue("BMTTxCommit", (BMTBeanIA.commitBean.getIntValue() == 1));
        svLogger.info("done");

        svLogger.info("checking data for BMTTxRollback ...");
        svLogger.info("Test Point: BMTTxRollback getIntValue: " + BMTBeanIA.rollbackBean.getIntValue());
        assertTrue("BMTTxRollback", (BMTBeanIA.rollbackBean.getIntValue() == 0));
        svLogger.info("done");
    }

    /*
     * testBMTNoCommit()
     *
     * send a message to MDB BMTBeanNoCommit
     */
    public void testAnnBMTNoCommit() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("Request test results", qcfName, bmtNoCommitRequestQueueName);
        svLogger.info("Message sent to MDB BMTBeanNoCommit.");

        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }
        FATMDBHelper.emptyQueue(qcfName, bmtNoCommitRequestQueueName);

        svLogger.info("checking data for BMTTxNoCommit ...");
        svLogger.info("Test Point: BMTTxNoCommit getIntValue: " + BMTBeanNoCommit.noCommitBean.getIntValue());
        assertTrue("BMTTxNoCommit", (BMTBeanNoCommit.noCommitBean.getIntValue() == 0));
        svLogger.info("done");
    }

    /*
     * testCMTIA()
     *
     * MIA01 - Illegal access for CMT
     * MCM08 - MDB to access SLL and SLR
     */
    public void testAnnCMTIA() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("Request test results", qcfName, cmtRequestQueueName);
        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }
    }

    /*
     * testNonDurableTopic()
     *
     * MCM03 - Test non-durable is the default settings for Topic
     */
    public void testAnnNonDurableTopic() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putTopicMessage("Request test results", tcfName, tnews);
        String s = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        svLogger.info("Received message : " + s);
        assertEquals("Compare received message with expected message", "testNonDurableTopic passed", s);
    }

    /*
     * testDurableTopic()
     *
     * MCM04 - Stop the server and receive message
     */
    public void testAnnDurableTopic1() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putTopicMessage("Request test results", tcfName, tsports);
    }

    public void testAnnDurableTopic2() throws Exception {
        String s = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        svLogger.info("Received message : " + s);
        assertEquals("Compare received message with expected message", "testDurableTopic passed", s);
    }

    /*
     * testMessageSelector()
     *
     * MCM02 - Make an MDB using the msg selector
     */
    public void testAnnMessageSelector() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putTopicMessage("Request test results", "FAILED", tcfName, tselect);
        FATMDBHelper.putTopicMessage("Request test results", "MCM02", tcfName, tselect);

        String s = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        svLogger.info("Received message : " + s);
        assertEquals("Compare received message with expected message", "testMessageSelector passed", s);
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
    }

    /*
     * testCMTNotSupported()
     *
     * MTX15 - CMT's onMsg() with 'NotSupported' invoking getGlobalTransaction to see onMsg is not part of any transaction context
     * MTX17 - CMT's onMsg() with 'Required' transaction attribute accessing a CMTD SLL with T attribute 'supports', check no Tx context is passed to SL
     * MTX20 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking setRollbackOnly
     * MTX21 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking getRollbackOnly
     */
    public void testAnnCMTNotSupported() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("Request test results", qcfName, cmtNotSupportedRequestQueueName);

        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }
    }

    /*
     * testCMTRequired()
     *
     * MTX17 - CMT's onMsg() with 'Required' getting GlobalTransaction to see a transaction context, and accessing a CMTD SLL with T attribute 'supports', check the Tx context is
     * passed to SL
     * MTX19 - CMT's onMsg() with 'Required' invoking getRollbackOnly
     */
    public void testAnnCMTRequired() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("CMT COMMIT", qcfName, cmtRequiredRequestQueueName);

        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }

        svLogger.info("checking data for CMTTxCommit ...");
        svLogger.info("Test Point: CMTTxCommit getIntValue: " + CMTBeanRequired.commitBean.getIntValue());
        assertTrue("CMTTxCommit", (CMTBeanRequired.commitBean.getIntValue() == 1));
        svLogger.info("done");
    }

    /*
     * testCMTRequiredRollback()
     *
     * MTX19 - CMT's onMsg() with 'Required', the Tx is not committed (DB update is not commited)
     */
    public void testAnnCMTRequiredRollback() throws Exception {
        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("CMT ROLLBACK", qcfName, cmtRequiredRequestQueueName);

        String results = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);

        if (results == null) {
            fail("Reply is an empty vector. No test results are collected.");
        }

        if (results.equals("")) {
            fail("No worthwhile results were returned.");
        }

        if (results.contains("FAIL")) {
            svLogger.info(results);
            fail("Reply contained FAIL keyword.  Look at client log for full result text.");
        }

        FATMDBHelper.emptyQueue(qcfName, cmtRequiredRequestQueueName);

        svLogger.info("checking data for CMTTxRollback ...");
        svLogger.info("Test Point: CMTTxRollback getIntValue: " + CMTBeanRequired.rollbackBean.getIntValue());
        assertTrue("CMTTxRollback", (CMTBeanRequired.rollbackBean.getIntValue() == 0));
        svLogger.info("done");
    }
}