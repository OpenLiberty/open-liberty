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

package org.eclipse.microprofile.rest.client.inject;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use the RestClient qualifier on an injection to point to indicate that this injection point is meant to use an instance
 * of a Type-Safe Rest Client.
 *
 * <pre>
 *     &#064;Inject
 *     &#064;RestClient
 *     private MyRemoteApi api;
 * </pre>
 *
 * This will cause the injection point to be satisfied by the MicroProfile Rest Client runtime.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RestClient {
    RestClient LITERAL = new RestClientLiteral();

    class RestClientLiteral extends AnnotationLiteral<RestClient> implements RestClient {

    }
}
