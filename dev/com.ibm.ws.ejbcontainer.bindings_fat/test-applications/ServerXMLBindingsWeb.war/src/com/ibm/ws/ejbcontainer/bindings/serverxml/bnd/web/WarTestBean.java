/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.bindings.serverxml.bnd.web;

import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

@Local(WarLocalBusiness.class)
@Remote(WarRemoteBusiness.class)
@LocalHome(WarTestLocalHome.class)
@RemoteHome(WarTestRemoteHome.class)
@Stateless
public class WarTestBean {
    public String getString() {
        return "Success";
    }

    public void create() {

    }
}
