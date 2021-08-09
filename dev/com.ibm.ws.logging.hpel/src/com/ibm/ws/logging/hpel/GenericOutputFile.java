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
package com.ibm.ws.logging.hpel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Interface to have a customized way to create output stream into a file. For
 * example, if need to use other than default encoding. File instances which
 * need such capability need to implement that interface with methods matching
 * FileOutputStream constructors accepting File instances.
 */
public interface GenericOutputFile {
    /**
     * @see FileOutputStream#FileOutputStream(File)
     */
    FileOutputStream createOutputStream() throws IOException;
    
    /**
     * @see FileOutputStream#FileOutputStream(File, boolean)
     */
    FileOutputStream createOutputStream(boolean append) throws IOException;
}
