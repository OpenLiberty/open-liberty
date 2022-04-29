/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

public class BoundedHashMap extends BoundedGenericHashMap<String, Object> {

    private static final long serialVersionUID = 7306671418293201026L;

    public BoundedHashMap(int maxEntries) {
        super(maxEntries);
    }

    public BoundedHashMap(int initSize, int maxEntries) {
        super(initSize, maxEntries);
    }

    public BoundedHashMap() {
        super();
    }

}
