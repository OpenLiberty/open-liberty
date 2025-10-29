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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.parser.XMLParserException;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Node;

/**
 * The XMLSignature Input as an input stream containing a collection of nodes
 * or a subnode excluding or not comments and excluding or not other nodes.
 * The stream is closed after processing.
 * <p>
 * NOTE: The stream may be closed in the process, but it is not guaranteed.
 */
public class XMLSignatureStreamInput extends XMLSignatureInput implements AutoCloseable {

    private InputStream inputStream;


    /**
     * Construct a XMLSignatureInput from an {@link InputStream}.
     * <p>
     * NOTE: The stream may be closed in the process, but it is not guaranteed.
     * </p>
     *
     * @param inputStream includes XML document or node
     */
    public XMLSignatureStreamInput(InputStream inputStream) {
        this.inputStream = inputStream;
    }


    @Override
    public boolean hasUnprocessedInput() {
        return inputStream != null;
    }


    @Override
    public InputStream getUnprocessedInput() {
        return inputStream;
    }

    @Override
    public void write(OutputStream output, boolean c14n11) throws CanonicalizationException, IOException {
        if (output == getOutputStream()) {
            return;
        }
        if (hasUnprocessedInput()) {
            byte[] buffer = new byte[8_192];
            int bytesread = 0;
            try {
                while ((bytesread = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesread);
                }
                output.flush();
                return;
            } finally {
                close();
            }
        }
        canonicalize(output, c14n11);
    }

    @Override
    protected Node convertToNode() throws XMLParserException, IOException {
        try (InputStream is = this.getUnprocessedInput()) {
            return XMLUtils.read(is, isSecureValidation());
        } finally {
            close();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.inputStream != null) {
            this.inputStream.close();
            this.inputStream = null;
        }
    }
}
