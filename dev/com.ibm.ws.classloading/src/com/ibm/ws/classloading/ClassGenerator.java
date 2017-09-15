/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

/**
 * This service can be registered to allow classes to be dynamically generated
 * for an application class loader.
 */
public interface ClassGenerator {
    /**
     * Dynamically generate a class that could not be found on the class path of
     * a class loader or by its parent class loader.
     *
     * @param name the name of the class that could not be found
     * @param loader the class loader that failed to load the class
     * @return null if the class cannot be dynamically generated
     * @throws ClassNotFoundException if an error occurred while generating the class;
     *             by convention, the message should be the class name, and the cause
     *             should contain details of the actual failure
     */
    byte[] generateClass(String name, ClassLoader loader) throws ClassNotFoundException;
}
