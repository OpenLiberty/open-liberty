/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.junit.rules;

public class JavaInfoUnitTestUtils {
    public static final int JAVA_VERSION = new JavaInfoUnitTestUtils().majorVersion();

    private final int MAJOR;

    private JavaInfoUnitTestUtils() {
        // Parse MAJOR and MINOR versions
        String specVersion = System.getProperty("java.specification.version");
        String[] versions = specVersion.split("[^0-9]"); // split on non-numeric chars
        // Offset for 1.MAJOR.MINOR vs. MAJOR.MINOR version syntax
        int offset = "1".equals(versions[0]) ? 1 : 0;
        if (versions.length <= offset)
            throw new IllegalStateException("Bad Java runtime version string: " + specVersion);
        MAJOR = Integer.parseInt(versions[offset]);
    }

    private int majorVersion() {
        return MAJOR;
    }

}