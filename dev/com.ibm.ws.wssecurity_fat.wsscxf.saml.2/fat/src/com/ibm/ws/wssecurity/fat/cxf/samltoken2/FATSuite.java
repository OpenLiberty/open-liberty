/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.samltoken2.OneServerTests.CxfSAMLCaller1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken2.TwoServerTests.CxfSAMLCaller2ServerTests;

@RunWith(Suite.class)
@SuiteClasses({

	    //Lite
        CxfSAMLCaller1ServerTests.class,
        //Full
        CxfSAMLCaller2ServerTests.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
