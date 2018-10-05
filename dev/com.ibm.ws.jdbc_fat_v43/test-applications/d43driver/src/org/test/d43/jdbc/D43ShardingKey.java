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

import java.sql.ShardingKey;

public class D43ShardingKey implements ShardingKey {
    final String key;

    D43ShardingKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object shardingKey) {
        return shardingKey instanceof D43ShardingKey && ((D43ShardingKey) shardingKey).key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("D43ShardingKey@")
                        .append(Integer.toHexString(hashCode()))
                        .append('|')
                        .append(key)
                        .toString();
    }
}