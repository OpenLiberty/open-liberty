/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.saml.fat.IDPInitiated.PkixIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.TimeIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.IDPInitiated.TrustedIssuerIDPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.PkixSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.PkixUnsolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TimeSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TimeUnsolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TrustedIssuerSolicitedSPInitiatedTests;
import com.ibm.ws.security.saml.fat.SPInitiated.TrustedIssuerUnsolicitedSPInitiatedTests;

@RunWith(Suite.class)
@SuiteClasses({

        TimeIDPInitiatedTests.class,
        TimeSolicitedSPInitiatedTests.class,
        TimeUnsolicitedSPInitiatedTests.class, // no lite
        PkixIDPInitiatedTests.class,
        PkixSolicitedSPInitiatedTests.class,
        PkixUnsolicitedSPInitiatedTests.class, // no lite
        TrustedIssuerIDPInitiatedTests.class,
        TrustedIssuerSolicitedSPInitiatedTests.class,
        TrustedIssuerUnsolicitedSPInitiatedTests.class // TrustedIssuerUnsolicitedSPInitiatedTests

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
