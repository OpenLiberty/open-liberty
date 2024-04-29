/*******************************************************************************n * Copyright (c) 2024 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class ArtifactDownloaderUtilsTest {

    /**
     * Test method for {@link com.ibm.ws.install.internal.ArtifactDownloaderUtils#checkResponseCode(int)}.
     */
    @Test
    public void testCheckResponseCodeOK() {
        assertTrue(ArtifactDownloaderUtils.checkResponseCode(200));
        assertTrue(ArtifactDownloaderUtils.checkResponseCode(400));
        assertTrue(ArtifactDownloaderUtils.checkResponseCode(403));
        assertTrue(ArtifactDownloaderUtils.checkResponseCode(401));
        assertTrue(ArtifactDownloaderUtils.checkResponseCode(301));
    }

    @Test
    public void testCheckResponseCodeFail() {
        assertFalse(ArtifactDownloaderUtils.checkResponseCode(404));
    }

}
