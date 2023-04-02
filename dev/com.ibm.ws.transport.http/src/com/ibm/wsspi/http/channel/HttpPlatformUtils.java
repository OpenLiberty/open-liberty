/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package com.ibm.wsspi.http.channel;

import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * Interface of the HTTP service that interacts with the WAS platform utils
 * service when running inside of WAS.
 * 
 */
public interface HttpPlatformUtils {

    /**
     * Log the z/os legacy message about a failure for this connection.
     * 
     * @param isc
     */
    void logLegacyMessage(HttpInboundServiceContext isc);

}
