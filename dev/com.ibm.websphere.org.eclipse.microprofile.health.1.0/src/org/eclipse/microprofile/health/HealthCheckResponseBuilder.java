/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.microprofile.health;

/**
 * A builder to construct a health procedure response.
 *
 * <p>
 * The HealthCheckResponseBuilder class is reserved for an extension by implementation providers.
 * </p>
 */
public abstract class HealthCheckResponseBuilder {

    public abstract HealthCheckResponseBuilder name(String name);

    public abstract HealthCheckResponseBuilder withData(String key, String value);

    public abstract HealthCheckResponseBuilder withData(String key, long value);

    public abstract HealthCheckResponseBuilder withData(String key, boolean value);

    public abstract HealthCheckResponseBuilder up();

    public abstract HealthCheckResponseBuilder down();

    public abstract HealthCheckResponseBuilder state(boolean up);

    public abstract HealthCheckResponse build();

}