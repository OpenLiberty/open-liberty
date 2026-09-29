/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json.stream;

/**
 * Provides the location information of a JSON event in an input source. The
 * {@code JsonLocation} information can be used to identify incorrect JSON
 * or can be used by higher frameworks to know about the processing location.
 *
 * <p>All the information provided by a {@code JsonLocation} is optional. For
 * example, a provider may only report line numbers. Also, there may not be any
 * location information for an input source. For example, if a
 * {@code JsonParser} is created using
 * {@link jakarta.json.JsonArray JsonArray} input source, all the methods in
 * this class return -1.
 * @see JsonParser
 * @see JsonParsingException
 */
public interface JsonLocation {

    /**
     * Return the line number (starts with 1 for the first line) for the current JSON event in the input source.
     *
     * @return the line number (starts with 1 for the first line) or -1 if none is available
     */
    long getLineNumber();

    /**
     * Return the column number (starts with 1 for the first column) for the current JSON event in the input source.
     *
     * @return the column number (starts with 1 for the first column) or -1 if none is available
     */
    long getColumnNumber();

    /**
     * Return the stream offset into the input source this location
     * is pointing to. If the input source is a file or a byte stream then
     * this is the byte offset into that stream, but if the input source is
     * a character media then the offset is the character offset.
     * Returns -1 if there is no offset available.
     *
     * @return the offset of input source stream, or -1 if there is
     * no offset available
     */
    long getStreamOffset();

}
