/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class IOUtils {

    /**
     * Copy the given InputStream to the given OutputStream.
     * 
     * Note: the InputStream is closed when the copy is complete.  The OutputStream 
     *       is left open.
     */
    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte buffer[] = new byte[2048];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead);
        }
        from.close();
    }
    
    /**
     * Copy the given Reader to the given Writer.
     * 
     * This method is basically the same as copyStream; however Reader and Writer
     * objects are cognizant of character encoding, whereas InputStream and OutputStreams
     * objects deal only with bytes.
     * 
     * Note: the Reader is closed when the copy is complete.  The Writer 
     *       is left open.  The Write is flushed when the copy is complete.
     */
    public static void copyReader(Reader from, Writer to) throws IOException {
        
        char buffer[] = new char[2048];
        int charsRead;
        while ((charsRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, charsRead);
        }
        from.close();
        to.flush();
    }

}
