/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
