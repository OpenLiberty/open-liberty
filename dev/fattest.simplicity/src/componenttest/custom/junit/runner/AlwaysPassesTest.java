/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import org.junit.Test;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test that will pass in all test modes on all platforms with any JDK.
 *
 * Intended for use in test buckets where all other tests may be filtered out
 * for some test modes or environments (such as only for Java 8). Since the build
 * requires at lest one passing test, this provides a simple way to insure one
 * test is not filtered, and always reports passing.
 */
public class AlwaysPassesTest {

    @Test
    @Mode(TestMode.LITE)
    @MinimumJavaLevel(javaLevel = 7)
    public void testThatWillAlwaysPass() throws Exception {}

}
