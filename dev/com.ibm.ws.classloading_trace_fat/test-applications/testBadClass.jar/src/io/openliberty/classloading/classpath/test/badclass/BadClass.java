/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.classpath.test.badclass;

/**
 * Marker source file for the bad-class test JAR.
 * The compiled .class bytes for this class are intentionally replaced
 * with invalid (corrupt) bytes by FATSuite before the JAR is packaged,
 * so that AppClassLoader.defineClass() throws ClassFormatError and emits
 * a "CLASS FAIL" trace line.
 */
public class BadClass {
    public String getMessage() {
        return "Message from BadClass";
    }
}
