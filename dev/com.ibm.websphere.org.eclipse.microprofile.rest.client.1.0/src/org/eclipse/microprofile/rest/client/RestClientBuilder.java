/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.rest.client;

import javax.annotation.Priority;
import javax.ws.rs.core.Configurable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.ToIntFunction;

/**
 * <p>
 * This is the main entry point for creating a Type Safe Rest Client.  Implementations are expected
 * to implement this class and register a service provider via {@link ServiceLoader} that works within their implementation.
 * <p>
 * Invoking <pre>RestClientBuilder.newBuilder()</pre> is intended to always create a new instance, not use a cached version.
 * <p>
 * If multiple implementations of RestClientBuilder are discovered, the one with the highest value of the {@link Priority} annotation is used
 * <p>
 * The {@link ServiceLoader} will first search via the current Thread's Context ClassLoader, then {@link RestClientBuilder}'s {@link ClassLoader}
 * <p>
 * The <pre>RestClientBuilder</pre> is a {@link Configurable} class as defined by JAX-RS.  This allows a user to register providers,
 * implementation specific configuration.
 */
public abstract class RestClientBuilder implements Configurable<RestClientBuilder>{
    public static RestClientBuilder newBuilder() {
        ServiceLoader<RestClientBuilder> loader = ServiceLoader.load(RestClientBuilder.class);
        List<RestClientBuilder> clientBuilders = new ArrayList<>();
        loader.forEach(clientBuilders::add);
        loader = ServiceLoader.load(RestClientBuilder.class, RestClientBuilder.class.getClassLoader());
        loader.forEach(clientBuilders::add);

        if(clientBuilders.size() == 0) {
            throw new RuntimeException("No implementation of '"+RestClientBuilder.class.getSimpleName()+"' found");
        }
        clientBuilders.sort(Comparator.comparingInt(priorityComparator()).reversed());
        return clientBuilders.get(0);
    }

    /**
     * Specifies the base URL to be used when making requests.  Assuming that the interface has a <pre>@Path("/api")</pre> at the interface level
     * and a <pre>url</pre> is given with <pre>http://my-service:8080/service</pre> then all REST calls will be invoked with a <pre>url</pre> of
     * <pre>http://my-service:8080/service/api</pre> in addition to any <pre>@Path</pre> annotations included on the method.
     * @param url the base Url for the service.
     * @return the current builder with the baseUrl set
     */
    public abstract RestClientBuilder baseUrl(URL url);

    /**
     * Based on the configured RestClientBuilder, creates a new instance of the given REST interface to invoke API calls against.
     * @param clazz the interface that defines REST API methods for use
     * @param <T> the type of the interface
     * @return a new instance of an implementation of this REST interface that
     * @throws IllegalStateException if not all pre-requisites are satisfied for the builder, this exception may get thrown.  For instance, if a URL
     *  has not been set.
     */
    public abstract <T> T build(Class<T> clazz) throws IllegalStateException;

    private static ToIntFunction<Object> priorityComparator() {
        return value -> {
            Priority priority = value.getClass().getAnnotation(Priority.class);
            if (priority == null) {
                return 1;
            }
            else {
                return priority.value();
            }
        };
    }
}
