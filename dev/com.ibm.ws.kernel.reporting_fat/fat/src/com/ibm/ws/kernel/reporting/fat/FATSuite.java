package com.ibm.ws.kernel.reporting.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ CVEReportingConfigTest.class, CVEDataTest.class, CVEResponseTest.class })
public class FATSuite {

}
