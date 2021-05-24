/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.userSAXParserFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class MySAXParserFactory extends SAXParserFactory {

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        throw fail();
    }

    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        throw fail();
    }

    @Override
    public boolean getFeature(String name) throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
        throw fail();
    }

    private ParserConfigurationException fail() {
        System.out.println("FAILED - User's ParserFactory was invoked");
        ParserConfigurationException ex = new ParserConfigurationException("FAILED - User's ParserFactory was invoked");
        ex.printStackTrace();
        return ex;
    }
}
