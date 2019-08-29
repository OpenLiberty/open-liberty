package com.ibm.ws.jdbc.fat.sqlserver;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
               AlwaysPassesTest.class,
               SQLServerTest.class
})
public class FATSuite {}