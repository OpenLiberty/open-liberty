/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams.spi;

/**
 * Exception thrown when a reactive streams engine doesn't support a stage that is passed to it.
 * <p>
 * All reactive streams engines should support all stages, but this allows for a graceful mechanism to report issues,
 * for example if in a future version a new stage is added that is not recognised by an existing implementation.
 */
public class UnsupportedStageException extends RuntimeException {
    public UnsupportedStageException(Stage stage) {
        super("The " + stage.getClass().getSimpleName() + " stage is not supported by this Reactive Streams engine.");
    }
}
