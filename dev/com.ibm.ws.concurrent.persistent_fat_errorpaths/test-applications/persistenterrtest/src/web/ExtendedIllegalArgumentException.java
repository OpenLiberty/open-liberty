/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

/**
 * Tests can use this extension of IllegalArgumentException to force serialization/deserialization errors
 * by setting the "illegalArgument" attribute.
 */
public class ExtendedIllegalArgumentException extends IllegalArgumentException {
    private static final long serialVersionUID = 6253314052251772086L;

    private final Object illegalArgument;

    public ExtendedIllegalArgumentException(String message, Throwable cause, Object illegalArgument) {
        super(message, cause);
        this.illegalArgument = illegalArgument;
    }

    public Object getIllegalArgument() {
        return illegalArgument;
    }
}
