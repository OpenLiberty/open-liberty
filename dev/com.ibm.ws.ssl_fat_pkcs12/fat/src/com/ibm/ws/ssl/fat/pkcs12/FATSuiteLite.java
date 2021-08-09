package com.ibm.ws.ssl.fat.pkcs12;

/*******************************************************************************
 * Copyright (c) 2018 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                DefaultJKSDoesNotExistSSLTest.class,
                DefaultPKCS12DoesNotExistSSLTest.class,
                DefaultJKSExistsSSLTest.class,
                DefaultPKCS12ExistsSSLTest.class,
                NonDefaultJKSSSLTest.class,
                NonDefaultPKCS12SSLTest.class,
                DefaultKeystoreNonDefaultLocation.class })
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuiteLite extends CommonLocalLDAPServerSuite {

}