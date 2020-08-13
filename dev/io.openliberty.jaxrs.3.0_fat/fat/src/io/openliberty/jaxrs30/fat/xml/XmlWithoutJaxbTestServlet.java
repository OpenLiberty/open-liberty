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
package io.openliberty.jaxrs30.fat.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/XmlWithoutJaxbTestServlet")
public class XmlWithoutJaxbTestServlet extends FATServlet {

    @Test
    public void testCannotInvokeResourceWithApplicationXmlWithNoBuiltInJaxbProvider() throws Exception {
        AtomicBoolean loggedError = new AtomicBoolean(false);
        Logger logger = Logger.getLogger("org.jboss.resteasy.resteasy_jaxrs.i18n");
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().equals(Level.SEVERE)
                    && record.getMessage().contains("RESTEASY002005")) {
                    loggedError.set(true);
                }
                System.out.println("Logged " + record.getLevel() + ": " + record.getMessage() + " | " + record.getThrown());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }});
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/xml/app/path/300");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(500, conn.getResponseCode());
        assertTrue(loggedError.get());
    }
}
