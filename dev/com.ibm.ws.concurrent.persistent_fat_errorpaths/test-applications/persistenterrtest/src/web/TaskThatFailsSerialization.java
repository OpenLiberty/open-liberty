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
 * Task that declares itself serializable but fails to serialize.
 */
public class TaskThatFailsSerialization implements Runnable, Serializable {
    private static final long serialVersionUID = -561219328209548326L;

    @Override
    public void run() {}

    @Override
    public String toString() {
        return "This is a task that says it is serializable but intentionally fails serialization";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Intentionally failing serialization");
    }
}
