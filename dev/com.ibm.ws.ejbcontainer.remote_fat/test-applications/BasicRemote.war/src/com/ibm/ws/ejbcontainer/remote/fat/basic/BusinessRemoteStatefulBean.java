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

import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;

@Stateful
@Remote({ BusinessRMIStateful.class, BusinessRemoteStateful.class })
public class BusinessRemoteStatefulBean implements BusinessRMIStateful, BusinessRemoteStateful {
    private boolean value;

    @Override
    public void initialize(boolean value) {
        this.value = value;
    }

    @Remove
    @Override
    public boolean getValueAndRemove() {
        return value;
    }
}
