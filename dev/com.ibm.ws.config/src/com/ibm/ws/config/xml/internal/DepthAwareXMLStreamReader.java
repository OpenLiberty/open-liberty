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
package com.ibm.ws.config.xml.internal;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
@Trivial
public class DepthAwareXMLStreamReader extends StreamReaderDelegate {

    private int depth = 0;

    public DepthAwareXMLStreamReader(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public int next() throws XMLStreamException {
        final int event = super.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
            ++depth;
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            --depth;
        }
        return event;
    }

    @Override
    public int nextTag() throws XMLStreamException {
        final int event = super.nextTag();
        if (event == XMLStreamConstants.START_ELEMENT) {
            ++depth;
        } else if (event == XMLStreamConstants.END_ELEMENT) {
            --depth;
        }
        return event;
    }

    public boolean hasNext(int currentDepth) throws XMLStreamException {
        return super.hasNext() && depth >= currentDepth;
    }

    public int getDepth() {
        return depth;
    }
}
