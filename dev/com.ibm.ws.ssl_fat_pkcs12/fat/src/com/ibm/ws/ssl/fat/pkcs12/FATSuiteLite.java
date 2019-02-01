package com.ibm.ws.ssl.fat.pkcs12;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DefaultJKSDoesNotExistSSLTest.class,
                DefaultPKCS12DoesNotExistSSLTest.class,
                DefaultJKSExistsSSLTest.class,
                DefaultPKCS12ExistsSSLTest.class,
                NonDefaultJKSSSLTest.class,
                NonDefaultPKCS12SSLTest.class })
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuiteLite extends CommonLocalLDAPServerSuite {

}