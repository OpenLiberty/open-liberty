/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.helpers;

import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;

public class HeaderEntry {

    private final H2HeaderField headerField;
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

    public HpackConstants.LiteralIndexType getFormatType() {
        return formatType;
    }

    public boolean isHuffman() {
        return huffman;
    }

}