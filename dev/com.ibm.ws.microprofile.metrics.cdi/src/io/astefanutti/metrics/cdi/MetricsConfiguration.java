/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.astefanutti.metrics.cdi;

/**
 * The Metrics CDI configuration. Metrics CDI fires a {@code MetricsConfiguration} event
 * during the deployment phase that the application can observe and use to configure it.
 *
 * Note that the event fired can only be used within the observer method invocation context. Any attempt to call one of its methods outside of that context will result in an
 * `IllegalStateException` to be thrown.
 */
public interface MetricsConfiguration {

    /**
     * Overrides the Metrics annotation {@code absolute} attribute values globally for the application to use metric absolute names.
     *
     * @return this Metrics CDI configuration
     * @throws IllegalStateException if called outside of the observer method invocation
     */
    MetricsConfiguration useAbsoluteName(boolean useAbsoluteName);
}
