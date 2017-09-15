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
package com.ibm.ws.fat.jcdi.util.html;

import java.nio.CharBuffer;

/**
 * The Default html SAX-like event handler interface.
 * 
 * @author yingwang
 * 
 */
public interface HtmlDefaultEventHandler {

    /**
     * start of the html/xhtml document.
     */
    public void startDoc();

    /**
     * handle the dtd.
     * 
     * @param buffer
     * @return
     */
    public CharBuffer handleDTD(CharBuffer buffer);

    /**
     * handle the tag.
     * 
     * @param buffer
     * @return
     */
    public CharBuffer handleTag(CharBuffer buffer);

    /**
     * handle the contents.
     * 
     * @param buffer
     * @return
     */
    public CharBuffer handleContents(CharBuffer buffer);

    /**
     * handle the comments.
     * 
     * @param buffer
     * @return
     */
    public CharBuffer handleComments(CharBuffer buffer);

    /**
     * end of the document.
     * 
     * @param buffer
     * @return
     */
    public void endDoc();

    /**
     * flush the final result.
     * 
     * @return
     */
    public CharBuffer[] flush();

    /**
     * reset the event handler.
     * 
     * @return
     */
    public void reset();

    /**
     * Whether or not the handler already finish parsing. So the parser could skip
     * the rest of the contents.
     * 
     * @return
     */
    public boolean isFinished();
}
