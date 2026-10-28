/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.suite;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.TestNameFilter;

/**
 * Tests whether a suite class and all of its child tests are filtered out by the fat.test.class.name property
 */
public class SuiteTestNameFilter extends Filter {

    private static final String FAT_TEST_QNAME;
    private static final String FAT_TEST_CLASS;

    static {

        // QName has precedence over class or class+method names
        FAT_TEST_QNAME = System.getProperty("fat.test.qualified.name");
        if (FAT_TEST_QNAME != null) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test method with qualified name: " + FAT_TEST_QNAME);
            int indx = FAT_TEST_QNAME.lastIndexOf('.');
            FAT_TEST_CLASS = FAT_TEST_QNAME.substring(0, indx);
        } else {
            //these properties allow shortcutting to a single test class and/or method
            FAT_TEST_CLASS = System.getProperty("fat.test.class.name");
        }

        if (FAT_TEST_CLASS != null) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test class with name: " + FAT_TEST_CLASS);
        }
    }

    @Override
    public String describe() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldRun(Description description) {
        if (FAT_TEST_CLASS != null) {
            Class<?> suiteClass = description.getTestClass();
            Set<Class<?>> classesToCheck = new HashSet<>();
            classesToCheck.add(suiteClass);
            classesToCheck.addAll(getSuiteChildren(suiteClass));
            return classesToCheck.stream().anyMatch(c -> match(FAT_TEST_CLASS, c.getName()));
        } else {
            return true;
        }
    }

    private List<Class<?>> getSuiteChildren(Class<?> suiteClass) {
        SuiteClasses suiteClasses = suiteClass.getAnnotation(SuiteClasses.class);
        if (suiteClasses == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(suiteClasses.value());
    }

    private boolean match(String list, String arg) {
        for (String s : list.split(",")) {
            if (s.contains(".") && wildcardMatch(s, arg)) {
                return true;
            } else {
                int indx = arg.lastIndexOf(".");
                String tmp = arg.substring(indx + 1);

                if (wildcardMatch(s, tmp)) {
                    return true;
                }
            }
        }
        return false;
    }

    // normal compare when no '*'
    private boolean wildcardMatch(String glob, String arg) {
        return Pattern.matches(glob.replace("*", ".*"), arg);
    }

}
