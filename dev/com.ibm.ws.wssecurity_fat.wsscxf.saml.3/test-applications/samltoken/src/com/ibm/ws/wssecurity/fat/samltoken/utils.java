/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.samltoken;
import java.util.Iterator;

import javax.xml.soap.Node;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;

import org.w3c.dom.NodeList;

public class utils {

    public static Boolean isSAMLAssertionInHeader(SOAPMessage request) {

        Boolean samlAssertionFound = false ; 
        try {
            for (Iterator<Node> i = request.getSOAPHeader().getChildElements() ; i.hasNext();) {
                Node n = i.next();
                System.out.println("this node: " + n.getNodeName() ) ;
                if (n.getNodeName().equals("wsse:Security")) {
                    NodeList nl = n.getChildNodes();
                    for (int j=0 ; j < nl.getLength(); j++) {
                        org.w3c.dom.Node cn = nl.item(j) ;
                        String nodeName = cn.getNodeName() ;
                        System.out.println("child node: " + nodeName) ;
                        if (nodeName.contains("saml") && nodeName.contains(":Assertion")) {  // match saml2:Assertion or saml:Assertion
                            samlAssertionFound = true ;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return samlAssertionFound ; 
    }
}