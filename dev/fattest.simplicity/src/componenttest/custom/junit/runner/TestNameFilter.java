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
package componenttest.custom.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

/**
 *
 */
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
        if (FAT_TEST_CLASS != null && !arg0.getClassName().equals(FAT_TEST_CLASS)) {
            return false;
        }
        if (FAT_TEST_METHOD != null && !arg0.getMethodName().equals(FAT_TEST_METHOD)) {
            return false;
        }
        return true;
    }

}
