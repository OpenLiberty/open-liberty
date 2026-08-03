/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.helpers;

import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;

public class HeaderEntry {

    private H2HeaderField headerField;
    private final HpackConstants.LiteralIndexType formatType;
    private final boolean huffman;

    public HeaderEntry(H2HeaderField headerField, HpackConstants.LiteralIndexType formatType, boolean huffman) {
        this.headerField = headerField;
        this.formatType = formatType;
        this.huffman = huffman;
    }

    public H2HeaderField getH2HeaderField() {
        return headerField;
    }

    // This is not thread safe. If attempting to change the header field, ensure that
    // the client has already finished using the header entry
    public void setH2HeaderField(H2HeaderField headerField) {
        this.headerField = headerField;
    }

    public HpackConstants.LiteralIndexType getFormatType() {
        return formatType;
    }

    public boolean isHuffman() {
        return huffman;
    }

}