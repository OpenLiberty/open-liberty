/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package io.openliberty.checkpoint.testapp.jaxws.ejbbasic.client;

public interface UserQueryEJBInterface {

    public String getUserAsyncHandler(final String name);

    public String getUserAsyncResponse(String name);

    public void setServerName(String serverName);

    public void setServerPort(String serverPort);

}
