/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package java11.multirelease.jar;

public class MRClass_RequireJava15 {

    // WARNING: If you need to modify this file, you must manually patch it into the corresponding location in publish/servers/server_MultiReleaseJarTest/lib/multiRelease.jar

    // This class should be in /META-INF/versions/15/java11/cnfe/web/ and only loadable on Java 15+

    private static final int JAVA_VERSION = 15;

    public static int getJavaVersion() {
        return JAVA_VERSION;
    }

    public class MRInnerClass_Overridden {
        public int getJavaVersion() {
            return JAVA_VERSION;
        }
    }

    public static class MRStaticInnerClass_Overridden {
        public static int getJavaVersion() {
            return JAVA_VERSION;
        }
    }

}
