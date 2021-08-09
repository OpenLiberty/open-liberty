/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.statistics;

import java.math.BigInteger;
import java.util.Date;

import com.ibm.oauth.core.api.statistics.OAuthStatistic;

public class OAuthStatisticImpl implements OAuthStatistic {

    String _name;
    long _count;
    BigInteger _elapsedTime;
    Date _timestamp;

    public OAuthStatisticImpl(String name) {
        init(name, 0, null, null);
    }

    public OAuthStatisticImpl(OAuthStatisticImpl other) {
        init(other._name, other._count, other._elapsedTime, other._timestamp);
    }

    public void setToNow() {
        _timestamp = new Date();
    }

    public void addMeasurement(long elapsedTimeMilliseconds) {
        _count++;
        _elapsedTime = _elapsedTime.add(BigInteger.valueOf(elapsedTimeMilliseconds));
    }

    void init(String name, long count, BigInteger elapsedTime, Date timestamp) {
        _name = name;
        _count = count;
        if (elapsedTime != null) {
            _elapsedTime = new BigInteger(elapsedTime.toByteArray());
        } else {
            _elapsedTime = BigInteger.ZERO;
        }
        if (timestamp != null) {
            _timestamp = timestamp;
        } else {
            _timestamp = new Date();
        }

    }

    public long getCount() {
        return _count;
    }

    public BigInteger getElapsedTime() {
        return _elapsedTime;
    }

    public String getName() {
        return _name;
    }

    public Date getTimestamp() {
        return _timestamp;
    }

}
