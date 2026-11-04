/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola;

import java.io.IOException;

import javax.resource.ResourceException;

import com.ibm.ws.zos.channel.wola.internal.otma.OTMAException;
import com.ibm.ws.zos.channel.wola.internal.otma.msg.OTMAMessageParseException;

/**
 * The WOLA JCA code (com.ibm.ws.zos.ola) bridges into the WOLA channel
 * via this interface. It looks up the implementation of this interface
 * in the OSGi DS service registry.
 *
 */
public interface WolaJcaBridge {

    /**
     * Invoke the given serviceName, hosted by the client with the given registerName,
     * passing along the given appData to the service. The response is returned as
     * a byte[].
     *
     * @param registerName       - The registration name of the client hosting the service
     * @param serviceName        - The service to invoke
     * @param appData            - Parameter data for the service
     * @param wolaJcaRequestInfo - Extra meta data for the request
     *
     * @return The response data
     *
     * @throws IOException       for failures to write the request or read the response
     * @throws ResourceException If the target serviceName returned an exception response.
     */
    public byte[] jcaInvoke(String registerName,
                            String serviceName,
                            byte[] appData,
                            WolaJcaRequestInfo wolaJcaRequestInfo) throws IOException, ResourceException;

    /**
     * Invoke a program within IMS via the OTMA C/I, passing along the given request data.
     * The response is returned as a byte[].
     *
     * @param requestData  - Data to be passed via OTMA
     * @param requestParms - Meta data for the request
     *
     * @return The response data
     * @throws OTMAException
     * @throws IOException
     * @throws OTMAMessageParseException
     * @throws com.ibm.ws.zos.channel.wola.internal.otma.ws390.ola.OTMAException
     */
    public byte[] otmaInvoke(byte[] requestData, WolaOtmaRequestInfo requestParms) throws IOException, OTMAException, OTMAMessageParseException;
}
