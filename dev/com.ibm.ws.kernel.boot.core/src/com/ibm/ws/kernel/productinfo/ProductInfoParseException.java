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
package com.ibm.ws.kernel.productinfo;

import java.io.File;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

@SuppressWarnings("serial")
public class ProductInfoParseException extends Exception {
    private final File file;
    private final String missingKey;

    ProductInfoParseException(File file, Throwable t) {
        super("failed to read " + file, t);
        this.file = file;
        this.missingKey = null;
    }

    ProductInfoParseException(File file, String missingKey) {
        super("missing key " + missingKey + " in " + file);
        this.file = file;
        this.missingKey = missingKey;
    }

    public File getFile() {
        return file;
    }

    public String getMissingKey() {
        return missingKey;
    }

    private void writeObject(ObjectOutputStream oos) throws NotSerializableException {
        throw new NotSerializableException();
    }
}
