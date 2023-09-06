/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.yasson.translation.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.eclipse.yasson.internal.JsonbConfigProperties;
import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;

import componenttest.app.FATServlet;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings({ "serial", "restriction" })
@WebServlet(urlPatterns = "/YassonTranslationTestServlet")
public class YassonTranslationTestServlet extends FATServlet {

    @Override
    public void before() {
        System.out.println("Server locale is: " + Locale.getDefault());
    }

    public void testTranslationMessageDefault() {
        String expected = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.getDefault());
        String actual = Messages.getMessage(MessageKeys.INVOKING_GETTER);

        assertEquals(expected, actual);
    }

    public void testTranslationMessageProvidedLocale() {
        String franceMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.FRANCE);
        String germanyMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.GERMANY);

        assertFalse(germanyMessage.equals(franceMessage));
    }

    public void testTranslationMessageServerLocale() {
        if (Locale.getDefault() != Locale.JAPAN) {
            throw new RuntimeException("Server should have been set to a locale of Japan");
        }

        String japanMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER);
        String germanyMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.GERMANY);

        assertFalse(germanyMessage.equals(japanMessage));
    }

    public void testTranslationMessageServerException() {
        if (Locale.getDefault() != Locale.JAPAN) {
            throw new RuntimeException("Server should have been set to a locale of Japan");
        }

        String expectedMessage = Messages.getMessage(MessageKeys.JSONB_CONFIG_PROPERTY_INVALID_TYPE, Locale.JAPAN, "jsonb.locale", "Locale");
        String actualMessage = "";

        // Force yasson to throw exception
        JsonbConfig config = new JsonbConfig()
                        .setProperty(JsonbConfig.LOCALE, Boolean.TRUE);
        try {
            new JsonbConfigProperties(config);
            fail("Should have thrown a JsonbException");
        } catch (JsonbException x) {
            actualMessage = x.getLocalizedMessage();
        }

        assertEquals(expectedMessage, actualMessage);
    }
}
