/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

public class TestNameFilter extends Filter {

    private static final String FAT_TEST_QNAME;
    private static final String FAT_TEST_CLASS;
    private static final String FAT_TEST_METHOD;

    static {

        // QName has precedence over class or class+method names
        FAT_TEST_QNAME = System.getProperty("fat.test.qualified.name");
        if (FAT_TEST_QNAME != null) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test method with qualified name: " + FAT_TEST_QNAME);
            int indx = FAT_TEST_QNAME.lastIndexOf('.');
            FAT_TEST_CLASS = FAT_TEST_QNAME.substring(0, indx);
            FAT_TEST_METHOD = FAT_TEST_QNAME.substring(indx + 1);
        } else {
            //these properties allow shortcutting to a single test class and/or method
            FAT_TEST_CLASS = System.getProperty("fat.test.class.name");
            FAT_TEST_METHOD = System.getProperty("fat.test.method.name");
        }

        if (FAT_TEST_CLASS != null) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test class with name: " + FAT_TEST_CLASS);
        }
        if (FAT_TEST_METHOD != null) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test with method name: " + FAT_TEST_METHOD);
        }
    }

    @Override
    public String describe() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shouldRun(Description arg0) {
        if (FAT_TEST_CLASS != null && !match(FAT_TEST_CLASS, arg0.getClassName())) {
            return false;
        }
        if (FAT_TEST_METHOD != null && !match(FAT_TEST_METHOD, arg0.getMethodName())) {
            return false;
        }
        return true;
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
