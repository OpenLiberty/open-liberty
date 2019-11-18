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
package com.ibm.ws.logstash.collector.internal;

import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.Client;
import com.ibm.ws.collector.ClientPool;
import com.ibm.wsspi.ssl.SSLSupport;

/*
 * A fixed size pool of LogstashClients.  Thread safe.
 */
public class LogstashClientPool extends ClientPool {

    public LogstashClientPool(String sslConfig, SSLSupport sslSupport, int numClients) throws SSLException {
        super(sslConfig, sslSupport, numClients);
    }

    /** {@inheritDoc} */
    @Override
    public Client createClient(String sslConfig, SSLSupport sslSupport) throws SSLException {
        return new LogstashClient(sslConfig, sslSupport);
    }

}
