/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A reporter class that keeps track of unhealthy services.
 * Could also keep track of healthy services.
 */
public class ExternalTestServiceReporter {

    private static final Class<?> c = ExternalTestServiceReporter.class;

    private static final ConcurrentHashMap<String, Collection<String>> UNHEALTHY = new ConcurrentHashMap<>();

    /**
     * This method should be used to report the instance as not working locally and
     * will not be randomly selected again unless no other options remain.
     *
     * @param service the unhealthy service
     * @param reason  the exception that explains why the service is unhealthy
     */
    public static void reportUnhealthy(ExternalTestService service, Throwable reason) {
        reportUnhealthy(service, reason.getMessage());
    }

    /**
     * This method should be used to report the instance as not working locally and
     * will not be randomly selected again unless no other options remain
     *
     * @param service the unhealthy service
     * @param reason  the reason
     */
    public static void reportUnhealthy(ExternalTestService service, String reason) {

        final String serviceName = service.getServiceName();
        final String address = service.getAddress();

        Log.info(c, "reportUnhealthy", "The " + serviceName + " service at " + address + " reported as unhealthy because: " + reason);

        UNHEALTHY.computeIfPresent(serviceName, (key, collection) -> {
            collection.add(address);
            return collection;
        });

        UNHEALTHY.computeIfAbsent(serviceName, (key) -> {
            return new HashSet<String>(Arrays.asList(address));
        });

    }

    /**
     * Returns an unmodifiable collection of unhealthy service reports for the
     * named service. This collection may be concurrently changing.
     *
     * @param  serviceName the service name to be reported
     * @return             the unmodifiable collection
     */
    public static Collection<String> getUnhealthyReport(String serviceName) {
        return Collections.unmodifiableCollection(UNHEALTHY.computeIfAbsent(serviceName, key -> new HashSet<String>()));
    }
}
