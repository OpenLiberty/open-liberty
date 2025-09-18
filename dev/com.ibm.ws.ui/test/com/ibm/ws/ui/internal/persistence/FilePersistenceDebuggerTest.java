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
package com.ibm.ws.ui.internal.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * This test DOES hit the file system (namely it reads import.xml).
 * This file should be safe to rely on to exist.
 */
public class FilePersistenceDebuggerTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final FilePersistenceDebugger debugger = new FilePersistenceDebugger();

    /**
     * Test method for {@link com.ibm.ws.ui.internal.persistence.FilePersistenceDebugger#getFileContents(java.io.File)}.
     */
    @Test
    public void getFileContentsDoesntExist() {
        String contents = debugger.getFileContents(new File("idontexist"));
        assertTrue("FAIL: Contents did not start with 'Unable to load file contents:'",
                   contents.startsWith("Unable to load file contents:"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.persistence.FilePersistenceDebugger#getFileContents(java.io.File)}.
     */
    @Test
    public void getFileContentsExists() {
        String contents = debugger.getFileContents(new File("import.xml"));
        assertFalse("FAIL: Contents should not start with 'Unable to load file contents:'",
                    contents.startsWith("Unable to load file contents:"));
    }

}
