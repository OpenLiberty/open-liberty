/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package test;

public class HelloD {
    private static String staticValue = "default";

    public static String areYouThere(String name) {
        return "test.HelloC" + name;
    }

    public void setstaticValue(String val) {
        staticValue = val;
    }

    public String getstaticValue() {
        return staticValue;
    }

    public ClassLoader getMyClassLoader() {
        return this.getClass().getClassLoader();
    }
}
