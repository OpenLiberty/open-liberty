/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.headerPropagation;

import javax.ws.rs.core.MultivaluedMap;

import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedHashMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class CustomClientHeadersFactory implements ClientHeadersFactory {

    private static final Logger LOG = Logger.getLogger(CustomClientHeadersFactory.class.getName());

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> myHeaders = new MultivaluedHashMap<>();
        myHeaders.putSingle("HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY", "123");
        LOG.info("update - adding HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY=123");
        return myHeaders;
        
    }

}
