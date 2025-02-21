/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package io.openliberty.java.internal.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                Java17Test.class,
                Java17CDITest.class,
                Java18Test.class,
                Java18CDITest.class,
                Java18TestJava2SecurityDisabled.class,
                Java19Test.class,
                Java20Test.class,
                Java21Test.class,
                Java22Test.class,
                Java23Test.class,
                Java24Test.class,
                JavaIllegalAccessTest.class,
                AlwaysPassesTest.class
})
public class FATSuite {
}
