/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUNTBasicTests;

@RunWith(Suite.class)
@SuiteClasses({

                CxfUNTBasicTests.class
                //CxfSSLUNTNonceTests.class,
                //CxfSSLUNTNonceTimeOutTests.class,
                //CxfNoWssecTests.class,
                //added 10/2020
                //CxfUntNoPassTests.class,
                //CxfX509MigTests.class,
                //CxfCallerUNTTests.class,
                //CxfSampleTests.class,
                //CxfSymSampleTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuiteLite {

}
