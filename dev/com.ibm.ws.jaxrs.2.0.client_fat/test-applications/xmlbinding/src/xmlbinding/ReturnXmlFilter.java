/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package xmlbinding;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ReturnXmlFilter implements ClientRequestFilter {

    //private static String XML = "<widget name=\"foo\" quantity=\"100\" />";
    private static String XML = "<widget><name>foo</name><quantity>100</quantity></widget>";

    @Override
    public void filter(ClientRequestContext crc) throws IOException {
        crc.abortWith(Response.ok(XML, MediaType.APPLICATION_XML).build());
    }
}
