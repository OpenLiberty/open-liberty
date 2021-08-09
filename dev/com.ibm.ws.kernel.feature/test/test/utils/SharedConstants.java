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
package test.utils;

import java.io.File;

/**
 *
 */
public class SharedConstants {
    /**
     * Test data directory: note the space! always test paths with spaces. Dratted
     * windows.
     */
    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    public static final String TEST_DATA_DIR = testBuildDir + "/test/test data";

    public static final File TEST_DATA_FILE = new File(TEST_DATA_DIR);

    /** Test dist dir: where our generated sample jars live */
    public static final String TEST_DIST_DIR = testBuildDir + "/test/test data/lib";
}
