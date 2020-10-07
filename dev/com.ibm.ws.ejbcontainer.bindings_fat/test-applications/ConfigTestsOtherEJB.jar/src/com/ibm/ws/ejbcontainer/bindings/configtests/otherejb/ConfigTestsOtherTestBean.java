/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.configtests.otherejb;

import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

@Local(ConfigTestsOtherLocalBusiness.class)
@Remote(ConfigTestsOtherRemoteBusiness.class)
@LocalHome(ConfigTestsOtherLocalHome.class)
@RemoteHome(ConfigTestsOtherRemoteHome.class)
@Stateless
public class ConfigTestsOtherTestBean {
    public String getString() {
        return "Success";
    }

    public void create() {

    }
}
