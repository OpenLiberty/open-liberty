/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.remote.singleton.ann.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import io.openliberty.ejbcontainer.remote.singleton.ann.shared.BasicSingleton;

@Singleton(name = "FailedPostConstructRBRemote")
@Remote(BasicSingleton.class)
public class FailedPostConstructRBRemoteBean implements BasicSingleton {
    private boolean ivValue;

    @Resource
    SessionContext ivEjbContext;

    @PostConstruct
    private void initialize() {
        ivEjbContext.setRollbackOnly();
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
