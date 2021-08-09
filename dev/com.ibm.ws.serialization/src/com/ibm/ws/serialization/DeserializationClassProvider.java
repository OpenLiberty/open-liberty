/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization;

/**
 * A service interface for declaring classes and packages that can be
 * deserialized from the registering bundle.
 */
public final class DeserializationClassProvider {
    /**
     * A service property containing the class names that may be loaded for
     * deserialization from the registering bundle. Only a single bundle may
     * provide a class. The property value should be either a String or
     * String[] ("|"-delimited in bnd).
     */
    public static final String CLASSES_ATTRIBUTE = "classes";

    /**
     * A service property containing the package names that may be used to
     * load classes for deserialization from the registering bundle. Only a
     * single bundle may provide a package. The property value should either
     * be a String or String[] ("|"-delimited in bnd).
     */
    public static final String PACKAGES_ATTRIBUTE = "packages";
}
