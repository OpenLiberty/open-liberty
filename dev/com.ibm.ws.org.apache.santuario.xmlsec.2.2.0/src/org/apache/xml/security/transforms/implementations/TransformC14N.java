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
package org.apache.xml.security.transforms.implementations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.implementations.Canonicalizer20010315;
import org.apache.xml.security.c14n.implementations.Canonicalizer20010315OmitComments;
import org.apache.xml.security.signature.XMLSignatureByteInput;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.transforms.TransformSpi;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Element;

/**
 * Implements the <CODE>http://www.w3.org/TR/2001/REC-xml-c14n-20010315</CODE>
 * transform.
 *
 */
public class TransformC14N extends TransformSpi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream os, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws CanonicalizationException {

        Canonicalizer20010315 c14n = getCanonicalizer();

        if (os == null && (input.hasUnprocessedInput() || input.isElement() || input.isNodeSet())) {
            try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
                c14n.engineCanonicalize(input, writer, secureValidation);
                writer.flush();
                XMLSignatureInput output = new XMLSignatureByteInput(writer.toByteArray());
                output.setSecureValidation(secureValidation);
                return output;
            } catch (IOException ex) {
                throw new CanonicalizationException("empty", new Object[] {ex.getMessage()});
            }
        }
        c14n.engineCanonicalize(input, os, secureValidation);
        XMLSignatureInput output = new XMLSignatureByteInput(null);
        output.setSecureValidation(secureValidation);
        output.setOutputStream(os);
        return output;
    }

    protected Canonicalizer20010315 getCanonicalizer() {
        return new Canonicalizer20010315OmitComments();
    }
}
