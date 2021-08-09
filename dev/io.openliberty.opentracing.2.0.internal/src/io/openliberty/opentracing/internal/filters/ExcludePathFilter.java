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
package io.openliberty.opentracing.internal.filters;

import java.net.URI;

/**
 *
 */
public class ExcludePathFilter extends ExcludeFilter {

    /**
     * @param pattern
     * @param type
     * @param ignoreCase
     * @param regex
     */
    public ExcludePathFilter(String pattern, SpanFilterType type, boolean ignoreCase, boolean regex) {
        super(pattern, type, ignoreCase, regex);
    }

    /** {@inheritDoc} */
    @Override
    public boolean process(boolean previousState, URI uri, String path, SpanFilterType contextType) {
        if (contextApplies(contextType) && match(path)) {
            return matchAction(previousState, uri, path, contextType);
        }
        return previousState;
    }

}
