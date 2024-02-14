/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.javaee.ddmodel.suite.core.*;
import com.ibm.ws.javaee.ddmodel.suite.simple.war.*;
import com.ibm.ws.javaee.ddmodel.suite.simple.ear.*;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses( {
    AlwaysPassesTest.class,

    DDValidAppTests.class,
    DDValidAppTests_J9.class,
    DDValidAppTests_J10.class,
    DDValidAppTests_J11.class,

    DDValidModTests.class,
    DDValidModTests_J9.class,
    DDValidModTests_J10.class,
    DDValidModTests_J11.class,

    DDValidAppPartialHeaderTests.class,
    DDValidAppMinimalHeaderTests.class,

    DDValidModPartialHeaderTests.class,
    DDValidModMinimalHeaderTests.class,

    DDNonValidTests.class,

    // War, no definitions
    DDSimpleWarTests.class,

    // War, single definition
    DDSimpleWarTests_Web.class,
    DDSimpleWarTests_Config.class,
    DDSimpleWarTests_ConfigExt.class,
    DDSimpleWarTests_ConfigExt_Exp.class,    

    // War, two definitions
    DDSimpleWarTests_Web_Config.class,
    DDSimpleWarTests_Web_ConfigExt.class,
    DDSimpleWarTests_Web_ConfigExt_Exp.class,    
    DDSimpleWarTests_Config_ConfigExt.class,
    DDSimpleWarTests_Config_ConfigExt_Exp.class,

    // War, three definitions
    DDSimpleWarTests_Web_Config_ConfigExt.class,
    DDSimpleWarTests_Web_Config_ConfigExt_Exp.class,

    // Ear, no definitions
    DDSimpleEarTests.class,

    // Ear, single definition
    DDSimpleEarTests_Web.class,
    DDSimpleEarTests_App.class,
    DDSimpleEarTests_ConfigExt.class,
    DDSimpleEarTests_ConfigExt_Exp.class,

    // Ear, two definitions, web
    DDSimpleEarTests_Web_App.class,    
    DDSimpleEarTests_Web_ConfigExt.class,
    DDSimpleEarTests_Web_ConfigExt_Exp.class,

    // Ear, two definitions, app
    DDSimpleEarTests_App_ConfigExt.class,
    DDSimpleEarTests_App_ConfigExt_Exp.class,

    // Ear, three definitions 
    DDSimpleEarTests_Web_App_ConfigExt.class,
    DDSimpleEarTests_Web_App_ConfigExt_Exp.class,    
})
public class FATSuite {
    // EMPTY
}
