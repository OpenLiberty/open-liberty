/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package io.openliberty.wsoc.util.wsoc;

/**
 * Taken from io.openliberty.wsoc.internal_fat
 */
public class MultiClientTestContext {

    private WsocTestContext[] _receivers = null;

    private WsocTestContext _publisher = null;

    private boolean _testTimedout = false;

    public void setReceiverContexts(WsocTestContext[] receivers) {
        _receivers = receivers;
    }

    public WsocTestContext[] getReceivers() {
        return _receivers;
    }

    public void setPublisherContext(WsocTestContext publisher) {
        _publisher = publisher;
    }

    public WsocTestContext getPublisher() {
        return _publisher;
    }

    public void setTestTimedout(boolean timedout) {
        _testTimedout = timedout;
    }

    public boolean getTestTimedout() {
        return _testTimedout;
    }

}
