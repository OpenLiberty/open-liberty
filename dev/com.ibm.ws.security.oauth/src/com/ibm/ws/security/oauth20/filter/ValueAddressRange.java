/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

public class ValueAddressRange implements IValue {
    private IPAddressRange range;

    /**
     * 
     */
    public ValueAddressRange(String ipRange) throws FilterException {
        super();
        range = new IPAddressRange(ipRange);
    }

    public boolean equals(IValue ip) throws FilterException {
        return range.inRange(((ValueIPAddress) ip).getIP());
    }

    public boolean greaterThan(IValue ip) throws FilterException {

        return range.aboveRange(((ValueIPAddress) ip).getIP());
    }

    public boolean lessThan(IValue ip) throws FilterException {
        return range.belowRange(((ValueIPAddress) ip).getIP());
    }

    /**
     * Is the IP address within the range (this). In some sense this is the opposite of the usual
     * interpretation of the containedBy() function. However, it's the best I can think of. If, you 
     * think of the string comparisons as asking the question "does the input match the value" where the value
     * has an implicit wildard at the beginning and end, then the interpretation here is essentially the same.
     * Yes, this is strained, but it's the best I can think of for now.
     */
    public boolean containedBy(IValue ip) throws FilterException {
        return range.inRange(((ValueIPAddress) ip).getIP());
    }
}
