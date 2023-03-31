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
package test.iiop.common;

import java.rmi.Remote;

import org.omg.CosNaming.NameComponent;

public interface CosNamingChecker extends Remote {
    void checkNameServiceIsAvailable() throws Exception;

    String getNameServiceListingFromServer() throws Exception;

    void bindResolvable(NameComponent[] name) throws Exception;

    void bindResolvableThatThrows(RuntimeException exceptionToThrow, NameComponent[] name) throws Exception;
}
