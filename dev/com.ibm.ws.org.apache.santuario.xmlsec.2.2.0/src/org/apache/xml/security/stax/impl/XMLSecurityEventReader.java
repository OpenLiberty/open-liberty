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

package org.apache.xml.security.stax.impl;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.xml.security.stax.ext.stax.XMLSecEvent;

/**
 */
public class XMLSecurityEventReader implements XMLEventReader, AutoCloseable {

    private final Iterator<XMLSecEvent> xmlSecEventIterator;
    private XMLEvent xmlSecEvent;

    public XMLSecurityEventReader(Deque<XMLSecEvent> xmlSecEvents, int fromIndex) {
        this.xmlSecEventIterator = xmlSecEvents.descendingIterator();
        int curIdx = 0;
        while (curIdx++ < fromIndex) {
            this.xmlSecEventIterator.next();
        }
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        if (this.xmlSecEvent != null) {
            final XMLEvent currentXMLEvent = this.xmlSecEvent;
            this.xmlSecEvent = null;
            return currentXMLEvent;
        }
        final XMLEvent currentXMLEvent;
        try {
            currentXMLEvent = xmlSecEventIterator.next();
        } catch (NoSuchElementException e) {
            throw new XMLStreamException(e);
        }
        return currentXMLEvent;
    }

    @Override
    public boolean hasNext() {
        if (this.xmlSecEvent != null) {
            return true;
        }
        return xmlSecEventIterator.hasNext();
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        if (this.xmlSecEvent != null) {
            return this.xmlSecEvent;
        }
        try {
            this.xmlSecEvent = xmlSecEventIterator.next();
            return this.xmlSecEvent;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        //ATM not needed and therefore not implemented
        throw new XMLStreamException(new UnsupportedOperationException());
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        //ATM not needed and therefore not implemented
        throw new XMLStreamException(new UnsupportedOperationException());
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        //ATM not needed and therefore not implemented
        throw new IllegalArgumentException(new UnsupportedOperationException());
    }

    @Override
    public void close() throws XMLStreamException {
        //nop
    }

    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
