/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.d43.jdbc;

import java.sql.ShardingKeyBuilder;

import javax.sql.CommonDataSource;

public abstract class D43CommonDataSource implements CommonDataSource {
    D43ShardingKey defaultShardingKey;
    D43ShardingKey defaultSuperShardingKey;

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() {
        return new D43ShardingKeyBuilder();
    }

    public String getShardingKey() {
        return defaultShardingKey == null ? null : defaultShardingKey.key;
    }

    public String getSuperShardingKey() {
        return defaultSuperShardingKey == null ? null : defaultSuperShardingKey.key;
    }

    public void setShardingKey(String value) {
        defaultShardingKey = value == null || value.length() == 0 ? null : new D43ShardingKey(value);
    }

    public void setSuperShardingKey(String value) {
        defaultSuperShardingKey = value == null || value.length() == 0 ? null : new D43ShardingKey(value);
    }
}