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
package com.ibm.ws.ejbcontainer.osgi.internal.ejb;

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

@Stateless
@RemoteHome(RemoteHomeIntf.class)
@LocalHome(LocalHomeIntf.class)
@Remote({ RemoteIntf1.class, RemoteIntf2.class })
@Local({ LocalIntf1.class, LocalIntf2.class })
@LocalBean
public class TestInterfaces {}
