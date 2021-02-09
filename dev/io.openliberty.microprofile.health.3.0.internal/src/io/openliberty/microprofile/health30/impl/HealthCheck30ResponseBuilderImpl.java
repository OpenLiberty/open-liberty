/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package io.openliberty.microprofile.health30.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HealthCheck30ResponseBuilderImpl extends HealthCheckResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheck30ResponseBuilderImpl.class);

    private String name;

    private final Optional<Map<String, Object>> data = Optional.of(new HashMap<String, Object>());
    private Status status;

    public HealthCheck30ResponseBuilderImpl() {}

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
        this.status = Status.UP;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        this.status = Status.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder status(boolean up) {
        if (up)
            this.status = Status.UP;
        else
            this.status = Status.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, status, data);
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
        builder.append("status");
        builder.append(" = ");
        builder.append(status);
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