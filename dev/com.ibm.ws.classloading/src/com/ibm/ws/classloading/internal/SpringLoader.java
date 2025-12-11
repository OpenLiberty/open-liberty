/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.classloading.internal;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Declare the methods expected by Spring's ReflectiveLoadTimeWeaver.
 * This interface is only used internally to document the methods that
 * Spring needs for class weaving.
 */
interface SpringLoader {
    boolean addTransformer(ClassFileTransformer cft);

    ClassLoader getThrowawayClassLoader();

    // This is needed by org.springframework.cglib.core.ReflectUtils which will reflectively call this if available
    Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain);
}
