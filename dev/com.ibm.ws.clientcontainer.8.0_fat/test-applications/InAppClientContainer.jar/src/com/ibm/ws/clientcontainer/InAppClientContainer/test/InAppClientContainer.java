/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.InAppClientContainer.test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class InAppClientContainer {

    public static void main(String[] args) throws NamingException {
        Context c = new InitialContext();
        Boolean inClientContainer = (Boolean) c.lookup("java:comp/InAppClientContainer");
        System.out.println("We are " + (inClientContainer ? "in the client container" : "not in the client container"));
    }

}
