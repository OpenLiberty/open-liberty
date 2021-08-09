/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.ejb.Init;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;

@Remote(StaticStatefulRemote.class)
@Stateful(name = "StaticStateful")
public class StaticStatefulBean {
    private int passCount = 0;
    private int actCount = 0;

    private static String staticString = "Static";
    private static SerObj staticSerObj = new SerObj("ABC", "TransABC");

    @Init
    public void create() {
        passCount = 0;
        actCount = 0;
    }

    @PrePassivate
    public void passivate() {
        passCount++;
    }

    @PostActivate
    public void activate() {
        actCount++;
    }

    @TransactionAttribute(NEVER)
    public int getPassivateCount() {
        return passCount;
    }

    @TransactionAttribute(NEVER)
    public int getActivateCount() {
        return actCount;
    }

    @TransactionAttribute(REQUIRED)
    public String getStaticString() {
        return staticString;
    }

    @TransactionAttribute(REQUIRED)
    public SerObj getStaticSerObj() {
        return staticSerObj;
    }

    @Remove
    public void finish() {
    }
}