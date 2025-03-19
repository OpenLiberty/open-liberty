/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.persistence;

import java.io.IOException;
import java.io.Writer;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Writes text to a DDL file for use with the persistence service.
 *
 * This implementation works with the persistence service to ignore attempts by
 * some JPA providers to close the writer, so that the DDL for multiple persistence
 * units may be written to the same file. This also allows the persistence service
 * to append a final exit command to the file, as required for some databases.
 */
public class DDLGenerationWriter extends Writer {
    private final Writer _out;
    private String _exitCmd;

    @Trivial
    public DDLGenerationWriter(Writer out) {
        _out = out;
    }

    @Trivial
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        _out.write(cbuf, off, len);
    }

    @Trivial
    @Override
    public void flush() throws IOException {
        _out.flush();
    }

    @Trivial
    @Override
    public void close() throws IOException {
        // Only closed by writeExitAndClose()
    }

    /**
     * Sets the exit command that is to be written to the stream before closing.
     * Overrides any previously set exit command; only one exit command is written to the stream.
     *
     * @param command the final exit command to be added before closing
     * @throws IOException if an I/O error occurs
     */
    public void setExitCommand(String command) throws IOException {
        _exitCmd = command;
    }

    /**
     * Closes the stream, writing the exit command and flushing first.
     * @throws IOException if an I/O error occurs
     */
    public void writeExitAndClose() throws IOException  {
        if (_exitCmd != null) {
            _out.write(_exitCmd);
            _exitCmd = null;
        }
        _out.close();
    }
}
