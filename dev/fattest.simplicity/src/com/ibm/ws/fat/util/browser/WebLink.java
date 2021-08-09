/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.browser;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Convenience class for HTML anchor elements
 * 
 * @author Tim Burns
 */
public class WebLink {

    protected final WebResponse response;
    protected final int index;
    protected final String name;
    protected final String text;
    protected final String id;
    protected final String href;
    protected final String url;

    /**
     * Primary constructor
     * 
     * @param response
     *            the WebResponse where this hyperlink is located. Must not be
     *            null.
     * @param anchor
     *            The anchor element for this hyperlink found on the specified
     *            response
     * @throws IllegalArgumentException
     *             if the specified WebResponse is null
     * @throws WebBrowserException
     *             if a relative href can't be resolved
     */
    protected WebLink(WebResponse response, Element anchor, int index) throws IllegalArgumentException, WebBrowserException {
        if (response == null) {
            throw new IllegalArgumentException("Unable to construct a " + this.getClass().getName() + " instance because the specified input argument is null: "
                                               + WebResponse.class.getName());
        }
        this.response = response;
        this.index = index;
        this.name = (anchor == null) ? null : anchor.getAttribute("name");
        this.href = (anchor == null) ? null : anchor.getAttribute("href");
        this.url = (this.href == null || this.href.trim().length() == 0) ? null : this.response.resolveRelativeUrl(this.href);
        this.text = this.getText(anchor);
        this.id = this.getId(anchor);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(this.response.toString());
        s.append(" Anchor Index ");
        s.append(this.index);
        return s.toString();
    }

    /**
     * Determines the ID of the specified anchor element by locating an
     * attribute with the name "id".<br>
     * <br>
     * I wanted to use Attr.isId to determine if an attribute is of type ID;
     * note that attributes with the name "ID" or "id" are not of type ID unless
     * so defined (for compatibility with document.getElementById(id)).<br>
     * 
     * However, for some reason, calling attr.isId() result in
     * "java.lang.AbstractMethodError: org/w3c/dom/Attr.isId()" when using an
     * IBM JRE. That seems wrong to me, but whatever.
     * 
     * @param element
     *            the element whose ID you want to determine
     * @return the ID for the input element, or null if no ID can be found
     */
    protected String getId(Element element) {
        if (element == null) {
            return null;
        }
        NamedNodeMap map = element.getAttributes();
        if (map == null) {
            return null;
        }
        int numAttrs = map.getLength();
        for (int i = 0; i < numAttrs; i++) {
            Attr attr = (Attr) map.item(i);
            //			if(attr.isId()) { // this doesn't appear to work, but I don't know why
            if ("id".equals(attr.getNodeName())) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Retrieve the text found inside the specified Element.<br>
     * <br>
     * For some reason, calling element.getTextContent() throws
     * "java.lang.AbstractMethodError: org/w3c/dom/Node.getTextContent()" when
     * using an IBM JRE. That seems wrong to me, but whatever.
     * 
     * @param element
     *            The element whose text you want to examine
     * @return the text inside the specified Element, or null if no text is
     *         found
     */
    protected String getText(Element element) {
        if (element == null) {
            return null;
        }
        //return element.getTextContent(); // this doesn't appear to work, but I don't know why
        NodeList children = element.getChildNodes();
        int numChildren = children.getLength();
        for (int i = 0; i < numChildren; i++) {
            Node child = children.item(i);
            if (Node.TEXT_NODE == child.getNodeType()) {
                return child.getNodeValue();
            }
        }
        return null;
    }

    /**
     * Returns the value of the "name" attribute for this anchor element
     * 
     * @return the name of this anchor, or null if no name is defined
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the value of text found inside this anchor element
     * 
     * @return the value of text found inside this anchor, or null if no text is
     *         defined
     */
    public String getText() {
        return this.text;
    }

    /**
     * Returns the ID of this anchor element (Attr.isId is used to determine if
     * an attribute is of type ID; note that attributes with the name "ID" or
     * "id" are not of type ID unless so defined)
     * 
     * @return the ID of this anchor, or null if no ID is defined
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the value of the "href" attribute for this anchor element. Note
     * that this value may be path-relative
     * 
     * @return the href of this anchor, or null if no href is defined
     */
    public String getHref() {
        return this.href;
    }

    /**
     * Gets the target of this anchor element by resolving the relative path
     * defined by the "href" attribute. (If the "href" attribute does not define
     * a relative path, then the value of the "href" attribute is returned).
     * 
     * @return the target of this anchor element
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Gets the index of this anchor element on the page.
     * 
     * @return the index of this anchor element on the page
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Makes an HTTP request for the resource defined by this anchor element.
     * 
     * @return the response from the server from clicking on this hyperlink
     * @throws WebBrowserException
     *             if any problem occurs
     */
    public WebResponse click() throws WebBrowserException {
        if (this.url == null) {
            throw new WebBrowserException("Failed to click on " + this.toString() + " because the 'href' attribute is either null or empty.");
        }
        return this.response.getBrowser().request(this.url);
    }

}
