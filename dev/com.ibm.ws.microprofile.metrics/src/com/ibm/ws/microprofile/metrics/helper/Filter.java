/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;

/**
 *
 */
public class Filter implements MetricFilter {
    private final Pattern p;

    public Filter(String regex) {
        p = Pattern.compile(regex);
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(String name, Metric metric) {
        // TODO Auto-generated method stub
        Matcher m = p.matcher(name);
        return m.matches();
    }

}
