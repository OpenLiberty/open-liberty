/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test;

/**
 * This class and its member classes are to be loaded by a
 * separate {@link ClassLoader} in the unit tests.
 */
public class OuterClass {
    /**
     * This is a nested class
     */
    public static class NestedClass {}

    /**
     * This is a class that should fail to initialise.
     */
    public static class NestedClassUnloadable {
        static final int NUMBER = 1 / Integer.parseInt("0");
    }

    /**
     * This class should fail to load because its parent failed to initialise.
     */
    public static class NestedClassUnloadableChild extends NestedClassUnloadable {}

}
