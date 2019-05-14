/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.singleton.ann.ejb;

import javax.ejb.Remote;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.BasicSingleton;

@Singleton(name = "FailedConstructorRemote")
@Remote(BasicSingleton.class)
public class FailedConstructorRemoteBean implements BasicSingleton {
    private boolean ivValue;

    public FailedConstructorRemoteBean() {
        throw new UnsupportedOperationException("Expected Constructor Exception.");
    }

    @Override
    public boolean getBoolean() {
        return ivValue;
    }

    @Override
    public void setBoolean(boolean value) {
        ivValue = value;
    }
}
