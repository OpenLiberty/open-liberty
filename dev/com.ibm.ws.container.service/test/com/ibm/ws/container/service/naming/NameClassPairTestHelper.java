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
package com.ibm.ws.container.service.naming;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NameClassPair;

public class NameClassPairTestHelper {
    public static Set<NameClassPairTestHelper> newSet(NameClassPair... pairs) {
        return newSet(Arrays.asList(pairs));
    }

    public static Set<NameClassPairTestHelper> newSet(Collection<? extends NameClassPair> pairs) {
        Set<NameClassPairTestHelper> keys = new HashSet<NameClassPairTestHelper>();
        for (NameClassPair pair : pairs) {
            keys.add(new NameClassPairTestHelper(pair));
        }
        return keys;
    }

    private final NameClassPair pair;

    public NameClassPairTestHelper(NameClassPair pair) {
        this.pair = pair;
    }

    @Override
    public String toString() {
        return '[' + pair.getName() + "=" + pair.getClassName() + ']';
    }

    @Override
    public int hashCode() {
        return pair.getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != NameClassPairTestHelper.class) {
            return false;
        }

        NameClassPairTestHelper helper = (NameClassPairTestHelper) o;
        return pair.getName().equals(helper.pair.getName()) && pair.getClassName().equals(helper.pair.getClassName());
    }
}
