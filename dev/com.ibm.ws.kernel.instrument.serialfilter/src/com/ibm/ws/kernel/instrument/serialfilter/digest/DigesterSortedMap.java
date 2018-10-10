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
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

class DigesterSortedMap<D extends Digester> extends TreeMap<String, D> implements Iterable<Map.Entry<String, D>> {
    @Override
    public Iterator<Map.Entry<String, D>> iterator() {
        return entrySet().iterator();
    }
}
