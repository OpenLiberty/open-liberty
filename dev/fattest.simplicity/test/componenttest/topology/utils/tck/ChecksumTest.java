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
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import componenttest.topology.utils.tck.TCKUtilities;

/**
 *
 */
public class ChecksumTest {

    @Test
    public void testSHA1() throws NoSuchAlgorithmException, IOException {
        InputStream is = ChecksumTest.class.getResourceAsStream("dependency.txt");
        String sha1 = TCKUtilities.generateSHA1(is);
        assertEquals("1799590f9c3039a6d243a8e72ee84e65c4e0fa44", sha1);
    }

    @Test
    public void testSHA256() throws NoSuchAlgorithmException, IOException {
        InputStream is = ChecksumTest.class.getResourceAsStream("dependency.txt");
        String sha256 = TCKUtilities.generateSHA256(is);
        assertEquals("123f2ccf71ac99f57ea296b071b1d03478a2e89d7973cc25f9eb1946bb83b4c8", sha256);
    }

}
