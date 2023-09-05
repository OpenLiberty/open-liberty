/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.yasson.translation.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;
import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings({ "serial", "restriction" })
@WebServlet(urlPatterns = "/yassontranslationtestapp")
public class YassonTranslationTestServlet extends FATServlet {

    @Test
    public void testTranslationMessageDefault() {
        String expected = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.getDefault());
        String actual = Messages.getMessage(MessageKeys.INVOKING_GETTER);

        assertEquals(expected, actual);
    }

    @Test
    public void testTranslationMessageNewLocale() {
        String franceMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.FRANCE);
        String defaultMessage = Messages.getMessage(MessageKeys.INVOKING_GETTER, Locale.getDefault());

        if (Locale.getDefault() == Locale.FRANCE) {
            assertTrue(defaultMessage.equals(franceMessage));
        } else {
            assertFalse(defaultMessage.equals(franceMessage));
        }
    }
}
