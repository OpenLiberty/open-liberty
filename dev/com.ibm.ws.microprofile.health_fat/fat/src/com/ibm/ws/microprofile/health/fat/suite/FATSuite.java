package com.ibm.ws.microprofile.health.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.health.fat.CDIHealthCheckTest;
import com.ibm.ws.microprofile.health.fat.HealthCheckExceptionTest;
import com.ibm.ws.microprofile.health.fat.HealthTest;
import com.ibm.ws.microprofile.health.fat.MultipleChecksTest;
import com.ibm.ws.microprofile.health.fat.NoHealthCheckAPIImplTest;
import com.ibm.ws.microprofile.health.fat.NoHealthCheckAnnotationTest;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

@RunWith(Suite.class)
@SuiteClasses({
                HealthTest.class,
                CDIHealthCheckTest.class,
                MultipleChecksTest.class,
                NoHealthCheckAPIImplTest.class,
                NoHealthCheckAnnotationTest.class,
                HealthCheckExceptionTest.class
})

public class FATSuite {

}
