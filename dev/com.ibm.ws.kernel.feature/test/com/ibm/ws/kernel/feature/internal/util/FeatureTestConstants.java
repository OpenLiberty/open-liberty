/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal.util;

import java.io.File;

public interface FeatureTestConstants {
    String TEST_BUILD_DIR = System.getProperty("test.buildDir", "generated");
    String TEST_DATA_DIR = TEST_BUILD_DIR + "/test/test data";
    File TEST_DATA_FILE = new File(TEST_DATA_DIR);
}
