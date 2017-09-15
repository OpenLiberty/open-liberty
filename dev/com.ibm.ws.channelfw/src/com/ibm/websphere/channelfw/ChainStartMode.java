/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.io.Serializable;

/**
 * The ChainStartMode object is an enumeration for the various
 * values/options for starting chains related to how error conditions
 * should be handled.
 */
public class ChainStartMode implements Comparable<ChainStartMode>, Serializable {

    /**
     * This is the current default mode of operation when chains are added. If this
     * is specified during chain start, and an error occurs, it should be silently
     * handled and program control should move on.
     */
    public static final ChainStartMode FAIL_EACH_SILENT;

    /**
     * This is the mode of operation that signals the caller of a chain start to
     * know that a retry may be successful if done a little later.
     */
    public static final ChainStartMode RETRY_EACH_ON_FAIL;

    /**
     * list of values
     */
    private static ChainStartMode _values[];

    /** Serialization ID string */
    private static final long serialVersionUID = -8795227916024523977L;

    static {
        _values = new ChainStartMode[2];
        _values[0] = (FAIL_EACH_SILENT = new ChainStartMode(0));
        _values[1] = (RETRY_EACH_ON_FAIL = new ChainStartMode(1));
    }

    /**
     * ordinal for this instance
     */
    private int _ordinal;

    /**
     * Constructor.
     * 
     * @param ordinal
     */
    private ChainStartMode(int ordinal) {
        this._ordinal = ordinal;
    }

    /**
     * Get the chain start mode definition for this ordinal.
     * 
     * @param ordinal
     * @return ChainStartMode, null if it does not match a defined instance
     */
    public static ChainStartMode getKey(int ordinal) {
        if (ordinal >= 0 && ordinal < _values.length) {
            return _values[ordinal];
        }
        return null;
    }

    /**
     * Get this ordinal.
     * 
     * @return int
     */
    public int getOrdinal() {
        return this._ordinal;
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ChainStartMode o) {
        if (o == null) {
            return -1;
        }
        return hashCode() - o.hashCode();
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || !(o instanceof ChainStartMode)) {
            return false;
        }
        return hashCode() == o.hashCode();
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this._ordinal;
    }
}
