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
/*
 * Change activity:
 *
 * Reason          Date        Origin       Description
 * --------------- ----------  -----------  --------------------------------------------
 *                 27/10/2003  van Leersum  Original
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.putget;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStoreException;

/**
 * @author DrPhill
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SkippingFilter implements Filter {

    private final int _skip;
    /**
     * 
     */
    public SkippingFilter(int skip) {
        super();
        _skip = skip;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.Filter#filterMatches(com.ibm.ws.sib.msgstore.AbstractItem)
     */
    public boolean filterMatches(AbstractItem abstractItem) throws MessageStoreException {
        boolean match = true;
        if (_skip > 0) {
            int i = ((Item)abstractItem).getMySequence();
            match =  (i % _skip) == 0;
        }
        return match;
    }

}
