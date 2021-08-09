/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

/**
 *
 */
public class MediaRangeTest {

    @Test
    public void parseMediaRange() {

        String acceptHeaderValue = "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5";
        MediaRange[] acceptRange = MediaRange.parse(acceptHeaderValue);
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        HashMap<String, String[]> extensions = new HashMap<String, String[]>();
        MediaRange[] expectedRanges = { new MediaRange("application/xhtml+xml", params, 1.0f, extensions),
                                       new MediaRange("application/xml", params, 1.0f, extensions),
                                       new MediaRange("image/png", params, 1.0f, extensions),
                                       new MediaRange("text/html", params, 0.9f, extensions),
                                       new MediaRange("text/plain", params, 0.8f, extensions),
                                       new MediaRange("*/*", params, 0.5f, extensions)

        };
        assertArrayEquals(expectedRanges, acceptRange);

    }

    @Test
    public void matchMediaRanges() {

        HashMap<String, String[]> params = new HashMap<String, String[]>();
        HashMap<String, String[]> extensions = new HashMap<String, String[]>();
        MediaRange range1 = new MediaRange("application/xhtml+xml", params, 1.0f, extensions);
        MediaRange range2 = new MediaRange("application/*", params, 1.0f, extensions);
        MediaRange range3 = new MediaRange("application/json", params, 1.0f, extensions);
        MediaRange range4 = new MediaRange("application/xhtml+xml", params, 0.9f, extensions);
        MediaRange range5 = new MediaRange("", params, 0.9f, extensions);
        MediaRange range6 = new MediaRange("*/*", params, 0.9f, extensions);

        assertTrue(MediaRange.contentRangesMatch(range1, range2));
        assertFalse(MediaRange.contentRangesMatch(range1, range3));
        assertTrue(MediaRange.contentRangesMatch(range1, range4));
        assertTrue(MediaRange.contentRangesMatch(range5, range6));

    }
}
