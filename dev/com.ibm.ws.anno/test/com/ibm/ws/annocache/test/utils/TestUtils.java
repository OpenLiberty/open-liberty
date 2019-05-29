/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.utils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;

public class TestUtils {

    public static void prepareDirectory(String targetPath) {
        File targetFile = new File(targetPath);
        String fullTargetPath = targetFile.getAbsolutePath();

        int failedRemovals = UtilImpl_FileUtils.unprotectedRemoveAll(null, targetFile);
        Assert.assertEquals("Removal of [ " + fullTargetPath + " ]", failedRemovals, 0);

        targetFile.mkdirs();
        Assert.assertTrue("Creation of [ " + fullTargetPath + " ]", targetFile.exists());
        Assert.assertTrue("Created as path [ " + fullTargetPath + " ]", targetFile.isDirectory());
    }

    public static String toString(Collection <? extends Object> values) {
        int numValues = values.size();
        if ( numValues == 0 ) {
            return "[]";

        } else if ( numValues == 1 ) {
            String valueText = null;
            for ( Object value : values ) {
                valueText = "[" + value + "]";
            }
            return valueText;

        } else {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            boolean isFirst = true;
            for ( Object value : values ) {
                if ( isFirst ) {
                    isFirst = false;
                } else {
                    builder.append(',');
                }
                builder.append(value);
            }
            builder.append(']');
            return builder.toString();
        }
    }

    public static Set<String> asSet(String[] valueArray) {
        Set<String> valueSet = new HashSet<String>(valueArray.length);
        for ( String value : valueArray ) {
            valueSet.add(value);
        }
        return valueSet;
    }

    private static final String ANNOCACHE_PREFIX = "com.ibm.ws.annocache.";

    public static Set<String> filter(Set<String> classNames) {
        Set<String> filteredClassNames = new HashSet<String>( classNames.size() );
        for ( String className : classNames ) {
            if ( className.startsWith(ANNOCACHE_PREFIX) ) {
                filteredClassNames.add(className);
            }
        }
        return filteredClassNames;
    }
}
