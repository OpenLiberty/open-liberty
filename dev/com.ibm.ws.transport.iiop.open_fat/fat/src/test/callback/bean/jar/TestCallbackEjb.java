/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.callback.bean.jar;

import java.rmi.RemoteException;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import test.iiop.common.ClientCallbackService;
import test.iiop.common.ServerCallbackService;
import test.iiop.notcommon.VersionedException;

@Stateless
@Remote(ServerCallbackService.class)
public class TestCallbackEjb implements ServerCallbackService {
    public TestCallbackEjb() {}

    @Override
    public void throwRuntimeException(ClientCallbackService invocand) throws RemoteException {
        if (invocand == null)
            throw new VersionedException();
        else
            invocand.throwRuntimeException();
    }

    @Override
    public String toString() {
        return "TestCallbackEjb []";
    }
}
