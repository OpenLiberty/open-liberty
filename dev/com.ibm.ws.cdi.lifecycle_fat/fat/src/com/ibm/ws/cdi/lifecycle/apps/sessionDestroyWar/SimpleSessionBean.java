/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.sessionDestroyWar;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;

@SessionScoped
public class SimpleSessionBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private static int id = 0;

    private final int localID;

    public SimpleSessionBean() {
        localID = id;
        id++;
    }

    public int getID() {
        return localID;
    }

    @PreDestroy
    public void pD() {
        System.out.println("pre destroy");
    }

    @PostConstruct
    public void pC() {
        System.out.println("post construct");
    }

}
