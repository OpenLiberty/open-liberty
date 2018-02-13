/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.transientpassivationtest;

import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

@LocalBean
@Stateful(passivationCapable = false)
public class MyStatefulSessionBean {

    String msg = "MyStatefulSessionBean was destroyed";

    public void doNothing() {
        int i = 1;
        i++;
    }

    public void setMessage(String s) {
        msg = s;
    }

    @PreDestroy
    public void preD() {
        GlobalState.addString(msg);
    }

    public MyStatefulSessionBean() {}
}
