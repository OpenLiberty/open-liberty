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

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerUNTTests;
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
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUNTBasicTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUNTNonceTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfUntNoPassTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfWssTemplatesTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11enc.CxfWss11EncTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11sig.CxfWss11SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensAsymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymSha2NegativeTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509BasicTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509CrlTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509EncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ObjectTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509OverRideTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509StrTypeTests;

import componenttest.common.apiservices.LocalMachine;

/**
 * Dynamically builds the list of classes to include in JUnit suite
 *
 * @author syed
 *
 */
public class FATSuiteBuilder {

    private static final Class<?> thisClass = FATSuiteBuilder.class;

    public static Class[] suite() throws Exception {

        String thisMethod = "suite";
        boolean isThisIbmJdk = false;
        Class[] allClasses = {
                               CxfSSLUNTBasicTests.class,
                               CxfSSLUNTNonceTests.class,
                               CxfNoWssecTests.class,
                               CxfPasswordDigestTests.class,
                               CxfUNTBasicTests.class,
                               CxfUNTNonceTests.class,
                               CxfUntNoPassTests.class,
                               CxfWssTemplatesTests.class,
                               CxfWss11SigTests.class,
                               CxfWss11EncTests.class,
                               CxfX509MigSymSha2NegativeTests.class,
                               CxfX509MigSymTests.class,
                               CxfX509MigTests.class,
                               CxfX509BasicTests.class,
                               CxfX509EncTests.class,
                               CxfX509ObjectTests.class,
                               CxfX509OverRideTests.class,
                               CxfX509SigTests.class,
                               CxfX509StrTypeTests.class,
                               CxfX509CrlTests.class,
                               CxfDeriveKeyTests.class,
                               CxfEndSupTokensAsymTests.class,
                               CxfEndSupTokensSymTests.class,
                               //CxfEndSupTokensSym2Tests.class,
                               //CxfMustUnderstandTests.class,
                               CxfCallerUNTTests.class,
                               CxfSampleTests.class,
                               CxfSymSampleTests.class,
                               CxfInteropX509Tests.class,
                               CxfSha2SigTests.class,
                               CxfBspTests.class

        };
        Class[] nonTwasClasses = {
                                   CxfSSLUNTBasicTests.class,
                                   CxfSSLUNTNonceTests.class,
                                   CxfNoWssecTests.class,
                                   CxfPasswordDigestTests.class,
                                   CxfUNTBasicTests.class,
                                   CxfUNTNonceTests.class,
                                   CxfUntNoPassTests.class,
                                   CxfWssTemplatesTests.class,
                                   CxfWss11SigTests.class,
                                   CxfWss11EncTests.class,
                                   CxfX509MigSymSha2NegativeTests.class,
                                   CxfX509MigSymTests.class,
                                   CxfX509MigTests.class,
                                   CxfX509BasicTests.class,
                                   CxfX509EncTests.class,
                                   CxfX509ObjectTests.class,
                                   CxfX509OverRideTests.class,
                                   CxfX509SigTests.class,
                                   CxfX509StrTypeTests.class,
                                   CxfX509CrlTests.class,
                                   CxfDeriveKeyTests.class,
                                   CxfEndSupTokensAsymTests.class,
                                   //CxfMustUnderstandTests.class,
                                   CxfCallerUNTTests.class,
                                   CxfSampleTests.class,
                                   CxfSymSampleTests.class,
                                   CxfSha2SigTests.class,
                        // CxfBspTests.class, this is for interop tests
                        // CxfInteropX509Tests.class, this is for interop tests
        };
        LocalMachine lclMachine = new LocalMachine();
        String vendorName = System.getProperty("java.vendor");
        Log.info(thisClass, thisMethod, "DEBUG:Java Vendor: " + vendorName);
        OperatingSystem localOs = lclMachine.getOperatingSystem();
        String myLocalOs = "" + localOs;
        Log.info(thisClass, thisMethod, "DEBUG:Local Operating system: " + myLocalOs);

        /*
         * if (myLocalOs.equals("WINDOWS") || myLocalOs.equals("AIX")||
         * myLocalOs.equals("LINUX") || myLocalOs.equals("ZOS") ) { // ibm jdk
         * isThisIbmJdk = true;
         * }
         */

        if (vendorName.contains("IBM")) {
            isThisIbmJdk = true;
        }

        return (isThisIbmJdk ? allClasses : nonTwasClasses);

    }
}
