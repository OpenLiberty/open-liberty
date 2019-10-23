/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import java.util.function.Predicate;

public class OASProviderFilterBuilder {

    private Predicate<OASProviderWrapper> filter;
    private boolean findFirst;

    public OASProviderFilterBuilder() {
        filter = (x) -> true;
    }

    public OASProviderFilterBuilder addPredicate(Predicate<OASProviderWrapper> p) {
        filter = filter.and(p);
        return this;
    }

    public Predicate<OASProviderWrapper> getPredicate() {
        return filter;
    }

    public OASProviderFilterBuilder setFindFirst(boolean b) {
        findFirst = b;
        return this;
    }

    public boolean getFindFirst() {
        return findFirst;
    }

    public static Predicate<OASProviderWrapper> publicFilter() {
        return (x) -> x.isPublic();
    }

    public static Predicate<OASProviderWrapper> contextRootFilter(String contextRoot) {
        return (x) -> x.getContextRoot() != null && x.getContextRoot().startsWith(contextRoot);
    }
}
