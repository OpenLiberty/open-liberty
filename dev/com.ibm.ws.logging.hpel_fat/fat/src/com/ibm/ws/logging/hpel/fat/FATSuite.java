/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
 * GH 12035        18.0    08/06/2018   pgunapal     Added test case for hideMessages and --excludeMessage
 */
package com.ibm.ws.logging.hpel.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                BinaryLogRecordContextTest.class,
                VerifyRepositoryAccuracy.class,
                BinaryLogExec.class,
                ConcurrentLoggingAccuracyTest.class,
                HpelDeleteEmptyDirectories.class,
                HPELHideMessagesTest.class,
                TraceSpecificationSetToAllTest.class,
                HpelPurgeMaxSizeIgnoreTest_2.class,
                HpelPurgeMaxSizeIgnoreTest_1.class

//                HPELDataDirFalsePositiveWarningTest.class,
//                HpelLogDirectoryChangeTest.class,
//                HpelLoggingElementDeleteTest.class,
//                HPELLogDirectoryPurgeMaxSizeTest.class,
//                HpelPurgeMaxSizeBackupFileTest.class,
//                HpelTextLogRetention.class,
//                LogViewerExec.class
})

public class FATSuite {

}
