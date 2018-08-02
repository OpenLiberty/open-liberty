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

import java.util.Collection;

/**
 * A graph.
 * <p>
 * Reactive Streams engines are required to convert the stages of this graph into a stream with interfaces according
 * to the shape. The shape is governed by whether the graph has an inlet, an outlet, neither or both.
 */
public class Graph {
    private final Collection<Stage> stages;
    private final boolean hasInlet;
    private final boolean hasOutlet;

    /**
     * Create a graph from the given stages.
     * <p>
     * The stages of this graph will be validated to ensure that connect properly, that is, every stage but the first
     * stage must have an inlet to receive a stream from the previous stage, and every stage but the last stage must have
     * an outlet to produce a stream from the next stage.
     * <p>
     * If the first stage has an inlet, then this graph has an inlet, and can therefore be represented as a
     * {@link org.reactivestreams.Subscriber}. If the last stage has an outlet, then this graph has an outlet, and
     * therefore can be represented as a {@link org.reactivestreams.Publisher}.
     *
     * @param stages The stages.
     */
    public Graph(Collection<Stage> stages) {

        boolean hasInlet = true;
        Stage lastStage = null;

        for (Stage stage : stages) {
            if (lastStage != null && !lastStage.hasOutlet()) {
                throw new IllegalStateException("Graph required an outlet from the previous stage " + lastStage + " but none was found.");
            }

            if (lastStage != null) {
                if (!stage.hasInlet()) {
                    throw new IllegalStateException("Stage encountered in graph with no inlet after the first stage: " + stage);
                }
            }
            else {
                hasInlet = stage.hasInlet();
            }

            lastStage = stage;
        }

        this.hasInlet = hasInlet;
        this.hasOutlet = lastStage == null || lastStage.hasOutlet();
        this.stages = stages;
    }

    /**
     * Get the stages of this graph.
     */
    public Collection<Stage> getStages() {
        return stages;
    }

    /**
     * Returns true if this graph has an inlet, ie, if this graph can be turned into a
     * {@link org.reactivestreams.Subscriber}.
     */
    public boolean hasInlet() {
        return hasInlet;
    }

    /**
     * Returns true if this graph has an outlet, ie, if this graph can be turned into a
     * {@link org.reactivestreams.Publisher}.
     */
    public boolean hasOutlet() {
        return hasOutlet;
    }

    @Override
    public String toString() {
        return "Graph{" +
            "stages=" + stages +
            '}';
    }
}
