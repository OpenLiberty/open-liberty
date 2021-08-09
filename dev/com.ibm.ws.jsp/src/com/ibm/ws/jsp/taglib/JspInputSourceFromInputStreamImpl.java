/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.w3c.dom.Document;

import com.ibm.wsspi.jsp.resource.JspInputSource;

/**
 * A JspInputSourceInputStreamImpl is a JspInputSource that directly
 * wraps an InputStream (unlike the standard one which uses URLs to create
 * input stream). We play safe by complaining if the input stream is retrieved more than
 * once (i.e., we take ownership of the input stream on construction and then pass that onto
 * the first caller of getInputStream)
 */
public class JspInputSourceFromInputStreamImpl implements JspInputSource {

    private InputStream _is;
    private boolean _retrieved = false;

    public JspInputSourceFromInputStreamImpl(InputStream is) {
        _is = is;
    }

    /** {@inheritDoc} */
    @Override
    public URL getAbsoluteURL() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public URL getContextURL() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Document getDocument() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        if (_retrieved)
            throw new IOException("Already retrieved");
        _retrieved = true;
        InputStream answer = _is;
        _is = null;
        return answer;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified() {
        return 0; // We can't tell, so 0 is the right answer
    }

    /** {@inheritDoc} */
    @Override
    public String getRelativeURL() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isXmlDocument() {
        return false;
    }

}
