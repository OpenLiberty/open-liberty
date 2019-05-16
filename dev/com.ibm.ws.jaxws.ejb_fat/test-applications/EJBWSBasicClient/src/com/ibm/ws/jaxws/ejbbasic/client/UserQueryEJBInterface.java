/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbbasic.client;

public interface UserQueryEJBInterface {

    public String getUserAsyncHandler(final String name);

    public String getUserAsyncResponse(String name);

    public void setServerName(String serverName);

    public void setServerPort(String serverPort);

}
