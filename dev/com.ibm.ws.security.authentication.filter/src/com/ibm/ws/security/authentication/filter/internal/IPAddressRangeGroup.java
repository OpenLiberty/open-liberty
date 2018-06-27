/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;

public class IPAddressRangeGroup {

    LinkedList ranges = new LinkedList();

    /**
     *  
     */
    public IPAddressRangeGroup() {}

    public void addRange(String rangeStr) throws FilterException {
        IPAddressRange range = new IPAddressRange(rangeStr);

        ranges.add(range);
    }

    /*
     * public boolean inRange(String ipStr) throws FilterException{ return
     * inRange(IPAddressRange.strToIP(ipStr)); }
     */

    public boolean inRange(InetAddress ip) throws FilterException {
        Iterator iter = ranges.iterator();
        while (iter.hasNext()) {
            IPAddressRange range = (IPAddressRange) iter.next();
            if (range.inRange(ip)) {
                return true;
            }
        }

        return false;
    }

}
