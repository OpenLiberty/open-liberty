/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static com.ibm.wsspi.classloading.SpiType.SPI;
import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.wsspi.classloading.SpiType;

public class SpiTypeTest {
    @Test
    public void testSpiTypeParsing() {
        assertEquals(null, SpiType.fromString(""));
        assertEquals(null, SpiType.fromString("rubblish"));
        assertEquals(null, SpiType.fromString(null));
        assertEquals(SPI, SpiType.fromString("spi"));
    }

    @Test
    public void testSpiTypeSetParsing() {
        assertEquals(set(), SpiType.createSpiTypeSet(""));
        assertEquals(set(), SpiType.createSpiTypeSet("rubbish"));
        assertEquals(set(), SpiType.createSpiTypeSet(null));
        assertEquals(set(SPI), SpiType.createSpiTypeSet("spi"));
    }

    private static Set<SpiType> set(SpiType... types) {
        return new HashSet<SpiType>(Arrays.asList(types));
    }
}
