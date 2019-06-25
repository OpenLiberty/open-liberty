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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/* package-private */ final class MetricsConfigurationEvent implements MetricsConfiguration {

    private final EnumSet<MetricsParameter> configuration = EnumSet.noneOf(MetricsParameter.class);

    private volatile boolean unmodifiable;

    @Override
    public MetricsConfiguration useAbsoluteName(boolean useAbsoluteName) {
        throwsIfUnmodifiable();
        if (useAbsoluteName)
            configuration.add(MetricsParameter.useAbsoluteName);
        else
            configuration.remove(MetricsParameter.useAbsoluteName);
        return this;
    }

    Set<MetricsParameter> getParameters() {
        return Collections.unmodifiableSet(configuration);
    }

    void unmodifiable() {
        unmodifiable = true;
    }

    private void throwsIfUnmodifiable() {
        if (unmodifiable)
            throw new IllegalStateException("Metrics CDI configuration event must not be used outside its observer method!");
    }
}
