/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package csiv2.sample.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 */
public interface IntermediateTestBean extends Remote {

    public String callRemoteTestBean(String port) throws RemoteException;

}
