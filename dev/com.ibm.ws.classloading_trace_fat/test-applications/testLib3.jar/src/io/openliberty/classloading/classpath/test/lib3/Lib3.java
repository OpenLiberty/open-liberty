/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.test.lib3;

/**
 * Marker class for testLib3.jar — used only to give ShrinkHelper a package
 * to scan when building the JAR.  The actual test resource is lib3.properties.
 */
public class Lib3 {
    public String getMessage() {
        return "Message from Lib3 class";
    }
}
