/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.test.lib1;

import io.openliberty.classloading.libs.util.CodeSourceUtil;

/**
 *
 */
public class API_User {
    public static String getCodeSourceA1() throws ClassNotFoundException {
        return CodeSourceUtil.getClassCodeSourceFileName(Class.forName("test.bundle.api1.a.API_A1"));
    }

    public static String getCodeSourceB1() throws ClassNotFoundException {
        return CodeSourceUtil.getClassCodeSourceFileName(Class.forName("test.bundle.api1.b.API_B1"));
    }

}
