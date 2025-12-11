/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.passivation;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet("/*")
@SuppressWarnings("serial")
public class PassivationCapableTestServlet extends FATServlet {
    public void testPassivationCapable() throws Exception {
        InitialContext c = new InitialContext();
        c.lookup("java:module/PassivationCapableBean");
        c.lookup("java:module/PassivationCapableAnnoTrueBean");
        c.lookup("java:module/PassivationCapableAnnoFalseBean");
        c.lookup("java:module/PassivationCapableXMLTrueBean");
        c.lookup("java:module/PassivationCapableXMLFalseBean");
        c.lookup("java:module/PassivationCapableXMLFalseLoadStrategyOnceBean");
        c.lookup("java:module/PassivationCapableXMLFalseLoadStrategyTransactionBean");
        c.lookup("java:module/PassivationCapableXMLFalseLoadStrategyActivitySessionBean");
    }
}
