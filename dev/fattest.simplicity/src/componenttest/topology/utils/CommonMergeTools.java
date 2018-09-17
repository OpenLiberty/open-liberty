/*******************************************************************************
 * Copyright (c) 2014,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package componenttest.topology.utils;

import java.io.File;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This tool's purpose is to merge config files under one file for easy debugging.
 */
public class CommonMergeTools {

    private static final Class<?> thisClass = CommonMergeTools.class;

    private String autoFVTDir = "";
    private String autoFVTServerRoot = "";

    private static final String ELEMENT_INCLUDE = "include";
    private static final String ELEMENT_FEATURE_MANAGER = "featureManager";
    private static final String ELEMENT_LOCATION = "location";

    private static final String COMMENT_BEGIN_INCLUDE = " begin include: ";
    private static final String COMMENT_END_INCLUDE = " end include:   ";

    private static final String XPATH_COMMENT_EXPRESSION = "//comment()[contains(.,'" + COMMENT_BEGIN_INCLUDE + "') or contains(.,'" + COMMENT_END_INCLUDE + "')]";
    private static final String XPATH_WHITESPACE_EXPRESSION = "//text()[normalize-space(.) = '']";

    /**
     * This method gathers all the "includes" elements inside of the XML file. And
     * merges all the files under one XML. This file is placed inside of the
     * same directory that the parameter 'serverXmlpath' is. If this method fails
     * for what ever reason it will return false.
     *
     * @param serverXmlPath
     *            The Location of the server.xml file to copy.
     * @param autoFVTPath
     *            The path to the "import" directory inside of the build.image
     *            folder... i.e. the server file Location
     * @return True if the merge was successful or false if the merge wasn't
     *         successful.
     *
     */
    public boolean mergeFile(String serverXmlPath, String autoFVTPath, String serverRootLoc) {

        String methodName = "mergeFile";
        autoFVTDir = autoFVTPath;
        autoFVTServerRoot = serverRootLoc;

        /*
         * Get XML document.
         */
        Document serverXML = this.retrieveDocument(serverXmlPath);
        if (serverXML == null) {
            Log.info(thisClass, methodName, "The XML file was empty or not parseable.");
            return false;
        }

        Element serverXMLRoot = serverXML.getDocumentElement();

        /*
         * Does the input file have any content?
         */
        NodeList childrenOfRoot = serverXMLRoot.getChildNodes();
        if (childrenOfRoot.getLength() <= 0) {
            Log.info(thisClass, methodName, "The XML file's root element had no children.");
            return false;
        }

        /*
         * Setup for the target document.
         */
        Document targetDoc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            targetDoc = db.newDocument();
            targetDoc.appendChild(targetDoc.createComment(" Original Server.xml: " + new File(serverXmlPath).getName() + " "));
        } catch (ParserConfigurationException e) {
            Log.error(thisClass, methodName, e, "Error generating new XML document.");
            return false;
        }

        /*
         * Copy the root element from the original document into the
         * target document.
         */
        Element targetRoot = (Element) targetDoc.importNode(serverXMLRoot, true);
        targetDoc.appendChild(targetRoot);

        /*
         * Are there any "include" elements to merge?
         */
        NodeList includeElements = targetRoot.getElementsByTagName(ELEMENT_INCLUDE);
        if (includeElements.getLength() <= 0) {
            Log.info(thisClass, methodName, "The XML file's root element has no include elements.");
            return false;
        }

        /*
         * Replace the "include" elements with the content from the files they point to.
         */
        replaceIncludeElements(includeElements, targetDoc, targetRoot);

        /*
         * Merge the "featureManager" elements into one "featureManager" element.
         */
        if (targetRoot.getElementsByTagName(ELEMENT_FEATURE_MANAGER).getLength() > 1) {
            mergeElement(targetRoot, targetDoc, ELEMENT_FEATURE_MANAGER);
        }

        /*
         * Write out the merged XML file.
         */
        trimWhitespace(targetDoc);
        addWhitespace(targetDoc);
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            String mergedXMLName = serverXmlPath.replace(".xml", "_Merged.xml");
            Log.info(thisClass, methodName, "Putting Merged file: " + mergedXMLName);

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(targetDoc);
            StreamResult result = new StreamResult(mergedXMLName);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            Log.error(thisClass, methodName, e, "Error writing merged XML document.");
            return false;
        }

        Log.info(thisClass, methodName, "MergeFile was successful");
        return true;
    }

    /**
     * Remove unnecessary text nodes / whitespace.
     *
     * @param targetDoc
     *            The target document.
     * @param targetRoot
     *            The parent node to remove all white space under.
     */
    private static void trimWhitespace(Document targetDoc) {
        try {
            targetDoc.getDocumentElement().normalize();
            XPathExpression xpath = XPathFactory.newInstance().newXPath().compile(XPATH_WHITESPACE_EXPRESSION);
            NodeList matchingNodes = (NodeList) xpath.evaluate(targetDoc, XPathConstants.NODESET);

            for (int idx = 0; idx < matchingNodes.getLength(); idx++) {
                matchingNodes.item(idx).getParentNode().removeChild(matchingNodes.item(idx));
            }
        } catch (XPathExpressionException e) {
            /*
             * This should never happen.
             */
            Log.error(thisClass, "trimWhitespace", e, "Error searching XML document for whitespace.");
        }
    }

    /**
     * Add whitespace to separate the generated "include" comments from the content from the include files
     * that replaced the include elements.
     *
     * @param targetDoc
     *            The target document to add the whitespace to.
     */
    private static void addWhitespace(Document targetDoc) {
        try {
            XPathExpression xpath = XPathFactory.newInstance().newXPath().compile(XPATH_COMMENT_EXPRESSION);
            NodeList matchingNodes = (NodeList) xpath.evaluate(targetDoc, XPathConstants.NODESET);

            Node text1 = targetDoc.createTextNode("\n\n    ");
            for (int idx = 0; idx < matchingNodes.getLength(); idx++) {
                Node matching = matchingNodes.item(idx);
                Node parent = matching.getParentNode();
                Node previousSibling = matching.getPreviousSibling();

                boolean previousNodeMatches = false;
                if (previousSibling != null) {
                    previousNodeMatches = previousSibling.isEqualNode(text1);
                }
                /**
                 * Do not add more whitespace if the previous node is whitespace we just added.
                 */
                if (previousNodeMatches) {
                    Node text = text1.cloneNode(false);
                    parent.replaceChild(text, matching);
                    parent.insertBefore(matching, text);
                } else {
                    Node text = text1.cloneNode(false);
                    parent.replaceChild(text, matching);
                    parent.insertBefore(matching, text);
                    parent.insertBefore(text.cloneNode(false), matching);
                }
            }
        } catch (XPathExpressionException e) {
            /*
             * This should never happen.
             */
            Log.error(thisClass, "trimWhitespace", e, "Error searching XML document for generated include file comments.");
        }
    }

    /**
     * This method goes through the list of includes and replaces the content of the include element with a comment and the
     * content of the file specified.
     *
     * @param includeElements
     *            List of Include elements
     * @param targetDoc
     *            The target document to add the included parts to.
     * @param newRoot
     *            The root to add the included parts to.
     */
    private void replaceIncludeElements(NodeList includeElements, Document targetDoc, Element newRoot) {
        String methodName = "replaceIncludeElements";

        /*
         * Keep iterating until we have replaced all the include elements.
         */
        while (includeElements != null && includeElements.getLength() > 0) {
            for (int idx = 0; idx < includeElements.getLength(); idx++) {
                /*
                 * Get the "include" element.
                 */
                Element includeElement = (Element) includeElements.item(idx);

                /*
                 * Resolve the path to the included file.
                 */
                String path = includeElement.getAttribute(ELEMENT_LOCATION);
                Node beginComment = targetDoc.createComment(COMMENT_BEGIN_INCLUDE + path + " ");
                Node endComment = targetDoc.createComment(COMMENT_END_INCLUDE + path + " ");

                /*
                 * If include is something like ../<filename>, prepend the servers dir if the file has ${shared.config.dir},
                 * replace that with the actual directory as the XML var won't resolve in java. May have to add other replacement
                 * code if we start including files from other locations
                 */
                if (Pattern.matches("^\\.\\..*", path)) {
                    path = autoFVTServerRoot + path;
                } else {
                    Pattern p = Pattern.compile("\\$\\{shared\\.config\\.dir\\}");
                    path = p.matcher(path).replaceAll(autoFVTDir);
                }
                Log.info(thisClass, methodName, "The Path to the updated include " + path);

                /*
                 * Replace the "include" element with the begin comment and then add the end comment.
                 */
                newRoot.replaceChild(beginComment, includeElement);

                /*
                 * Add the "end include:" comment.
                 */
                newRoot.insertBefore(endComment, beginComment.getNextSibling());

                /*
                 * Get the document for the included file and copy its content into the
                 * target document.
                 */
                Document includeDoc = retrieveDocument(path);
                if (includeDoc != null) {
                    Element includeRoot = includeDoc.getDocumentElement();
                    cloneChildContent(includeRoot, targetDoc, newRoot, endComment);
                } else {
                    Log.info(thisClass, methodName, "Failed to make Document for: " + path);
                    return;
                }
            }

            /*
             * Search for any include elements that were brought in from replacing the
             * previous include elements.
             */
            includeElements = newRoot.getElementsByTagName(ELEMENT_INCLUDE);
        }
    }

    /**
     * Creates a Document Object from the specified directory
     *
     * @param pathToDocument
     *            Is the Path to the file to convert
     * @return A Document file if it was successful or returns null
     */
    private Document retrieveDocument(String pathToDocument) {

        String methodName = "retrieveDocument";

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new File(pathToDocument));
        } catch (Exception e) {
            Log.error(thisClass, methodName, e, "Error parsing source XML document.");
        }

        return null;
    }

    /**
     * Will merge all the specified elements in the root under one element
     *
     * @param targetRoot
     *            The root element to search under.
     * @param targetDoc
     *            The target document to merge into.
     * @param elementName
     *            Name of element to merge.
     */
    private static void mergeElement(Element targetRoot, Document targetDoc, String elementName) {
        String methodName = "mergeElement";
        NodeList list = targetRoot.getElementsByTagName(elementName);
        if (list.getLength() <= 0) {
            Log.info(thisClass, methodName, "No " + elementName + " found");
            return;
        }

        /*
         * Iterate over all the elements and clone the contents to the first in the
         * list. Remove the old elements.
         */
        Element main = (Element) list.item(0);
        for (int idx = 1; idx < list.getLength(); idx++) {
            Node child = list.item(idx);
            cloneChildContent(child, targetDoc, main, null);
        }
        for (int idx = list.getLength() - 1; idx > 0; idx--) {
            targetRoot.removeChild(list.item(idx));
        }
    }

    /**
     * Clone the child nodes from the original node to the target node.
     *
     * @param originalNode
     *            The node to clone content from.
     * @param targetDoc
     *            The target document to copy to.
     * @param targetNode
     *            The node to copy the clones to.
     * @param insertBefore
     *            The node to insert the children before. If null, appends to the end of the list of children of 'targetNode'.
     */
    private static void cloneChildContent(Node originalNode, Document targetDoc, Node targetNode, Node insertBefore) {

        NodeList children = originalNode.getChildNodes();

        for (int idx = 0; idx < children.getLength(); idx++) {
            Node child = targetDoc.importNode(children.item(idx), true);
            targetNode.insertBefore(child, insertBefore);
        }
    }
}