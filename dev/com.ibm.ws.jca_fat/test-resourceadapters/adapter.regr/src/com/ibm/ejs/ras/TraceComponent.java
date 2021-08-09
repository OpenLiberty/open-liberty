/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

public class TraceComponent {

    static final String[] EMPTY_STRING_ARRAY = new String[0];

    private int fineLevelsEnabled;

    protected TraceComponent(String name, Class<?> aClass, String group,
                             String bundle) {
        this(name, aClass, group == null ? EMPTY_STRING_ARRAY : new String[] { group }, false, bundle);
    }

    /**
     * @param name
     * @param aClass
     * @param strings
     * @param b
     * @param bundle
     */
    public TraceComponent(String name, Class<?> aClass, String[] strings,
                          boolean b, String bundle) {}

    public final boolean isDebugEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_DEBUG)) != 0;
    }

    public final boolean isEntryEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_ENTRY_EXIT)) != 0;
    }

    public final boolean isEventEnabled() {
        return (this.fineLevelsEnabled & (1 << TrLevelConstants.TRACE_LEVEL_EVENT)) != 0;
    }

}
