/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.classloading;

import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * This interface allows a class to be transformed before it is loaded.
 * It is intended for use by JPA but is defined here to avoid creating
 * a dependency on any JPA packages.
 */
public interface ClassTransformer {
    /**
     * The following method is to be called before a class is defined.
     * 
     * @param name
     *            Name of the class being defined
     * @param bytes
     *            Byte code as loaded from disk
     * @param source
     *            Code source used to define the class.
     * @param loader
     *            Classloader to create the class from classByte.
     * 
     * @return The transformed byte code returned by the persistence provider. If no transformation
     *         takes place, the original classBytes is returned. All data of the returned byte[]
     *         MUST be used by the classloader to define the POJO entity class. I.e. returnClass =
     *         defineClass(name, classBytes, 0, classBytes.length, cs);
     */
    byte[] transformClass(String name, byte[] bytes, CodeSource source, ClassLoader loader);

    /**
     * Transform a class definition or redefinition.
     *
     * @param name name of the class being defined
     * @param classBeingRedefined class being redefined, or {@code null} for an initial definition
     * @param bytes bytecode as loaded from disk
     * @param protectionDomain protection domain used to define the class
     * @param loader class loader defining the class
     * @return transformed bytecode, or the original bytecode when no transformation occurs
     */
    default byte[] transformClass(String name,
                                  Class<?> classBeingRedefined,
                                  byte[] bytes,
                                  ProtectionDomain protectionDomain,
                                  ClassLoader loader) {
        return transformClass(name,
                              bytes,
                              protectionDomain == null ? null : protectionDomain.getCodeSource(),
                              loader);
    }
}
