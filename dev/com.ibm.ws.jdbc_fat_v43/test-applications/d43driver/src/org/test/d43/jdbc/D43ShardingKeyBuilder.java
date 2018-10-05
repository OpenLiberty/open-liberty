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

import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.ShardingKey;
import java.sql.ShardingKeyBuilder;

public class D43ShardingKeyBuilder implements ShardingKeyBuilder {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public ShardingKey build() throws SQLException {
        return new D43ShardingKey(sb.toString());
    }

    @Override
    public D43ShardingKeyBuilder subkey(Object subkey, SQLType subkeyType) {
        sb.append(subkeyType.getName()).append(':').append(subkey).append(';');
        return this;
    }
}