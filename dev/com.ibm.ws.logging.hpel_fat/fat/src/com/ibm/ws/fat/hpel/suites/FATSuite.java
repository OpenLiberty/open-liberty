/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//%Z% %I% %W% %G% %U% [%H% %T%]
/*
 * Change History:
 *
 * Reason         Version  Date         User id      Description
 * ----------------------------------------------------------------------------------------------------
 * F017049-21927   8.0     01/28/2010   shighbar     Add testcase for trace to memory buffer by wsadmin
 * 635940          8.0     01/25/2010   spaungam     Add testcase for testing *=all
 * F017049-19395   8.0	   07/21/2010	shighbar     Add remote reader test on HPEL mBean via JMX.
 * F017049-33795   8.0	   11/02/2010   shighbar     Add SSFAT test HpelClassificationOnlyTraceSSFAT.
 * F004749-35525   8.0	   12/07/2010   shighbar     Add SSFAT test HpelRawTraceListSSFAT.
 * 682506          8.0	   12/15/2010	shighbar     Add RAS Raw Trace Filter test cases.
 * 688984          8.0	   02/10/2011	shighbar     Add HpelMBeanOp tests.
 * F017049-41948   8.0	   03/30/2011	shighbar     Add VerifyServiceLogConfig test case.
 * 699970          8.0	   04/05/2011	shighbar     Add HpelInternalTrace test case.
 * 711532          8.0     07/21/2011	shighbar     Remove StartServerWithVerboseLogging from DR suite.
 * 703214          8.0     07/22/2011	shighbar     Add Internal trace guard RawTraceSpecVerifier / MBeanOpTest test.
 * 714823          8.0     08/29/2011	shighbar     Reorder to avoid logViewer monitor hang from impacting follow on tests.
 * 702533          8.0     09/03/2011   olteamh      Add HpelDeleteEmptyDirectories test case.
 * PM48157         8.0     10/03/2011   shighbar     HPEL TextLog retention policy does not remove previous server instances logs.
 * F1344-49496     8.0     10/29/2011   belyi        Add TestLogRecordContextSSFAT test
 * F1344-56880     8.5     01/14/2012   belyi        Add VerifyJmxLogNotification test
 * 95518           8.5.5   03/07/2012   dbourne      remove text log tests
 */
package com.ibm.ws.fat.hpel.suites;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.hpel.tests.BinaryLogExec;
import com.ibm.ws.fat.hpel.tests.BinaryLogRecordContextTest;
import com.ibm.ws.fat.hpel.tests.ConcurrentLoggingAccuracyTest;
import com.ibm.ws.fat.hpel.tests.HpelDeleteEmptyDirectories;
import com.ibm.ws.fat.hpel.tests.TraceSpecificationSetToAllTest;
import com.ibm.ws.fat.hpel.tests.VerifyRepositoryAccuracy;

/**
 * Packages all our HPEL FAT tests
 */
public class FATSuite {

    /**
     * Builds a TestSuite of HPEL Functional Acceptance tests
     * 
     * @return a JUnit TestSuite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("Hpel FAT suite");

/*
 * The RasServerSideUnitTestSuite is not really related to HPEL at all, but had to be included here in the HPEL
 * suite instead of being called directly by the Full Regression because it makes use of FAT resources such as
 * HpelSetup.getServerUnderTest(). Until we refactor the setup of the servers etc. out of HpelSetup and into
 * RASSetup etc. It must be called from here.
 */
        // server side ras legacy test suite
//		suite.addTest(RasServerSideUnitTestSuite.suite());
//		
//		// ========== RAS FAT test cases ========
//		suite.addTestSuite(com.ibm.ws.fat.ras.tests.AttributeSetTestingByWsadmin.class);
//		suite.addTestSuite(com.ibm.ws.fat.ras.tests.TraceServiceTestingByWsAdmin.class);
//		suite.addTestSuite(MBeanOpTest.class);
//		suite.addTestSuite(VerifyServiceLogConfig.class);
//		
//		// ========== FAT test cases ============
//		suite.addTestSuite(EnableHPELByWsadmin.class);
        suite.addTestSuite(BinaryLogRecordContextTest.class);
        suite.addTestSuite(VerifyRepositoryAccuracy.class);
        suite.addTestSuite(BinaryLogExec.class);
//		suite.addTestSuite(EnableTextLogByWsadmin.class);
//		suite.addTestSuite(EnableRuntimeTraceSpecByWsadmin.class);
//		suite.addTestSuite(ChangeRepositoryDirectoryByWsAdmin.class);
        suite.addTestSuite(ConcurrentLoggingAccuracyTest.class);
//		suite.addTestSuite(LogViewerFormatValidation.class);
//        suite.addTestSuite(MergedRepositories.class);
//		suite.addTestSuite(com.ibm.ws.fat.hpel.tests.AttributeSetTestingByWsadmin.class);
//		suite.addTestSuite(ControlTraceMemoryByWsadmin.class);
//		suite.addTestSuite(DisableHPELByWsadmin.class);
//		suite.addTestSuite(HpelRemoteReaderMBeanByJMX.class);
//		suite.addTestSuite(VerifyJmxLogNotification.class);
//		suite.addTestSuite(TestOwnershipVerification.class);
//		suite.addTestSuite(RawTraceSpecVerifier.class);
        suite.addTestSuite(HpelDeleteEmptyDirectories.class);
//        suite.addTestSuite(HpelTextLogRetention.class);
//		
//		// =========== FAT Server Side test cases ==========
//		suite.addTestSuite(HpelMBeanOpSSFAT.class);
//		suite.addTestSuite(HpelLogEventNotificationsSSFAT.class);
//		suite.addTestSuite(HpelMemoryBufferSSFAT.class);
//		suite.addTestSuite(HpelReadWriteSSFAT.class);
//		suite.addTestSuite(HpelClassificationOnlyTraceSSFAT.class);
//		suite.addTestSuite(HpelRetentionSSFAT.class);
//		suite.addTestSuite(HpelLogManResetSSFAT.class);
//		suite.addTestSuite(HpelRawTraceListSSFAT.class);
//		suite.addTestSuite(HpelInternalTraceSSFAT.class);
//		suite.addTestSuite(TestLogRecordContextSSFAT.class);
//		suite.addTestSuite(LogViewerSSFAT.class);
        suite.addTestSuite(TraceSpecificationSetToAllTest.class);
// removing temporarily for defect 158844        suite.addTestSuite(HpelPurgeMaxSizeIgnoreTest_2.class);
// removing temporarily for defect 158844        suite.addTestSuite(HpelPurgeMaxSizeIgnoreTest_1.class);

        return new HpelSetup(suite);
    }
}
