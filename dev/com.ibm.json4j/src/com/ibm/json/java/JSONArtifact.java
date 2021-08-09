/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Interface class to define a set of generic apis both JSONObject and JSON array implement.
 * This is namely so that functions such as serialize, which are common between the two, can be easily
 * invoked.
 */
public interface JSONArtifact
{
    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(os,false);
     * Note that encoding is always written as UTF-8, as per JSON spec.
     * @param os The output stream to serialize data to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(OutputStream os) throws IOException;

    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(writer,false);
     * Note that encoding is always written as UTF-8, as per JSON spec.
     * @param os The output stream to serialize data to.
     * @param verbose Whether or not to write the JSON text in a verbose format.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(OutputStream os, boolean verbose) throws IOException;

    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(writer,false);
     * @param writer The writer which to serialize the JSON text to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(Writer writer) throws IOException;
    /**
     * Convert this object into a stream of JSON text, specifying verbosity.
     * @param writer The writer which to serialize the JSON text to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(Writer writer, boolean verbose) throws IOException;
    /**
     * Convert this object into a String of JSON text, specifying verbosity.
     * @param verbose Whether or not to serialize in compressed for formatted Strings.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public String serialize(boolean verbose) throws IOException;
    /**
     * Convert this object into a String of JSON text.  Same as serialize(false);
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public String serialize() throws IOException;
}
