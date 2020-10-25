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

import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerUNTTests;
import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerX509AsymTests;
import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerX509SymTests;
import com.ibm.ws.wssecurity.fat.cxf.nowssec.CxfNoWssecTests;
import com.ibm.ws.wssecurity.fat.cxf.sample.CxfBspTests;
import com.ibm.ws.wssecurity.fat.cxf.sample.CxfInteropX509Tests;
import com.ibm.ws.wssecurity.fat.cxf.sample.CxfSampleTests;
import com.ibm.ws.wssecurity.fat.cxf.sample.CxfSymSampleTests;
import com.ibm.ws.wssecurity.fat.cxf.sha2sig.CxfSha2SigTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfDeriveKeyTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfPasswordDigestTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTBasicTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTNonceTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTNonceTimeOutTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUNTBasicTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUNTNonceTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUntNoPassTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11enc.CxfWss11EncTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11sig.CxfWss11SigTests;
import com.ibm.ws.wssecurity.fat.cxf.wsstemplates.CxfWssTemplatesTestsWithExternalPolicy;
import com.ibm.ws.wssecurity.fat.cxf.wsstemplates.CxfWssTemplatesTestsWithWSDL;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensAsymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymSha2NegativeTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ASyncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509BasicTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509CrlTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509EncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ObjectTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509OverRideTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509StrTypeTests;

@RunWith(Suite.class)
@SuiteClasses({

                //CxfSSLUNTBasicTests.class,
                //CxfSSLUNTNonceTests.class,
                //CxfSSLUNTNonceTimeOutTests.class,
                //CxfNoWssecTests.class,
                //CxfPasswordDigestTests.class,
                CxfUNTBasicTests.class
                //CxfUNTNonceTests.class,
                //CxfUntNoPassTests.class,
                //CxfWssTemplatesTestsWithWSDL.class,
                //CxfWssTemplatesTestsWithExternalPolicy.class,
                //CxfWss11SigTests.class,
                //CxfWss11EncTests.class,
                //CxfX509MigSymSha2NegativeTests.class,
                //CxfX509MigSymTests.class,
                //CxfX509MigTests.class,
                //CxfX509BasicTests.class,
                //CxfX509EncTests.class,
                //CxfX509ObjectTests.class,
                //CxfX509OverRideTests.class,
                //CxfX509SigTests.class,
                //CxfX509ASyncTests.class,
                //CxfX509StrTypeTests.class,
                //CxfX509CrlTests.class,
                //CxfDeriveKeyTests.class,
                //CxfEndSupTokensAsymTests.class,
                //CxfEndSupTokensSymTests.class,
                //CxfEndSupTokensSym2Tests.class,
                //CxfMustUnderstandTests.class,
                //CxfCallerUNTTests.class,
                //CxfCallerX509AsymTests.class,
                //CxfCallerX509SymTests.class,
                //CxfSampleTests.class,
                //CxfSymSampleTests.class,
                //CxfSha2SigTests.class,
                //CxfBspTests.class,
                //CxfInteropX509Tests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
