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
package com.ibm.ws.health.center.data;

import java.util.HashMap;

public class HCRecData {

    private final HashMap<Integer, String> recommendations;
    private final long timestamp;

    public HCRecData(HashMap<Integer, String> recommendations, long timestamp) {
        this.recommendations = recommendations;
        this.timestamp = timestamp;
    }

    public HashMap<Integer, String> getRecommendations() {
        return recommendations;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "HCRecData [\nrecommendations=" + recommendations + ", \ntimestamp=" + timestamp + "\n]";
    }

}
