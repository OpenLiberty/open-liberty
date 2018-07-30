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

package org.eclipse.microprofile.reactive.streams.tck;

import org.eclipse.microprofile.reactive.streams.GraphAccessor;
import org.eclipse.microprofile.reactive.streams.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.spi.Graph;
import org.eclipse.microprofile.reactive.streams.spi.Stage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test for the GraphAccessor class.
 * <p>
 * This does not need an implementation of the engine to verify it.
 */
public class GraphAccessorVerification {

    @Test
    public void buildGraphForPublisherShouldProduceTheCorrectGraph() {
        Graph graph = GraphAccessor.buildGraphFor(ReactiveStreams.of(1).map(i -> i * 2));
        assertEquals(2, graph.getStages().size());
        List<Stage> stages = new ArrayList<>(graph.getStages());
        assertInstanceOf(stages.get(0), Stage.Of.class);
        assertInstanceOf(stages.get(1), Stage.Map.class);
    }

    @Test
    public void buildGraphForProcessorShouldProduceTheCorrectGraph() {
        Graph graph = GraphAccessor.buildGraphFor(ReactiveStreams.<Integer>builder().filter(i -> i > 10).takeWhile(i -> i < 20));
        assertEquals(2, graph.getStages().size());
        List<Stage> stages = new ArrayList<>(graph.getStages());
        assertInstanceOf(stages.get(0), Stage.Filter.class);
        assertInstanceOf(stages.get(1), Stage.TakeWhile.class);
    }

    @Test
    public void buildGraphForSubscriberShouldProduceTheCorrectGraph() {
        Graph graph = GraphAccessor.buildGraphFor(ReactiveStreams.<Integer>builder().limit(10).toList());
        assertEquals(2, graph.getStages().size());
        List<Stage> stages = new ArrayList<>(graph.getStages());
        assertInstanceOf(stages.get(0), Stage.Limit.class);
        assertInstanceOf(stages.get(1), Stage.Collect.class);
    }

    @Test
    public void buildGraphForCompletionRunnerShouldProduceTheCorrectGraph() {
        Graph graph = GraphAccessor.buildGraphFor(ReactiveStreams.failed(new Exception()).distinct().cancel());
        assertEquals(3, graph.getStages().size());
        List<Stage> stages = new ArrayList<>(graph.getStages());
        assertInstanceOf(stages.get(0), Stage.Failed.class);
        assertInstanceOf(stages.get(1), Stage.Distinct.class);
        assertEquals(Stage.Cancel.INSTANCE, stages.get(2));
    }

    private static void assertInstanceOf(Object obj, Class<?> clazz) {
        assertTrue(clazz.isInstance(obj), obj + " is not a an instance of " + clazz);
    }
}
