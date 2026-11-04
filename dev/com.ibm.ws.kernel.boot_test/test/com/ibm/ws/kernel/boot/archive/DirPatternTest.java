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
package com.ibm.ws.kernel.boot.archive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;

/**
 * Unit tests for {@link DirPattern} include/exclude pattern logic.
 */
public class DirPatternTest {

    @Rule
    public TestName testName = new TestName();

    private static Set<Pattern> patterns(String... regexes) {
        Set<Pattern> set = new HashSet<Pattern>();
        for (String regex : regexes) {
            set.add(Pattern.compile(regex));
        }
        return set;
    }

    private static Set<Pattern> noPatterns() {
        return new HashSet<Pattern>();
    }

    // --- includePreference tests ---

    @Test
    public void testIncludePreference_noPatterns_includeByDefault() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("File should be included when includeByDefault=true and no patterns",
                DirPattern.includePreference(file, noPatterns(), noPatterns(), true));
    }

    @Test
    public void testIncludePreference_noPatterns_excludeByDefault() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("File should be excluded when includeByDefault=false and no patterns",
                DirPattern.includePreference(file, noPatterns(), noPatterns(), false));
    }

    @Test
    public void testIncludePreference_matchesExcludeOnly() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("File matching only an exclude pattern should be excluded",
                DirPattern.includePreference(file, patterns("server\\.xml"), noPatterns(), true));
    }

    @Test
    public void testIncludePreference_matchesIncludeOnly() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("File matching only an include pattern should be included",
                DirPattern.includePreference(file, noPatterns(), patterns("server\\.xml"), false));
    }

    @Test
    public void testIncludePreference_bothMatch_includeWins() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("Include pattern should override exclude in IncludePreference mode",
                DirPattern.includePreference(file, patterns("server\\.xml"), patterns("server\\.xml"), true));
    }

    @Test
    public void testIncludePreference_noMatch_fallsBackToIncludeByDefault_true() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("Non-matching file should fall back to includeByDefault=true",
                DirPattern.includePreference(file, patterns("bootstrap\\.properties"), patterns("jvm\\.options"), true));
    }

    @Test
    public void testIncludePreference_noMatch_fallsBackToIncludeByDefault_false() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("Non-matching file should fall back to includeByDefault=false",
                DirPattern.includePreference(file, patterns("bootstrap\\.properties"), patterns("jvm\\.options"), false));
    }

    // --- excludePreference tests ---

    @Test
    public void testExcludePreference_noPatterns_includeByDefault() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("File should be included when includeByDefault=true and no patterns",
                DirPattern.excludePreference(file, noPatterns(), noPatterns(), true));
    }

    @Test
    public void testExcludePreference_noPatterns_excludeByDefault() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("File should be excluded when includeByDefault=false and no patterns",
                DirPattern.excludePreference(file, noPatterns(), noPatterns(), false));
    }

    @Test
    public void testExcludePreference_bothMatch_excludeWins() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("Exclude pattern should override include in ExcludePreference mode",
                DirPattern.excludePreference(file, patterns("server\\.xml"), patterns("server\\.xml"), true));
    }

    @Test
    public void testExcludePreference_matchesIncludeOnly() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertTrue("File matching only an include pattern should be included",
                DirPattern.excludePreference(file, noPatterns(), patterns("server\\.xml"), false));
    }

    @Test
    public void testExcludePreference_matchesExcludeOnly() {
        File file = new File("/server/wlp/usr/servers/defaultServer/server.xml");
        assertFalse("File matching only an exclude pattern should be excluded",
                DirPattern.excludePreference(file, patterns("server\\.xml"), noPatterns(), true));
    }

    // --- Constructor / accessor tests ---

    @Test
    public void testConstructor_includeByDefault() {
        DirPattern dp = new DirPattern(DirPattern.INCLUDE_BY_DEFAULT, PatternStrategy.IncludePreference);
        assertTrue("includeByDefault should be true", dp.isIncludeByDefault());
        assertTrue("includePatterns should be empty on construction", dp.getIncludePatterns().isEmpty());
        assertTrue("excludePatterns should be empty on construction", dp.getExcludePatterns().isEmpty());
    }

    @Test
    public void testConstructor_excludeByDefault() {
        DirPattern dp = new DirPattern(DirPattern.EXCLUDE_BY_DEFAULT, PatternStrategy.ExcludePreference);
        assertFalse("includeByDefault should be false", dp.isIncludeByDefault());
    }
    
}
