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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.implementations.Canonicalizer11_OmitComments;
import org.apache.xml.security.c14n.implementations.Canonicalizer20010315OmitComments;
import org.apache.xml.security.c14n.implementations.CanonicalizerBase;
import org.apache.xml.security.parser.XMLParserException;
import org.apache.xml.security.utils.JavaUtils;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Node;

/**
 * The XMLSignature Input
 */
public abstract class XMLSignatureInput {

    /** The original set from the constructor. */
    private Set<Node> inputNodeSet;
    /** The original Element */
    private Node subNode;
    private boolean isNodeSet;

    /** Node Filter list. */
    private final List<NodeFilter> nodeFilters = new ArrayList<>();

    /** Exclude Node *for enveloped transformations */
    private Node excludeNode;
    private boolean excludeComments;

    private String sourceURI;
    private String mimeType;
    private boolean needsToBeExpanded;
    private boolean secureValidation = true;
    private OutputStream outputStream;

    /**
     * Construct a XMLSignatureInput
     */
    protected XMLSignatureInput() {
    }


    /**
     * Construct a XMLSignatureInput from a subtree rooted by rootNode. This
     * method included the node and <I>all</I> its descendants in the output.
     *
     * @param rootNode
     */
    protected XMLSignatureInput(Node rootNode) {
        this.subNode = rootNode;
    }


    /**
     * Construct a XMLSignatureInput from a {@link Set} of {@link Node}s.
     *
     * @param nodeSet
     */
    protected XMLSignatureInput(Set<Node> nodeSet) {
        this.inputNodeSet = nodeSet;
    }


    /**
     * @return true if this instance still can provide the unprocessed input
     *         which was specified as the parameter of {@link XMLSignatureInput}
     */
    public abstract boolean hasUnprocessedInput();

    /**
     * @return the {@link InputStream} from input which was specified as
     *         the parameter of {@link XMLSignatureInput} constructor
     * @throws IOException
     */
    public abstract InputStream getUnprocessedInput() throws IOException;

    /**
     * @return data given in constructor converted to a {@link Node} or null if such conversion is
     *         not supported by this {@link XMLSignatureInput}
     * @throws XMLParserException
     * @throws IOException
     */
    protected abstract Node convertToNode() throws XMLParserException, IOException;


    /**
     * Writes the data to the output stream.
     *
     * @param output
     * @throws CanonicalizationException
     * @throws IOException
     */
    public void write(OutputStream output) throws CanonicalizationException, IOException {
        write(output, false);
    }


    /**
     * Writes the data to the output stream.
     *
     * @param output
     * @param c14n11
     * @throws CanonicalizationException
     * @throws IOException
     * @see <a href="https://www.w3.org/TR/xmldsig-core/#sec-ReferenceGeneration">XmlDSig-Core
     *      Reference Generation</a>
     */
    public abstract void write(OutputStream output, boolean c14n11) throws CanonicalizationException, IOException;


    /**
     * Get the Input NodeSet.
     *
     * @return the Input NodeSet.
     */
    public Set<Node> getInputNodeSet() {
        return inputNodeSet;
    }


    /**
     * Returns the node set from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor
     * <p>
     * Can call the {@link #convertToNode()} to parse the {@link Node} from the input data.
     * The internal state will change then.
     *
     * @return the node set
     * @throws XMLParserException
     * @throws IOException
     */
    public Set<Node> getNodeSet() throws XMLParserException, IOException {
        return getNodeSet(false);
    }


    /**
     * Returns the node set from input which was specified as the parameter
     * of {@link XMLSignatureInput} constructor
     * <p>
     * Can call the {@link #convertToNode()} to parse the {@link Node} from the input data.
     * The internal state will change then.
     *
     * @param circumvent
     * @return the node set
     * @throws XMLParserException
     * @throws IOException
     */
    private Set<Node> getNodeSet(boolean circumvent) throws XMLParserException, IOException {
        if (inputNodeSet != null) {
            return inputNodeSet;
        }
        if (subNode != null) {
            if (circumvent) {
                XMLUtils.circumventBug2650(XMLUtils.getOwnerDocument(subNode));
            }
            inputNodeSet = new LinkedHashSet<>();
            XMLUtils.getSet(subNode, inputNodeSet, excludeNode, excludeComments);
            return inputNodeSet;
        } else if (hasUnprocessedInput()) {
            this.subNode = convertToNode();
            Set<Node> result = new LinkedHashSet<>();
            XMLUtils.getSet(subNode, result, null, false);
            return result;
        }

        throw new RuntimeException("getNodeSet() called but no input data present");
    }


    /**
     * Gets the node of this XMLSignatureInput
     *
     * @return The excludeNode set.
     */
    public Node getSubNode() {
        return subNode;
    }


    /**
     * @param filter
     * @throws XMLParserException
     * @throws IOException
     */
    public void addNodeFilter(NodeFilter filter) throws XMLParserException, IOException {
        if (hasUnprocessedInput()) {
            this.subNode = convertToNode();
        }
        nodeFilters.add(filter);
    }


    /**
     * @return the node filters
     */
    public final List<NodeFilter> getNodeFilters() {
        return nodeFilters;
    }


    /**
     * @param nodeSet
     */
    public final void setNodeSet(boolean nodeSet) {
        isNodeSet = nodeSet;
    }


    /**
     * Returns the byte array from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor OR tries to reconstruct that if
     * the element or node was already processed.
     *
     * @return the byte array
     * @throws CanonicalizationException
     * @throws IOException
     */
    public byte[] getBytes() throws IOException, CanonicalizationException {
        if (hasUnprocessedInput()) {
            return JavaUtils.getBytesFromStream(getUnprocessedInput());
        }
        if (isElement() || isNodeSet()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            canonicalize(baos, false);
            return baos.toByteArray();
        }
        return null;
    }


    /**
     * @return true if the {@link #XMLSignatureInput(Set)} was used or the node set was parsed from
     *         an input coming from another constructor.
     */
    public boolean isNodeSet() {
        return isNodeSet || inputNodeSet != null;
    }


    /**
     * Determines if the object has been set up with an Element
     *
     * @return true if the object has been set up with an Element
     */
    public boolean isElement() {
        return subNode != null && inputNodeSet == null && !isNodeSet;
    }


    /**
     * @return String given through constructor. Null by default, see extensions of this class.
     */
    public String getPreCalculatedDigest() {
        return null;
    }


    /**
     * @return the exclude node of this XMLSignatureInput
     */
    public Node getExcludeNode() {
        return excludeNode;
    }


    /**
     * Sets the exclude node of this XMLSignatureInput
     *
     * @param excludeNode The excludeNode to set.
     */
    public void setExcludeNode(Node excludeNode) {
        this.excludeNode = excludeNode;
    }


    /**
     * @return Returns the excludeComments.
     */
    public boolean isExcludeComments() {
        return excludeComments;
    }


    /**
     * @param excludeComments The excludeComments to set.
     */
    public void setExcludeComments(boolean excludeComments) {
        this.excludeComments = excludeComments;
    }


    /**
     * @return Source URI
     */
    public String getSourceURI() {
        return sourceURI;
    }


    /**
     * @param sourceURI
     */
    public void setSourceURI(String sourceURI) {
        this.sourceURI = sourceURI;
    }


    /**
     * Some Transforms may require explicit MIME type, charset (IANA registered "character set"),
     * or other such information concerning the data they are receiving from an earlier Transform
     * or the source data, although no Transform algorithm specified in this document needs such
     * explicit information.
     * <p>
     * Such data characteristics are provided as parameters to the Transform algorithm and should be
     * described in the specification for the algorithm.
     *
     * @return mimeType
     */
    public String getMIMEType() {
        return mimeType;
    }


    /**
     * Some Transforms may require explicit MIME type, charset (IANA registered "character set"),
     * or other such information concerning the data they are receiving from an earlier Transform
     * or the source data, although no Transform algorithm specified in this document needs such
     * explicit information.
     * <p>
     * Such data characteristics are provided as parameters to the Transform algorithm and should be
     * described in the specification for the algorithm.
     *
     * @param mimeType
     */
    public void setMIMEType(String mimeType) {
        this.mimeType = mimeType;
    }


    /**
     * @return true if the structure needs to be expanded.
     */
    public boolean isNeedsToBeExpanded() {
        return needsToBeExpanded;
    }


    /**
     * Set if the structure needs to be expanded.
     *
     * @param needsToBeExpanded true if so.
     */
    public void setNeedsToBeExpanded(boolean needsToBeExpanded) {
        this.needsToBeExpanded = needsToBeExpanded;
    }


    /**
     * @return true by default, enabled validation in r/w operations
     */
    public boolean isSecureValidation() {
        return secureValidation;
    }


    /**
     * Set to false to disable validation in r/w operations.
     *
     * @param secureValidation default is true.
     */
    public void setSecureValidation(boolean secureValidation) {
        this.secureValidation = secureValidation;
    }


    /**
     * @return true if {@link #setOutputStream} has been called with a non-null OutputStream
     */
    public boolean isOutputStreamSet() {
        return outputStream != null;
    }


    /**
     * @param outputStream this stream will be ignored in {@link #write(OutputStream)} method
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }


    /**
     * @return {@link OutputStream} set in {@link #setOutputStream(OutputStream)}
     */
    public OutputStream getOutputStream() {
        return this.outputStream;
    }


    /**
     * Creates a short description of this instance.
     */
    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        if (isNodeSet()) {
            return className + "/NodeSet/" + inputNodeSet.size() + " nodes/" + getSourceURI();
        }
        if (isElement()) {
            return className + "/Element/" + subNode + " exclude " + excludeNode + " comments:" + excludeComments
                + "/" + getSourceURI();
        }
        if (hasUnprocessedInput()) {
            try {
                int available = getUnprocessedInput().available();
                return className + "/OctetStream/" + available + " bytes/" + getSourceURI();
            } catch (IOException ex) {
                // Stream is unavailable and toString should not touch any IO, so ignore it.
            }
        }
        return className + "/OctetStream//" + getSourceURI();
    }


    /**
     * Canonicalizes this object to the output stream.
     *
     * @param c14n11
     * @param output
     * @throws CanonicalizationException
     * @throws IOException
     */
    protected void canonicalize(OutputStream output, boolean c14n11) throws CanonicalizationException, IOException {
        final CanonicalizerBase c14nizer;
        if (c14n11) {
            c14nizer = new Canonicalizer11_OmitComments();
        } else {
            c14nizer = new Canonicalizer20010315OmitComments();
        }
        c14nizer.engineCanonicalize(this, output, isSecureValidation());
        output.flush();
    }
}
