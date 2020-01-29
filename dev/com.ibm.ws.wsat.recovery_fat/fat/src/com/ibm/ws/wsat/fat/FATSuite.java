package com.ibm.ws.wsat.fat;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
	SingleRecoveryTest.class,
	MultiRecoveryTest.class,
})
public class FATSuite {

}
