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

import java.net.URI;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class CdiCustomClientHeadersFactory implements ClientHeadersFactory {

    private static final Logger LOG = Logger.getLogger(CdiCustomClientHeadersFactory.class.getName());

    @Context
    private UriInfo uriInfo;

    @Inject
    private Foo foo;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> myHeaders = new MultivaluedHashMap<>();
        myHeaders.putSingle("HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY", "456");
        LOG.info("update - adding HEADER_FROM_CUSTOM_CLIENTHEADERSFACTORY=456");

        if (uriInfo != null) {
            URI uri = uriInfo.getAbsolutePath();
            myHeaders.putSingle("INJECTED_URI_INFO", uri == null ? "null" : uri.toString());
        }
        LOG.info("UriInfo injected by @Context: " + uriInfo);

        if (foo != null) {
            myHeaders.putSingle("INJECTED_FOO", foo.getWord());
        }
        LOG.info("Foo injected by @Inject: " + foo);
        return myHeaders;
        
    }

}
