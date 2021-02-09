/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.health20.fat.ApplicationStateHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.DelayAppStartupHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.DifferentApplicationNameHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.MultipleHealthCheckTest;

@RunWith(Suite.class)
@SuiteClasses({
                ApplicationStateHealthCheckTest.class,
                DelayAppStartupHealthCheckTest.class,
                MultipleHealthCheckTest.class,
                DifferentApplicationNameHealthCheckTest.class
})

public class FATSuite {

}
