/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.special.characters.servlet;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jaxws.special.characters.____.__or.WebServiceWithSpecialCharacters;
import com.ibm.ws.jaxws.special.characters.____.__or.WebServiceWithSpecialCharactersPortType;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SpecialCharacterTestServlet")
public class SpecialCharacterTestServlet extends FATServlet {
    
    /*
     *  Not having IllegalArgumentException is enough for this test to pass 
     */
    @Test
    public void SpecialCharacterSkipTest() throws Exception {
        WebServiceWithSpecialCharacters service = new WebServiceWithSpecialCharacters();
        WebServiceWithSpecialCharactersPortType port = service.getPort(WebServiceWithSpecialCharactersPortType.class);
        port.sc("Test");
    }
}
