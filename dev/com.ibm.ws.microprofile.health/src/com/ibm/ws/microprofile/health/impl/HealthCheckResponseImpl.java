/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.health.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Health
 */
public class HealthCheckResponseImpl extends HealthCheckResponse {

    private static final TraceComponent tc = Tr.register(HealthCheckResponseImpl.class);

    private final String name;
    private final State state;
    private final Optional<Map<String, Object>> data;

    public HealthCheckResponseImpl(String name, State state, Optional<Map<String, Object>> data) {
        this.name = name;
        this.state = state;
        this.data = data;
    }

    @Override
    public String getName() {
        if (this.name == null || this.name.length() == 0)
            throw new IllegalArgumentException(Tr.formatMessage(tc, "Name is null"));
        else
            return name;
    }

    @Override
    public State getState() {
        if (state == null || (state != State.UP && state != State.DOWN))
            throw new IllegalArgumentException(Tr.formatMessage(tc, "State is null"));
        else
            return state;
    }

    @Override
    public Optional<Map<String, Object>> getData() {
        if (data.isPresent()) {
            Set<String> keys = data.get().keySet();
            Iterator<String> keysIter = keys.iterator();
            while (keysIter.hasNext()) {
                String key = keysIter.next();
                if (key == null || key.length() == 0) {
                    throw new IllegalArgumentException(Tr.formatMessage(tc, "Key is null"));
                }
            }
        }

        return data;
    }

    public static HealthCheckResponseBuilder named(String sName) {
        HealthCheckResponseBuilder builder = null;

        if (sName != null && sName.length() > 0) {
            builder = HealthCheckResponse.named(sName);
            builder.name(sName);
        } else
            throw new IllegalArgumentException(Tr.formatMessage(tc, "Name is null"));
        return builder;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HealthCheckResponse: ");
        builder.append("\n\t");
        builder.append("name");
        builder.append(" = ");
        builder.append(this.name);
        builder.append("\n\t");
        builder.append("state");
        builder.append(" = ");
        builder.append(this.state);
        builder.append("\n\t");
        builder.append("data");
        builder.append(" = ");
        builder.append(this.data);

        return builder.toString();
    }

}
