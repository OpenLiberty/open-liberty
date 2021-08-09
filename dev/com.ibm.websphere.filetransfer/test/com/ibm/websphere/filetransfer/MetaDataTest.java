/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.filetransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.ibm.websphere.filetransfer.FileServiceMXBean.MetaData;

/**
 *
 */
public class MetaDataTest {
    private static final String FILENAME = "/file";

    /**
     * Test method for {@link com.ibm.websphere.filetransfer.FileServiceMXBean.MetaData}.
     */
    @Test
    public void ctor1() {
        MetaData md = new MetaData(false, null, 0L, false, FILENAME);
        assertFalse("FAIL: getDirectory should be false",
                    md.getDirectory());
        assertNull("FAIL: getLastModified should null",
                   md.getLastModified());
        assertEquals("FAIL: getSize should be 0",
                     (Long) 0L, md.getSize());
        assertFalse("FAIL: getReadOnly should be false",
                    md.getReadOnly());
        assertEquals("FAIL: did not get back expected name",
                     FILENAME, md.getFileName());
    }

    /**
     * Test method for {@link com.ibm.websphere.filetransfer.FileServiceMXBean.MetaData}.
     */
    @Test
    public void ctor2() {
        Date date = new Date();
        MetaData md = new MetaData(true, date, 1000L, true, FILENAME);
        assertTrue("FAIL: getDirectory should be true",
                   md.getDirectory());
        assertEquals("FAIL: did not get back expected last modified value",
                     date, md.getLastModified());
        assertEquals("FAIL: getSize should be 100",
                     (Long) 1000L, md.getSize());
        assertTrue("FAIL: getReadOnly should be true",
                   md.getReadOnly());
        assertEquals("FAIL: did not get back expected name",
                     FILENAME, md.getFileName());
    }

}
