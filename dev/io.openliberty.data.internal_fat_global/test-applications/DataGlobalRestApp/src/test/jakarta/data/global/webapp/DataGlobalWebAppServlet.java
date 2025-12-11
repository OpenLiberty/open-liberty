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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@ApplicationScoped
@SuppressWarnings("serial")
@WebServlet("/webapp/*")
public class DataGlobalWebAppServlet extends FATServlet {
    @Inject
    Alphabet alphabet;

    @Inject
    Dictionary dictionary;

    /**
     * Set up some data during application initialization
     */
    public void initialize(@Observes @Initialized(ApplicationScoped.class) Object init) {
        dictionary.addWord(Word.of("initialized"));
    }

    /**
     * Set up some data before tests run.
     */
    public void setup(@Observes Startup event) {
        dictionary.addWord(Word.of("startup"));
    }

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

    /**
     * Verify that a method that observes an event with qualifier @Initialized(ApplicationScoped.class)
     * has access to a Jakarta Data repository and can use it to populate data.
     */
    @Test
    public void testObservesInitialized() {
        assertEquals(true, dictionary.isWord("initialized"));
    }

    /**
     * Verify that a method that observes the CDI Startup event has access to a
     * Jakarta Data repository and can use it to populate data.
     */
    @Test
    public void testStartupEvent() {

        assertEquals(true, dictionary.isWord("startup"));
    }
}
