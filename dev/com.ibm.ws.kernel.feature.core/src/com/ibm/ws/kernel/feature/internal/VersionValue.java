/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.ArrayList;
import java.util.List;

public class VersionValue {
    private static final VersionValue singleton = new VersionValue();
    private static final ThreadLocal<List<String>> values = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    public static VersionValue valueOf(String value) {
        values.get().add(value);
        return singleton;
    }

    private VersionValue() {

    }

    public static VersionValue getInstance() {
        return singleton;
    }

    public static List<String> getValues() {
        List<String> copy = new ArrayList<String>(values.get());
        values.get().clear();
        return copy;
    }
}
