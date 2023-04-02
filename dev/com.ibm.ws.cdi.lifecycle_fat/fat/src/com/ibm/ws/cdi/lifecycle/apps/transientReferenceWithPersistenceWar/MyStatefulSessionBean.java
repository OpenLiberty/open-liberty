/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.transientReferenceWithPersistenceWar;

import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

@LocalBean
@Stateful(passivationCapable = false)
public class MyStatefulSessionBean {

    String msg = "MyStatefulSessionBean not injected anywhere destroyed";

    public void doNothing() {
        int i = 1;
        i++;
    }

    public void setDestroyMessage(String s) {
        msg = s;
    }

    @PreDestroy
    public void preD() {
        GlobalState.addString(msg);
    }

    public MyStatefulSessionBean() {}
}
