/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.logstash.collector.tests.CustomizedTagTest;
import com.ibm.ws.logstash.collector.tests.LogStashWithBinaryLoggingTest;
import com.ibm.ws.logstash.collector.tests.LogsStashSSLTest;
import com.ibm.ws.logstash.collector.tests.LogstashCollectorIndependentTest;
import com.ibm.ws.logstash.collector.tests.MaxFieldLengthTest;
import com.ibm.ws.logstash.collector.tests.ThrottleMaxEventsTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // Must keep this test to run something in the Java 6 builds.
                //               ValidateEnableLogTest.class,
//               ValidateTraceLogEnableTest.class,
//               JsonNewAttributesTest.class,
//               FeatureValidateTest.class,
//               FeatureDisableTest.class,
//               ValidateDefaultCollectorConfigTest.class,
//               ValidateMessagesLogTest_full.class,
//               ValidateAccessDataTest_full.class,
//               ValidateTraceLogTest_full.class,
//               ValidateMessageTraceRequestSizeTest.class,
//               ValidateBlockUrlFromAdminCenterTest.class,
//               CollectorToEngineInCollectiveTest.class,
//               ValidateFFDCLogTest_full.class
                CustomizedTagTest.class,
                ThrottleMaxEventsTest.class,
                LogsStashSSLTest.class,
                MaxFieldLengthTest.class,
                LogStashWithBinaryLoggingTest.class,
                LogstashCollectorIndependentTest.class

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {
}
