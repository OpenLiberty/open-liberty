/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

import com.ibm.ws.security.client.fat.view.MyComponentInjectionHomeRemote;


/**
 * Session Bean implementation class MyComponentInjectionBean
 */
@Stateless
@RemoteHome(MyComponentInjectionHomeRemote.class)
public class MyComponentInjectionBean {

    public int add(int x, int y) {
        return x+y;
    }
}

