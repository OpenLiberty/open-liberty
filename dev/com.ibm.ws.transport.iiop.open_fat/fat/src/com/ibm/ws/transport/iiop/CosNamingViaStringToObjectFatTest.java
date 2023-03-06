/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.transport.iiop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.MaximumJavaLevel;

public class CosNamingViaStringToObjectFatTest extends AbstractCosNamingFatTest {

    @BeforeClass
    public static void setup() throws Exception {
        setupUsingStringToObject();
    }

    @AfterClass
    public static void teardown() {
        destroyORB();
    }

    @Test
    @MaximumJavaLevel(javaLevel = 8)
    @Override
    public void testNameServiceListing() throws Exception {
        super.testNameServiceListing();
    }
}
