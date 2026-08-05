/*******************************************************************************
 * Copyright (c) 2017,2026 IBM Corporation and others.
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
package com.ibm.ws.concurrent.cdi.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                ConcurrentCDI2Test.class, // Jakarta EE 8 and Java EE 8
                ConcurrentCDI3Test.class, // Jakarta EE 9
                ConcurrentCDI4Test.class, // Jakarta EE 10
                ConcurrentCDI4_1Test.class, // Jakarta EE 11
                ConcurrentCDI5Test.class // Jakarta EE 12
})
public class FATSuite {
}
