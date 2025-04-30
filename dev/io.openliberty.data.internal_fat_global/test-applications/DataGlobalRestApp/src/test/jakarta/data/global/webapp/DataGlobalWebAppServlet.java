/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.global.webapp;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/webapp/*")
public class DataGlobalWebAppServlet extends FATServlet {
    @Inject
    Alphabet alphabet;

    @Inject
    Dictionary dictionary;

    /**
     * Use a repository that requires a java:global DataSource that is defined in
     * a different application.
     */
    @Test
    public void testRepositoryUsesDataSourceFromDifferentApp() {

        dictionary.addWord(Word.of("hello"));

        assertEquals(true, dictionary.isWord("Hello"));

        assertEquals(false, dictionary.isWord("llo"));

        assertEquals(1, dictionary.deleteWord("hello"));
    }

    /**
     * Use a repository that requires a java:global/env DataSource resource
     * reference that is defined in this same application.
     */
    @Test
    public void testRepositoryUsesResourceReferenceFromSameApp() {

        alphabet.addLetter(Letter.of('D'));

        assertEquals(true, alphabet.hasLetter('D'));

        assertEquals(false, alphabet.hasLetter('2'));

        assertEquals(1, alphabet.deleteLetter('D'));
    }
}
