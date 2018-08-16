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

package org.eclipse.microprofile.reactive.streams;

import org.eclipse.microprofile.reactive.streams.spi.Stage;

/**
 * Internal stages, used to capture the graph while being built, but never passed to a
 * {@link org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine}.
 */
class InternalStages {

    interface InternalStage extends Stage {
    }

    /**
     * An identity stage - this stage simply passes is input to its output unchanged. It's used to represent processor
     * builders that have had no stages defined.
     * <p>
     * It gets ignored by the {@link ReactiveStreamsGraphBuilder} when encountered.
     */
    static final class Identity implements InternalStage {
        static final Identity INSTANCE = new Identity();

        private Identity() {
        }
    }

    /**
     * A nested stage. This is used to avoid having to rebuild the entire graph (which is represented as an immutable
     * cons) whenever two graphs are joined, or a stage is prepended into the graph.
     * <p>
     * It gets flattened out by the {@link ReactiveStreamsGraphBuilder} when building the graph.
     */
    static final class Nested implements InternalStage {
        private final ReactiveStreamsGraphBuilder graphBuilder;

        Nested(ReactiveStreamsGraphBuilder graphBuilder) {
            this.graphBuilder = graphBuilder;
        }

        ReactiveStreamsGraphBuilder getBuilder() {
            return graphBuilder;
        }
    }
}
