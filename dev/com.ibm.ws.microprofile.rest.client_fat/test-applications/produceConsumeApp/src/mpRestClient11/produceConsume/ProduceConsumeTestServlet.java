/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.produceConsume;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ProduceConsumeTestServlet")
public class ProduceConsumeTestServlet extends FATServlet {
    private final static Logger _log = Logger.getLogger(ProduceConsumeTestServlet.class.getName());

    /**
     * Tests that MP Rest Client's <code>@Produces</code> annotation affects the value transmitted in
     * the <code>Accept</code> header, and that it's <code>@Consumes</code> annotation affects the
     * value transmitted in the <code>Content-Type</code> header.  Note that this is opposite of
     * what you would expect for JAX-RS resources. 
     */
    @Test
    public void testProducesConsumesAnnotationOnClientInterface(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testProducesConsumesAnnotationOnClientInterface";
        MyClient client = RestClientBuilder.newBuilder()
                                           .baseUri(URI.create("http://localhost:23/null"))
                                           .register(Filter.class)
                                           .build(MyClient.class);
        
        _log.info(m + " @Produce(application/json) @Consume(application/xml)");
        Response r = client.produceJSONConsumeXML();
        String acceptHeader = r.getHeaderString("Sent-Accept");
        _log.info(m + "Sent-Accept: " + acceptHeader);
        String contentTypeHeader = r.getHeaderString("Sent-ContentType");
        _log.info(m + "Sent-ContentType: " + contentTypeHeader);
        assertEquals(MediaType.APPLICATION_JSON, acceptHeader);
        assertEquals(MediaType.APPLICATION_XML, contentTypeHeader);
        
        _log.info(m + " @Produce(application/xml) @Consume(application/json)");
        r = client.produceXMLConsumeJSON();
        acceptHeader = r.getHeaderString("Sent-Accept");
        _log.info(m + "Sent-Accept: " + acceptHeader);
        contentTypeHeader = r.getHeaderString("Sent-ContentType");
        _log.info(m + "Sent-ContentType: " + contentTypeHeader);
        assertEquals(MediaType.APPLICATION_XML, acceptHeader);
        assertEquals(MediaType.APPLICATION_JSON, contentTypeHeader);
    }
}
