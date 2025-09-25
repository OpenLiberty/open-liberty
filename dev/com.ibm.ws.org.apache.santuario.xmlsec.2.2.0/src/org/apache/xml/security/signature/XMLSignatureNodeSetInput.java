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
import java.util.Set;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.parser.XMLParserException;
import org.w3c.dom.Node;

/**
 * The XMLSignature Input wrapping a {@link Set} of {@link Node}s.
 */
public class XMLSignatureNodeSetInput extends XMLSignatureInput {

    /**
     * @param nodeSet a set of nodes to wrap
     */
    public XMLSignatureNodeSetInput(Set<Node> nodeSet) {
        super(nodeSet);
    }


    @Override
    public boolean hasUnprocessedInput() {
        return false;
    }


    @Override
    public InputStream getUnprocessedInput() {
        return null;
    }


    @Override
    protected Node convertToNode() throws XMLParserException, IOException {
        return null;
    }


    @Override
    public void write(OutputStream output, boolean c14n11) throws CanonicalizationException, IOException {
        if (output == getOutputStream()) {
            return;
        }
        canonicalize(output, c14n11);
    }
}
