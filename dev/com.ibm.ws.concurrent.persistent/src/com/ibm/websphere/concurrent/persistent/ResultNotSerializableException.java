/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
package com.ibm.websphere.concurrent.persistent;

/**
 * Thrown when an attempt is made to access a task result that does not implement
 * the <code>Serializable</code> interface.
 * The argument should be the name of the class.
 */
public class ResultNotSerializableException extends RuntimeException {
    private static final long serialVersionUID = 4860312564481081289L;

    /**
     * Constructs a ResultNotSerializableException for the specified class name.
     * 
     * @param className Class of the instance being serialized/deserialized.
     */
    public ResultNotSerializableException(String className) {
        super(className);
    }
}