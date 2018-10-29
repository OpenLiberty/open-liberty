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
package com.ibm.ws.kernel.boot.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * A basic implementation of a factory for FileOutputStream that defers creation
 * to the base FileOutputStream constructor.
 * <p>
 * Package protected class. This is a delegate of the public factory
 */
class DefaultFileStreamFactory implements TextFileOutputStreamFactory.Delegate {
    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(File file, boolean append) throws FileNotFoundException {
        return new FileOutputStream(file, append);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(String name) throws FileNotFoundException {
        return new FileOutputStream(name);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(String name, boolean append) throws FileNotFoundException {
        return new FileOutputStream(name, append);
    }
}
