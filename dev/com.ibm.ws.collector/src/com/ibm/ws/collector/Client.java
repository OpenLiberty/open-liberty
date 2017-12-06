/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector;

import java.io.IOException;
import java.util.List;

/**
 * Client interface used for LogstashClients and LogmetClients
 */
public interface Client {

    public void connect(String hostName, int port) throws IOException;

    public void sendData(List<Object> dataObjects) throws IOException;

    public void close() throws IOException;

}
