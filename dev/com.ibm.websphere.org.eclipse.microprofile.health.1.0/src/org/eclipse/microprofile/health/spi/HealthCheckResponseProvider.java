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

package org.eclipse.microprofile.health.spi;

import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

/**
 * <p>
 * Reserved for implementors as means to supply their own HealthCheckResponseBuilder. This provider is located using the default
 * service loader and instantiated from the {@link org.eclipse.microprofile.health.HealthCheckResponse}
 * </p>
 * Created by hbraun on 07.07.17.
 */
public interface HealthCheckResponseProvider {

    HealthCheckResponseBuilder createResponseBuilder();

}