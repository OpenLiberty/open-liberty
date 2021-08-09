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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HealthCheckResponseBuilderImpl extends HealthCheckResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheckResponseBuilderImpl.class);

    private String name;

    private final Optional<Map<String, Object>> data = Optional.of(new HashMap<String, Object>());
    private State state;

    public HealthCheckResponseBuilderImpl() {}

    @Override
    public HealthCheckResponseBuilder name(String name) {
        validateName(name);
        this.name = name;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, String value) {
        return withData(key, (Object) value);
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, long value) {
        return withData(key, Long.valueOf(value));
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, boolean value) {
        return withData(key, Boolean.valueOf(value));
    }

    @Override
    public HealthCheckResponseBuilder up() {
        this.state = State.UP;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        this.state = State.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder state(boolean up) {
        if (up)
            this.state = State.UP;
        else
            this.state = State.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        HealthCheckResponse response = new HealthCheckResponseImpl(name, state, data);
        return response;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HealthCheckResponseBuilder: ");
        builder.append("\n\t");
        builder.append("name");
        builder.append(" = ");
        builder.append(name);
        builder.append("\n\t");
        builder.append("state");
        builder.append(" = ");
        builder.append(state);
        builder.append("\n\t");
        builder.append("data");
        builder.append(" = ");
        builder.append(data);

        return builder.toString();
    }

    private HealthCheckResponseBuilder withData(String key, Object value) {
        validateKey(key);

        if (data.isPresent())
            data.get().put(key, value);

        return this;
    }

    private void validateName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "Name is null"));
        }
    }

    private void validateKey(String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "Key is null"));
        }
    }

}