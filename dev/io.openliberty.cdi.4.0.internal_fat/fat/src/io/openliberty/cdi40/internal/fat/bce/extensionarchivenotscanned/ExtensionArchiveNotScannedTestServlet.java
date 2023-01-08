/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi40.internal.fat.bce.extensionarchivenotscanned;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Test that archives containing build compatible extensions do not get scanned
 */
@WebServlet("/extensionArchiveNotScanned")
@SuppressWarnings("serial")
public class ExtensionArchiveNotScannedTestServlet extends FATServlet {

    @Inject
    private Instance<TestBean1> testBean1s;

    @Inject
    private Instance<TestBean2> testBean2s;

    @Test
    public void test() {
        // TestBean1 is added by the extension
        assertEquals(1L, testBean1s.stream().count());

        assertEquals(0L, testBean2s.stream().count());
    }

}
