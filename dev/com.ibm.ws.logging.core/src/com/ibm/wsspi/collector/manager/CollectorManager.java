/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.collector.manager;

import java.util.List;

public interface CollectorManager {

    /**
     * A handler calls this method to subscribe itself to receive events from the specified sources.
     * Collector manager does the required boot strapping to ensure that relevant events are sent to this handler.
     * <br>
     * Note:
     * <br>
     * 1) If a source is not available during subscription it is the responsibility of the collector manager to
     * ensure that whenever that source becomes available it starts sending events to the interested handler.
     * <br>
     * 2) Only handlers bound to collector manager will be allowed to subscribe.
     * 
     * @param handler Handler instance.
     * @param sourceIds List of source identifiers.
     * @throws Exception
     */
    void subscribe(Handler handler, List<String> sourceIds) throws Exception;

    /**
     * A handler calls this method to unsubscribe itself from the specified sources. Collector manager makes the necessary
     * changes so that this handler stops receiving events from the specified sources.
     * <br>
     * Note:
     * <br>
     * 1) Collector manager will stop a source when it detects that the last handler for a particular
     * source goes away.
     * <br>
     * 2) Only handlers bound to collector manager will be allowed to unsubscribe.
     * 
     * @param handler Handler instance.
     * @param sourceIds List of source identifiers.
     * @throws Exception List of source identifiers.
     */
    void unsubscribe(Handler handler, List<String> sourceIds) throws Exception;
}
