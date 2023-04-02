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
package com.ibm.ws.channelfw.testsuite.channels.server;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundApplicationLink;

/**
 * Inbound application test link object.
 */
@SuppressWarnings("unused")
public class AppDummyLink2 extends InboundApplicationLink {

    /**
     * Constructor.
     */
    public AppDummyLink2() {
        super();
    }

    public void destroy(Exception e) {
        // nothing
    }

    public void ready(VirtualConnection inVC) {
        init(inVC);
    }

}
