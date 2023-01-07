/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.bindings.ejbinwar.web;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseSingletonInterface;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseSingletonInterfaceRemote;

@Singleton
@Startup
@Local(BasicLooseSingletonInterface.class)
@Remote(BasicLooseSingletonInterfaceRemote.class)
public class LooseSingletonBean {
    public boolean verifyLooseSingletonBeanLookup() {
        return true;
    }
}
