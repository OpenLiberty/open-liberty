/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
package com.ibm.ws.java11_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                Java11Test.class,
                Java12Test.class,
                Java13Test.class,
                Java14Test.class,
                Java15Test.class,
                Java16Test.class,
                Java11CNFETest.class,
                JavaInfoTest.class,
                MultiReleaseJarTest.class,
                MultiReleaseJarTestWithJarURLs.class
})
public class FATSuite {
}
