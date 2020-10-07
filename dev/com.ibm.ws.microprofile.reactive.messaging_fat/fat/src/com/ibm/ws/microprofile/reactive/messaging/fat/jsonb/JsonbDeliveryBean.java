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

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractDeliveryBean;

/**
 * Bean which drives the "input" channel
 */
@ApplicationScoped
public class JsonbDeliveryBean extends AbstractDeliveryBean<TestData> {

    @Override
    @Outgoing("input")
    public CompletionStage<Message<TestData>> getMessage() {
        return super.getMessage();
    }

}
