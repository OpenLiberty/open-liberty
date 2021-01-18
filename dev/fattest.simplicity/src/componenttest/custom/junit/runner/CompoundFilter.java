/*******************************************************************************
 * Copyright (c) 2013, 2013 IBM Corporation and others.
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

/**
 * Composition filter.
 * 
 * A composite filter runs a test if and only if all of its member
 * filters run the test.  If any member filter skips the test, then
 * the composite filter will skip the test.
 *
 * An empty compound filter runs all tests.
 *
 * This filter is used to work-around a JUnit bug, which is that
 * only the last filter is used.
 *
 * TFB: TODO: This should be verified to still be a problem in
 * the current version of JUnit.
 */
public class CompoundFilter extends Filter {

    /**
     * Create a compound filter on an array of filters.
     *
     * @param filters The element filters of the new compound filter.
     */
    public CompoundFilter(Filter[] filters) {
        this.filters = filters;
    }

    /** The element filters of this compound filter. */
    private final Filter[] filters;

    /**
     * Describe this compound filter.
     *
     * @return A print string of this filter.
     */    
    @Override
    public String describe() {
        if ( filters.length == 0 ) {
            return "CompoundFilter()";

        } else if ( filters.length == 1 ) {
            return "CompoundFilter(" + filters[0].describe() + ")";

        } else {
            StringBuilder builder = new StringBuilder("CompoundFilter(");

            builder.append( filters[0].describe() );
            for ( int filterNo = 1; filterNo < filters.length; filterNo++ ) {
                builder.append(',');
                builder.append( filters[filterNo].describe() );
            }

            builder.append(')');

            return builder.toString();
        }
    }

    /**
     * Tell if a test method should be run.
     * 
     * A test method is run if and only if all of the element filters
     * of this compound filter run the test method.  (An empty compound
     * filter runs all test methods.)
     * 
     * @return True or false telling if the test method is to be run.
     */
    @Override
    public boolean shouldRun(Description desc) {
        for ( Filter filter : filters ) {
            if ( !filter.shouldRun(desc) ) {
                return false;
            }
        }
        return true;
    }
}
