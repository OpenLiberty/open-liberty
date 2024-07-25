/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.app;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;

import javax.security.auth.Subject;

/**
 * A class for testing related utilities
 */
public class FATUtilities {

    /**
     * In Java 23, the Subject.getSubject(...) method always throws an UnsupportedOperationException
     * So for versions before Java 23, we can still use Subject.getSubject(), but for Java 23 and beyond,
     * we need to switch to using Subject.current(), which was introduced in Java 18.
     *
     * So for Java 22 and earlier, this returns Subject.getSubject(...)
     * For Java 23 and later, this method returns Subject.current()
     *
     * @return
     */
    public static Subject getCurrentSubject() {
        if (JavaInfo.JAVA_VERSION <= 22) {
            // return Subject.getSubject(...)
            return Subject.getSubject(AccessController.getContext());
        } else {
            // return Subject.current()
            try {
                final MethodType subjectMethodType = MethodType.methodType(Subject.class);
                MethodHandle getCurrentMethodHandle = MethodHandles.lookup().findStatic(Subject.class, "current", subjectMethodType);
                return (Subject) getCurrentMethodHandle.invoke();
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
