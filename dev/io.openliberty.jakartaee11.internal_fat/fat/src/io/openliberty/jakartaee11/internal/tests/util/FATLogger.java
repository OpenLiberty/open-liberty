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
package io.openliberty.jakartaee11.internal.tests.util;

import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

public class FATLogger {
    public static void info(Class<?> c, String method, String text) {
        Log.info(c, method, text);
    }

    public static void dumpErrors(Class<?> c, String method, String title, List<String> errors) {
        Log.info(c, method, title);
        for (String error : errors) {
            Log.info(c, method, error);
        }
    }
}
