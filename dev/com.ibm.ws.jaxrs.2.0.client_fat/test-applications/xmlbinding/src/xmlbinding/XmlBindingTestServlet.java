/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package xmlbinding;

import static org.junit.Assert.assertEquals;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/XmlBindingTestServlet")
public class XmlBindingTestServlet extends FATServlet {

    @Test
    public void testCanReceiveXmlEntity(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response r = ClientBuilder.newBuilder()
                                  .register(ReturnXmlFilter.class)
                                  .build()
                                  .target("http://localhost:12345/notUsed")
                                  .request(MediaType.APPLICATION_XML_TYPE)
                                  .get();

        assertEquals(200, r.getStatus());
        Widget w = r.readEntity(Widget.class);
        //assertEquals("foo", w.getName());
        assertEquals(100, w.getQuantity());
    }
}
