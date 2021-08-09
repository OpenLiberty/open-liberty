/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb;

import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * RMI Remote Stateless bean for testing IDLEntity parameters and return types.
 **/
@Stateless
@Remote(IDLEntityRMIC.class)
public class IDLEntityRMICBean extends IDLEntityRemoteBean {
    // all methods inherited from IDLEntityRemoteBean
}