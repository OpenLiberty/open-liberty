/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.lang.reflect.Field;

/**
 * Wrapper around {@code ClassValue<Field>} that returns a declared field that
 * has been made accessible from the specified class. This abstraction exists
 * for portability to Java 6, which does not have {@code ClassValue}.
 */
public interface FieldClassValue {
    /**
     * Returns a specific declared field from the class.
     *
     * @param klass the class that must contain the field
     * @return the declared field
     * @throws IllegalStateException if the class does not contain the field
     */
    Field get(Class<?> klass);
}
