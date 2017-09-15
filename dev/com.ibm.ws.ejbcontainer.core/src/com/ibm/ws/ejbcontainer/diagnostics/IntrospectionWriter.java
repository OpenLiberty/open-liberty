/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.diagnostics;

/**
 * Common interface for writing introspection (dump) data using a variety of
 * different output resources; such as an OutputSream, Tr.dump, and IncidentStream.
 */
public interface IntrospectionWriter {
    /**
     * Begin a section of output.
     * 
     * @param title the section title, or null
     */
    public void begin(String title);

    /**
     * End a section of output.
     */
    public void end();

    /**
     * Prints a String and then terminates the line.
     * 
     * @param line the String value to be printed
     */
    public void println(String line);

    /**
     * Convenience method for dumping an array of introspection data. <p>
     * 
     * @param dumpData introspection data to be printed
     */
    public void dump(String[] dumpData);
}
