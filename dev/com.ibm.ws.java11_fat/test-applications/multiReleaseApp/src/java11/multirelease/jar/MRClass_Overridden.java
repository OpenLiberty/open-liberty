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

public class MRClass_Overridden {

    // WARNING: If you need to modify this file, you must manually patch it into the corresponding locations in publish/servers/server_MultiReleaseJarTest/lib/multiRelease.jar

    // This class will be overridden by other classes with the same name under /META-INF/versions/N/java11/multirelease/jar

    public static int getJavaVersion() {
        return 8;
    }

    public class MRInnerClass_Overridden {

        public int getJavaVersion() {
            return 8;
        }

    }

    public static class MRStaticInnerClass_Overridden {

        public static int getJavaVersion() {
            return 8;
        }

    }

}
