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

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.BasicRmiSingleton;

@Singleton(name = "FailedInjectionRmi")
@Remote(BasicRmiSingleton.class)
public class FailedInjectionRmiBean {
    private boolean ivValue;

    @Resource
    private void setSessionContext(SessionContext ejbContext) {
        throw new UnsupportedOperationException("Expected Injection Exception.");
    }

    public boolean getBoolean() {
        return ivValue;
    }

    public void setBoolean(boolean value) {
        ivValue = value;
    }
}
