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
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Delegate on z/OS that makes sure text files are created with file system tags indicating
 * the files contain text that is in an encoding other than EBCDIC.
 * <p>
 * Package protected class. This is a delegate of the public factory
 */
class TaggedFileOutputStreamFactory implements TextFileOutputStreamFactory.Delegate {

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(File file) throws IOException {
        return new TaggedFileOutputStream(file, false);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(File file, boolean append) throws IOException {
        return new TaggedFileOutputStream(file, append);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(String name) throws IOException {
        return new TaggedFileOutputStream(new File(name), false);
    }

    /** {@inheritDoc} */
    @Override
    public FileOutputStream createOutputStream(String name, boolean append) throws IOException {
        return new TaggedFileOutputStream(new File(name), append);
    }

}
