/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.health.fat.CDIHealthCheckTest;
import com.ibm.ws.microprofile.health.fat.HealthCheckExceptionTest;
import com.ibm.ws.microprofile.health.fat.HealthTest;
import com.ibm.ws.microprofile.health.fat.MultipleChecksTest;
import com.ibm.ws.microprofile.health.fat.NoHealthCheckAPIImplTest;
import com.ibm.ws.microprofile.health.fat.NoHealthCheckAnnotationTest;

@RunWith(Suite.class)
@SuiteClasses({
                HealthTest.class,
                CDIHealthCheckTest.class,
                MultipleChecksTest.class,
                NoHealthCheckAPIImplTest.class,
                NoHealthCheckAnnotationTest.class,
                HealthCheckExceptionTest.class
})

public class FATSuite {

}
