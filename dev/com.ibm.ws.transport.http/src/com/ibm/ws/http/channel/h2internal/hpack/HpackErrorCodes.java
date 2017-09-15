/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

public class HpackErrorCodes {

    /*
     * Table errors
     */
    static final int TABLE_INDEX_OUT_OF_RANGE = 1;
    static final int TABLE_SIZE_REQUEST_OUT_OF_ORDER = 2;
    static final int TABLE_SIZE_INCREASE_LARGER_THAN_MAX = 2;

    /*
     * Decoding errors
     */
    static final int PADDING_LONGER_THAN_7_BITS = 100;
    static final int PADDING_NOT_EOS = 101;
    static final int HUFFMAN_CANNOT_CONTAIN_EOS = 102;

}
