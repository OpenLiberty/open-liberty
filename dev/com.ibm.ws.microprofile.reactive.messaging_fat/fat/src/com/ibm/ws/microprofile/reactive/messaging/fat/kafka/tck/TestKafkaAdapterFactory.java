/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;

/**
 * A KafkaAdaptorFactory for use in tests outside of liberty. It expects the Kafka library using its own classloader.
 */
public class TestKafkaAdapterFactory extends KafkaAdapterFactory {

    @Override
    protected ClassLoader getClassLoader() {
        // In the test environments, everything shares the same classloader, all classes should be available
        return TestKafkaAdapterFactory.class.getClassLoader();
    }
}
