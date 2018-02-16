/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;
import com.ibm.ws.repository.resources.writeable.IfixResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class IfixResourceTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    @Test
    public void testIsDownloadable() {
        assertEquals("Ifixes should not be downloadable",
                     DownloadPolicy.ALL, WritableResourceFactory.createIfix(repoConnection).getDownloadPolicy());
    }

    @Test
    public void testDisplayPolicy() throws Exception {
        IfixResourceWritable ifix = WritableResourceFactory.createIfix(repoConnection);
        assertEquals("Ifixes display policy should be hidden by default",
                     DisplayPolicy.HIDDEN, ifix.getDisplayPolicy());
        assertEquals("Ifixes web display policy should be hidden by default", DisplayPolicy.HIDDEN, ifix.getWebDisplayPolicy());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException, SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new IfixResourceImpl(repoConnection), new IfixResourceImpl(repoConnection));
    }
}
