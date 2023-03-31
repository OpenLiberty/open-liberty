/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package test.iiop.client;

import java.rmi.RemoteException;

import test.iiop.common.ClientCallbackService;
import test.iiop.notcommon.VersionedException;

public class ClientCallbackServiceImpl implements ClientCallbackService {
    @Override
    public void throwRuntimeException() throws RemoteException {
        throw new VersionedException();
    }
}
