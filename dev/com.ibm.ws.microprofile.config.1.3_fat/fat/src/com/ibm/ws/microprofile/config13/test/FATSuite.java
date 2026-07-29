/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
//                HotAddMPConfig.class, //FULL
//                ServerXMLTest.class, //FULL
//                VariableServerXMLTest.class, //LITE
//                ConfigOrdinalServerXMLTest.class, // FULL
                VarExpansionTest.class //LITE
})
public class FATSuite {

}
