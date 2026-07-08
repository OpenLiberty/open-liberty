/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.securitylimits;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.ibm.websphere.jaxrs20.multipart.AttachmentBuilder;
import com.ibm.websphere.jaxrs20.multipart.IAttachment;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

/**
 * Tests for CXF #3159 (attachment-headers-max-count) and CXF #3177
 * (maxFormParameterCount default value).
 *
 * The WAR contains three Application sub-classes mounted at:
 * <ul>
 *   <li>{@code /app}             – default limits (500 for both)</li>
 *   <li>{@code /lowHeadersApp}   – attachment-headers-max-count=2</li>
 *   <li>{@code /lowFormParamApp} – maxFormParameterCount=2</li>
 * </ul>
 *
 * <h3>Attachment header count (CXF #3159)</h3>
 * Attachments at index 1+ in a List are serialised by CXF's
 * AttachmentSerializer.writeHeaders(), which emits Content-Type,
 * Content-Transfer-Encoding, Content-ID, and Content-Disposition – four unique
 * MIME part headers. loadPartHeaders() counts unique header names as it parses,
 * and throws IOException when the count reaches the configured limit.
 *
 * <h3>Form parameter count (CXF #3177)</h3>
 * Before this fix, {@code maxFormParameterCount} had no default: if the property
 * was not explicitly set, no limit was applied. After the fix the default is 500.
 * The tests verify both that the default limit of 500 is enforced, and that
 * an application-supplied lower limit is also enforced.
 */
@WebServlet(urlPatterns = "/SecurityLimitsTestServlet")
public class SecurityLimitsTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static final String BASE_URL =
        "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/securitylimits";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    // -------------------------------------------------------------------------
    // Attachment header count tests (CXF #3159)
    // -------------------------------------------------------------------------

    /**
     * Sends a two-part multipart request using the default limit (500).
     * The second part is serialised via AttachmentSerializer.writeHeaders(),
     * which writes four unique headers (Content-Type, Content-Transfer-Encoding,
     * Content-ID, Content-Disposition). Four headers is well within 500.
     * Expects a 200 response.
     */
    @Test
    public void testAttachmentHeadersWithinDefaultLimit() {
        List<IAttachment> parts = buildTwoParts();

        Response response = client.target(BASE_URL)
                                  .path("/app/limits/multipart")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.entity(parts, MediaType.MULTIPART_FORM_DATA));
        assertEquals(200, response.getStatus());
    }

    /**
     * Sends a two-part multipart request to the low-header-limit application
     * (attachment-headers-max-count=2). AttachmentSerializer writes four unique
     * header names per part (Content-Type, Content-Transfer-Encoding, Content-ID,
     * Content-Disposition). addHeaderLine() checks {@code heads.size() >= limit}
     * before inserting each new unique name, so the IOException is thrown when
     * the third unique header name (Content-ID) is about to be added and the map
     * already contains 2 entries.
     *
     * Expects a 400 Bad Request response (CXF maps the IOException to a Fault
     * which is surfaced as 500 via WebContainer).
     */
    @Test
    @AllowedFFDC({"java.io.IOException", "org.apache.cxf.interceptor.Fault"})
    public void testAttachmentHeadersExceedLowLimit() {
        List<IAttachment> parts = buildTwoParts();

        Response response = client.target(BASE_URL)
                                  .path("/lowHeadersApp/limits/multipart")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.entity(parts, MediaType.MULTIPART_FORM_DATA));

        assertEquals(500, response.getStatus());
    }

    // -------------------------------------------------------------------------
    // Form parameter count tests (CXF #3177)
    // -------------------------------------------------------------------------

    /**
     * Sends a single form parameter using the default limit (500).
     * One parameter is well within range. Expects a 200 response.
     */
    @Test
    public void testFormParamWithinDefaultLimit() {
        Form form = new Form().param("p1", "value1");

        Response response = client.target(BASE_URL)
                                  .path("/app/limits/form")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.form(form));
        assertEquals(200, response.getStatus());
    }

    /**
     * Sends exactly two form parameters to the low-limit application
     * (maxFormParameterCount=2). The check is {@code numberOfParts >= maxPartsCount},
     * so two parameters at a limit of two triggers a 413 response.
     */
    @Test
    public void testFormParamAtLowLimit() {
        Form form = new Form()
            .param("p1", "value1")
            .param("p2", "value2");

        Response response = client.target(BASE_URL)
                                  .path("/lowFormParamApp/limits/form")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.form(form));
        assertEquals(413, response.getStatus());
    }

    /**
     * Verifies that the new default limit of 500 is applied even without any
     * explicit configuration. Sends 500 form parameters to the default-limit
     * application, which should trigger a 413.
     */
    @Test
    public void testFormParamAtDefaultLimit() {
        Form form = new Form();
        for (int i = 0; i < 500; i++) {
            form.param("param" + i, "value" + i);
        }

        Response response = client.target(BASE_URL)
                                  .path("/app/limits/form")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.form(form));
        assertEquals(413, response.getStatus());
    }

    /**
     * Verifies that 499 form parameters – one fewer than the default limit –
     * do not trigger a 413.
     */
    @Test
    public void testFormParamBelowDefaultLimit() {
        Form form = new Form();
        for (int i = 0; i < 499; i++) {
            form.param("param" + i, "value" + i);
        }

        Response response = client.target(BASE_URL)
                                  .path("/app/limits/form")
                                  .request(MediaType.TEXT_PLAIN)
                                  .post(Entity.form(form));
        assertEquals(200, response.getStatus());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a two-element attachment list.  Attachments at index 1+ in a List
     * are written by AttachmentSerializer.writeHeaders(), giving us predictable
     * wire-level header content to test against the configured limit.
     */
    private List<IAttachment> buildTwoParts() {
        List<IAttachment> parts = new ArrayList<>();
        parts.add(AttachmentBuilder.newBuilder("part1")
                                   .inputStream(new ByteArrayInputStream("hello".getBytes()))
                                   .contentType(MediaType.TEXT_PLAIN)
                                   .build());
        parts.add(AttachmentBuilder.newBuilder("part2")
                                   .inputStream(new ByteArrayInputStream("world".getBytes()))
                                   .contentType(MediaType.TEXT_PLAIN)
                                   .build());
        return parts;
    }
}
