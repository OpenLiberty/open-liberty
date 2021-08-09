/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirPattern {

    boolean includeByDefault;

    Set<Pattern> includePatterns;
    Set<Pattern> excludePatterns;
    PatternStrategy strategy;

    /**
     * An pattern to filter the directory content
     * 
     * @param includeByDefault if a file underneath base directory is included by default when no pattern apply to it.
     * @param strategy when a file matches both the includePattern and excludePattern, decide which take preference.
     */
    public DirPattern(boolean includeByDefault, PatternStrategy strategy) {
        this.includeByDefault = includeByDefault;

        this.strategy = strategy;
        this.includePatterns = new HashSet<Pattern>();
        this.excludePatterns = new HashSet<Pattern>();
    }

    public Set<Pattern> getIncludePatterns() {
        return includePatterns;
    }

    public Set<Pattern> getExcludePatterns() {
        return excludePatterns;
    }

    public PatternStrategy getStrategy() {
        return strategy;
    }

    public boolean isIncludeByDefault() {
        return includeByDefault;
    }

    /**
     * Simple regexp filter mechanism for controlling which directories and/or files are included in the zipfile.
     * Include Patterns override Exclude Patterns.
     * 
     * @param file
     * @param excludePattern
     * @param includePattern
     * @return if the file should be included.
     */
    static boolean includePreference(File file, Set<Pattern> excludePattern, Set<Pattern> includePattern, boolean includeByDefault) {
        boolean include = includeByDefault;

        // Iterate over exclude patterns, if there is any match, exclude
        if (include) {
            for (Pattern pattern : excludePattern) {
                Matcher excludeMatcher = pattern.matcher(file.getAbsolutePath());
                if (excludeMatcher.find()) {
                    include = false;
                    break;
                }
            }
        }

        // Iterate over include patterns, if there is any match, include
        if (!include) {
            for (Pattern pattern : includePattern) {
                Matcher includeMatcher = pattern.matcher(file.getAbsolutePath());
                if (includeMatcher.find()) {
                    include = true; // If we are here, we are overriding an exclude
                    break;
                }
            }
        }

        return include;
    }

    /**
     * Include if match the includePattern, then exclude that if it matches the excludePattern
     * 
     * @param file
     * @param excludePattern
     * @param includePattern
     * @return if the file should be included.
     */
    static boolean excludePreference(File file, Set<Pattern> excludePattern, Set<Pattern> includePattern, boolean includeByDefault) {
        boolean include = includeByDefault;

        // Iterate over include patterns, if there is any match, include
        if (!include) {
            for (Pattern pattern : includePattern) {
                Matcher includeMatcher = pattern.matcher(file.getAbsolutePath());
                if (includeMatcher.find()) {
                    include = true;
                    break;
                }
            }
        }

        // Iterate over exclude patterns, if there is any match, exclude
        if (include) {
            for (Pattern pattern : excludePattern) {
                Matcher excludeMatcher = pattern.matcher(file.getAbsolutePath());
                if (excludeMatcher.find()) {
                    include = false; // If we are here, we are overriding an include
                    break;
                }
            }
        }

        return include;
    }

    public enum PatternStrategy {
        IncludePreference, ExcludePreference;
    }

}