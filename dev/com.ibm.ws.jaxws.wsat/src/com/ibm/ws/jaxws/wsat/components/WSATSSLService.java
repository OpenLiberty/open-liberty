/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jaxws.wsat.components;

import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.transport.Conduit;

/**
 *
 */
public interface WSATSSLService {
    public boolean checkId(String id);

    public Properties getProperties(String id) throws Exception;

    public Map<String, Object> getOutboundConnectionMap();

    public SSLSocketFactory getSSLSocketFactory(String id, URL url) throws Exception;

    public void setSSLFactory(Conduit conduit, String sslRef, String certAlias);

}
