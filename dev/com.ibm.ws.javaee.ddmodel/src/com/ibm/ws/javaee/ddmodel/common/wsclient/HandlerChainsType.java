/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common.wsclient;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 * <xsd:complexType name="handler-chainsType">
 * <xsd:sequence>
 * <xsd:element name="handler-chain"
 * type="javaee:handler-chainType"
 * minOccurs="0"
 * maxOccurs="unbounded"/>
 * </xsd:sequence>
 * <xsd:attribute name="id"
 * type="xsd:ID"/>
 * </xsd:complexType>
 */
public class HandlerChainsType extends DDParser.ElementContentParsable {
    // elements
    HandlerChainType.ListType handler_chain;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("handler-chain".equals(localName)) {
            HandlerChainType handler_chain = new HandlerChainType();
            parser.parse(handler_chain);
            addHandlerChain(handler_chain);
            return true;
        }
        return false;
    }

    private void addHandlerChain(HandlerChainType handler_chain) {
        if (this.handler_chain == null) {
            this.handler_chain = new HandlerChainType.ListType();
        }
        this.handler_chain.add(handler_chain);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("handler-chain", handler_chain);
    }

    public List<HandlerChain> getList() {
        if (handler_chain != null) {
            return handler_chain.getList();
        } else {
            return Collections.emptyList();
        }
    }
}
