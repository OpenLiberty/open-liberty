/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tls;

import java.util.Map;
import io.netty.handler.ssl.SslContext;

public interface NettyTlsProvider {

    public SslContext getOutboundSSLContext(Map<String, Object> sslOptions, String host, String port);

    public SslContext getInboundSSLContext(Map<String, Object> sslOptions, String host, String port);
}
