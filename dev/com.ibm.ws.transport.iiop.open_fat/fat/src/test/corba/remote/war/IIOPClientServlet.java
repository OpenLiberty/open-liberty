/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.corba.remote.war;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.annotation.WebServlet;

import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.IIOPClientTestLogic;

@WebServlet("/IIOPClientServlet")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class IIOPClientServlet extends FATServlet implements IIOPClientTestLogic {
    private static final long serialVersionUID = 1L;

    @Resource
    private ORB orb;
    public ORB getOrb() {return orb;}
}
