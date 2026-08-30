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
package com.ibm.ws.zos.core.utils;

import java.nio.ByteBuffer;

/**
 * Helper to format byte[]/ByteBuffer in a double gutter format
 */
public interface DoubleGutter {

    /**
     * Format a byte array into a double-gutter hex dump that's more
     * suitable for human consumption. This format closely resembles
     * the one used in traditional WAS for z/OS.
     *
     * @param address the address of the buffer
     * @param data    the contents of the area to be traced
     * @return String representing formatted byte[] with ASCII/EBCDIC Gutters
     */
    public String asDoubleGutter(long address, byte[] data);

    /**
     * Format the given ByteBuffer into a double-gutter hex dump.
     *
     * Note: only the data between ByteBuffer.position() and ByteBuffer.limit() is formatted.
     *
     * @param address the address of the buffer data
     * @param data    the data to be formatted.
     *
     * @return The data formatted with ASCII/EBCDIC gutters.
     */
    public String asDoubleGutter(long address, ByteBuffer data);
}
