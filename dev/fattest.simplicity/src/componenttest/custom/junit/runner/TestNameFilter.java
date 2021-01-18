/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

/**
 * Test filter which compares the name of the class of a test method
 * against a filter value, and which compares the name of the test
 * method against a filter value.
 * 
 * The filter may be specified in two ways, either as a single fully
 * qualified method name, or as a qualified class name and method name.
 * 
 * The qualified class name and the qualified method name may be provided
 * by themselves, in which case the filter will ignore the portion of the
 * filter.
 *
 * Matching is done using {@link Pattern#matches(String, CharSequence)}.
 * 
 * When specifying a qualified class name or a method name, multiple
 * values may be specified, using ',' as a separator.
 * 
 * Multiple values should not be used when specifying a fully qualified
 * method name as the filter value.
 */
public class TestNameFilter extends Filter {
    public static final String FAT_TEST_QNAME_PROPERTY_NAME = "fat.test.qualified.name";
    public static final String FAT_TEST_CLASS_PROPERTY_NAME = "fat.test.class.name";
    public static final String FAT_TEST_METHOD_PROPERTY_NAME = "fat.test.method.name";

    private static final String FAT_TEST_QNAME;
    private static final String FAT_TEST_CLASS;
    private static final String[] FAT_TEST_CLASS_ELEMENTS;
    private static final String[] FAT_TEST_CLASS_EXPRESSIONS;
    
    private static final String FAT_TEST_METHOD;
    private static final String[] FAT_TEST_METHOD_ELEMENTS;
    private static final String[] FAT_TEST_METHOD_EXPRESSIONS;
    
    static {
        FAT_TEST_QNAME = System.getProperty(FAT_TEST_QNAME_PROPERTY_NAME);
        if ( FAT_TEST_QNAME != null ) {
            Log.info(TestNameFilter.class, "<clinit>", "Running only test method with qualified name: " + FAT_TEST_QNAME);
            int indx = FAT_TEST_QNAME.lastIndexOf('.');
            FAT_TEST_CLASS = FAT_TEST_QNAME.substring(0, indx);
            FAT_TEST_METHOD = FAT_TEST_QNAME.substring(indx + 1);
        } else {
            FAT_TEST_CLASS = System.getProperty(FAT_TEST_CLASS_PROPERTY_NAME);
            FAT_TEST_METHOD = System.getProperty(FAT_TEST_METHOD_PROPERTY_NAME);
        }

        if ( FAT_TEST_CLASS != null ) {
            FAT_TEST_CLASS_ELEMENTS = FAT_TEST_CLASS.split(",");
            FAT_TEST_CLASS_EXPRESSIONS = new String[ FAT_TEST_CLASS_ELEMENTS.length ];
            for ( int elementNo = 0; elementNo < FAT_TEST_CLASS_ELEMENTS.length; elementNo++ ) {
                FAT_TEST_CLASS_EXPRESSIONS[elementNo] = 
                    FAT_TEST_CLASS_ELEMENTS[elementNo].replace("*", ".*");
            }
            Log.info(TestNameFilter.class, "<clinit>", "Running only test class with name: " + FAT_TEST_CLASS);
        } else {
            FAT_TEST_CLASS_ELEMENTS = null;
            FAT_TEST_CLASS_EXPRESSIONS = null;
        }

        if ( FAT_TEST_METHOD != null ) {
            FAT_TEST_METHOD_ELEMENTS = FAT_TEST_METHOD.split(",");
            FAT_TEST_METHOD_EXPRESSIONS = new String[ FAT_TEST_METHOD_ELEMENTS.length ];
            for ( int elementNo = 0; elementNo < FAT_TEST_METHOD_ELEMENTS.length; elementNo++ ) {
                FAT_TEST_METHOD_EXPRESSIONS[elementNo] = 
                    FAT_TEST_METHOD_ELEMENTS[elementNo].replace("*", ".*");
            }
            Log.info(TestNameFilter.class, "<clinit>", "Running only test with method name: " + FAT_TEST_METHOD);
        } else {
            FAT_TEST_METHOD_ELEMENTS = null;
            FAT_TEST_METHOD_EXPRESSIONS = null;
        }
    }

    //

    /**
     * Answer a print string for this filter.
     * 
     * @return A print string for this filter.
     */
    @Override
    public String describe() {
        String filterValue;
        if ( FAT_TEST_CLASS == null ) {
            if ( FAT_TEST_METHOD == null ) {
                filterValue = "()";
            } else {
                filterValue = "(method=" + FAT_TEST_METHOD + ')';
            }
        } else if ( FAT_TEST_METHOD == null ) {
            filterValue = "(class=" + FAT_TEST_METHOD + ')';
        } else {
            filterValue ="class=" + FAT_TEST_METHOD + ',' + "method=" + FAT_TEST_METHOD + ')';
        }
        return "TestNameFilter" + filterValue;
    }

    /**
     * Tell if a test method should be run.
     * 
     * If a test class filter was specified, the class of the test method
     * must match the test class filter.
     * 
     * If a test method filter was specified, the test method must match
     * the test class filter. 
     *
     * @param desc The description of the test method which is to be checked.
     *
     * @return True or false, telling if a test method should be run.
     */
    @Override
    public boolean shouldRun(Description desc) {
        if ( (FAT_TEST_CLASS != null) &&
              !match(FAT_TEST_CLASS, FAT_TEST_CLASS_ELEMENTS, FAT_TEST_CLASS_EXPRESSIONS,
                     desc.getClassName()) ) {
            return false;
        }
        
        if ( (FAT_TEST_METHOD != null) &&
             !match(FAT_TEST_METHOD, FAT_TEST_METHOD_ELEMENTS, FAT_TEST_METHOD_EXPRESSIONS,
                    desc.getMethodName())) {
            return false;
        }
        
        return true;
    }

    /**
     * Tell if a value matches a filter value.
     * 
     * If the filter contains a ".", matching is against the entire
     * filter value.
     * 
     * If the filter does not contain a ".", matching is against the
     * last element of the value.
     * 
     * @param filter The raw filter value.
     * @param filterElements The parsed elements of the filter.
     * @param filterExpressions The parsed elements of the filter, as
     *    regular expressions.
     * @param value The value which is to be matched against the expression.
     *
     * @return True or false telling if the value matches the filter
     *     expression.
     */
    private boolean match(
        String filter,
        String[] filterElements, String[] filterExpressions,
        String value) {
        
        for ( int elementNo = 0; elementNo < filterElements.length; elementNo++ ) {
            String filterElement = filterElements[elementNo];
            String filterExpression = filterExpressions[elementNo];
            if ( filterElement.contains(".") && wildcardMatch(filterExpression, value) ) {
                return true;
            } else {
                int indx = value.lastIndexOf(".");
                String simpleName = value.substring(indx + 1);
                if ( wildcardMatch(filterExpression, simpleName) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tell if a filter expression matches a specified value.
     * 
     * @param filterExpression The expression which is to be matched against.
     * @param value The value which is to be matched against the expression.
     *
     * @return True or false telling if the value matches the filter
     *     expression.
     */
    private boolean wildcardMatch(String filterExpression, String value) {
        return Pattern.matches(filterExpression, value);
    }
}
