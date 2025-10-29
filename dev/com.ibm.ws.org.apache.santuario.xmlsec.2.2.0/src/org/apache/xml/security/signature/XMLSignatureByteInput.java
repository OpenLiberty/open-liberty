/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.signature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.parser.XMLParserException;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Node;

/**
 * The XMLSignature Input as a byte array containing a collection of nodes
 * or a subnode excluding or not comments and excluding or not other nodes.
 */
public class XMLSignatureByteInput extends XMLSignatureInput {

    private byte[] bytes;

    /**
     * Construct a XMLSignatureInput from an array.
     * <p>
     * The {@link #getUnprocessedInput()} method will provide {@link ByteArrayOutputStream} based
     * on the input.
     * <p>
     * NOTE: no defensive copy - the input is directly set to the object
     * </p>
     *
     * @param input a byte array which includes XML document or node. Can be null.
     */
    public XMLSignatureByteInput(byte[] input) {
        this.bytes = input;
    }


    @Override
    public boolean hasUnprocessedInput() {
        return bytes != null;
    }


    @Override
    public InputStream getUnprocessedInput() {
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }


    @Override
    public void write(OutputStream outputStream, boolean c14n11) throws CanonicalizationException, IOException {
        if (outputStream == getOutputStream()) {
            return;
        }
        if (bytes == null) {
            canonicalize(outputStream, c14n11);
            return;
        }
        outputStream.write(bytes);
        outputStream.flush();
    }


    @Override
    protected Node convertToNode() throws XMLParserException, IOException {
        try {
            return XMLUtils.read(this.getUnprocessedInput(), isSecureValidation());
        } finally {
            this.bytes = null;
        }
    }
}
