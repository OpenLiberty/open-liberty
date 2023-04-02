/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package test.corba.web.war;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;

@Stateful
@LocalBean
public class MyIIOPClientBean extends MyIIOPClientServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public ORB getORB() throws NamingException {
        return ((ORB) new InitialContext().lookup("java:comp/ORB"));
    }
}
