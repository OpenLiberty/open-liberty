/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test;

import java.io.File;

/**
 *
 */
public class TestConstants {
    public static final String TEST_DATA_PATH = "test/test data/";

    public static final File TEST_DATA = new File(TEST_DATA_PATH);

    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    public static final String BUILD_TMP_PATH = testBuildDir + "/logs/";

    public static final File BUILD_TMP = new File(BUILD_TMP_PATH);

}
