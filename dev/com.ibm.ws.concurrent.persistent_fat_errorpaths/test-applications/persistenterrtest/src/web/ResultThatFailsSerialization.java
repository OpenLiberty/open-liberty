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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A task result that declares itself serializable but intentionally fails serialization
 */
public class ResultThatFailsSerialization implements Serializable {
    private static final long serialVersionUID = 8804435909129645389L;

    private final transient Throwable cause;
    private final transient Object illegalArg;

    public ResultThatFailsSerialization() {
        cause = null;
        illegalArg = null;
    }

    public ResultThatFailsSerialization(Object illegalArg, Throwable cause) {
        this.cause = cause;
        this.illegalArg = illegalArg;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (illegalArg == null)
            throw new NotSerializableException("Intentionally failing serialization");
        else
            throw new ExtendedIllegalArgumentException("Intentionally caused failure for testing purposes", cause, illegalArg);
    }
}
