/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.jsonb;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * Bean which uses JsonB to convert objects to json
 * <p>
 * Note that the Jsonb object will be created at application startup
 */
@ApplicationScoped
public class JsonbBean {

    @Incoming("input")
    @Outgoing("step1")
    public ProcessorBuilder<TestData, String> toJson() {
        // Tests creating Jsonb at startup
        Jsonb jsonb = JsonbBuilder.create();
        return ReactiveStreams.<TestData> builder()
                        .map((t) -> jsonb.toJson(t));
    }

    @Incoming("step1")
    @Outgoing("output")
    public TestData fromJson(String json) {
        // Tests creating Jsonb at processing time (app fully started)
        Jsonb jsonb = JsonbBuilder.create();
        return jsonb.fromJson(json, TestData.class);
    }

}
