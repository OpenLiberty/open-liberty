/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import javax.ejb.BeforeCompletion;
import javax.ejb.Stateful;

@Stateful
public class RollbackBean {
    public void enlist() {
    }

    @BeforeCompletion
    public void beforeCompletion() {
        throw new TestRollbackException();
    }
}
