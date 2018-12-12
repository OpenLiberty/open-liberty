/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.ormdiagnostics;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.ormdiagnostics.tests.TestBasicLibertyDump;
import com.ibm.ws.jpa.ormdiagnostics.tests.TestEARLibertyDump;

@RunWith(Suite.class)
@SuiteClasses({
                TestBasicLibertyDump.class,
                TestEARLibertyDump.class
})
public class FATSuite {

}
