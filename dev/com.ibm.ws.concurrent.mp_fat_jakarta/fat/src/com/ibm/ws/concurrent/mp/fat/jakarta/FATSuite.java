/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat.jakarta;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                MPContextProp1_3_Test.class,

// TODO eventually, this is intended to test a Jakarta EE 10 server
// with MicroProfile Context Propagation 2.0.
// This test is disabled until
// the EE 10 compatible version of MP Context Propagation becomes available.
//MPContextProp2_0_Test.class
})
public class FATSuite {
}
