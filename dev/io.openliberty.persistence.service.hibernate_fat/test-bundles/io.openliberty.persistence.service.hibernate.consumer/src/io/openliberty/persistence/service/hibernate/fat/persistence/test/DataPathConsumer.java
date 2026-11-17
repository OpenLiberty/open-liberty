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
package io.openliberty.persistence.service.hibernate.fat.persistence.test;

/**
 * Shared interface between the OSGi consumer bundle and the test web application
 * for the Jakarta Data path, which obtains persistence service units from a
 * DatabaseStore and passes it the internal entityClassInfo, tableNames and
 * generatedEntities properties.
 */
public interface DataPathConsumer {

    String getTablePrefix();

    void insertAnnotated(long id, String name);

    String findAnnotatedName(long id);

    void insertUnannotated(long id, String name, boolean active, String... tags);

    String findUnannotatedName(long id);

    int findUnannotatedTagCount(long id);

    long countUnannotatedByActive(boolean active);

    void insertBook(long id, String title, long authorId, String authorName);

    /**
     * Loads a Book and verifies its lazy many-to-one Author is not loaded until used.
     *
     * @return "lazy:" followed by the author name, or a message describing the problem.
     */
    String readLazyAuthor(long id);

    void insertCommercialAd(long id, String title, int seconds);

    int findCommercialAdSeconds(long id);

    /**
     * Creates an EntityAgent from the unit and closes it.
     *
     * @return the class name of the agent.
     */
    String useEntityAgent() throws Exception;

    String generateDDL();

    /**
     * Creates and closes a second persistence service unit that is given a
     * generatedEntities property, as Jakarta Data does for Java records.
     */
    void createUnitWithGeneratedEntityResource() throws Exception;
}
