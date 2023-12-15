/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.custom.junit.runner;

import org.junit.runner.Description;

public class FilterUtils {

    /**
     * Like {@link Description#getTestClass}, but without initializing the class.
     */
    static Class<?> getTestClass(Description desc, Class<?> callingClass) {
        try {
            return Class.forName(desc.getClassName(), false, callingClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
