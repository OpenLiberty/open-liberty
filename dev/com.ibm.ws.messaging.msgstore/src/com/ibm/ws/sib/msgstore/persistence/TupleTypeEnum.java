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
package com.ibm.ws.sib.msgstore.persistence;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;

/**
 * Enumerated type for {@link com.ibm.ws.sib.msgstore.persistence.impl.Tuple}
 * objects.
 *
 * @author pradine
 */
public class TupleTypeEnum {
    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
        
    /**
     * Item tuple. Usually represents the thing being stored.
     */
    public static final TupleTypeEnum ITEM = new TupleTypeEnum("II");
    
    /**
     * Item Reference tuple. Used as a pointer to {@link #ITEM} tuples
     */
    public static final TupleTypeEnum ITEM_REFERENCE = new TupleTypeEnum("RI");
    
    /**
     * Item Stream tuple. Contains other tuples such as {@link #ITEM},
     * and {@link #REFERENCE_STREAM}.
     */
    public static final TupleTypeEnum ITEM_STREAM = new TupleTypeEnum("IS");
    
    /**
     * Reference Stream tuple. Contains {@link #ITEM_REFERENCE} tuples.
     */
    public static final TupleTypeEnum REFERENCE_STREAM = new TupleTypeEnum("RS");
    
    /**
     * Root tuple. Represents the top of the hierarchy.
     */
    public static final TupleTypeEnum ROOT = new TupleTypeEnum("RT");
    
    private String type;
    
    /*
     * Private constructor in order to prevent instantiation
     */
    private TupleTypeEnum(String type) {
        this.type = type;
    }
    
    /**
     * Factory method for <code>TupleTypeEnum</code> objects.
     * 
     * @param type the string corresponding to the type of <code>TupleTypeEnum</code> that you want.
     * @return a <code>TupleTypeEnum</code> object.
     */
    public static TupleTypeEnum getInstance(String type) {
        if (type.equals(ITEM.type))
            return ITEM;
        else if (type.equals(ITEM_REFERENCE.type))
            return ITEM_REFERENCE;
        else if (type.equals(ITEM_STREAM.type))
            return ITEM_STREAM;
        else if (type.equals(REFERENCE_STREAM.type))
            return REFERENCE_STREAM;
        else if (type.equals(ROOT.type))
            return ROOT;
        else
            throw new IllegalArgumentException(nls.getFormattedMessage("INVALID_TUPLE_TYPE_SIMS1502",
                                                                       new Object[] {type},
                                                                       null));
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return type;
    }
}
