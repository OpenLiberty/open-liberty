/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

package com.ibm.ws.repository.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.ConfigSnippetResource;
import com.ibm.ws.repository.resources.internal.ConfigSnippetResourceImpl;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class ConfigSnippetResourceTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    @Test
    public void testIsDownloadable() throws IOException {
        ConfigSnippetResource configSnippet = WritableResourceFactory.createConfigSnippet(repoConnection);
        assertEquals("Admin scripts should be downloadable",
                     DownloadPolicy.ALL, configSnippet.getDownloadPolicy());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException, SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new ConfigSnippetResourceImpl(repoConnection), new ConfigSnippetResourceImpl(repoConnection));
    }
}