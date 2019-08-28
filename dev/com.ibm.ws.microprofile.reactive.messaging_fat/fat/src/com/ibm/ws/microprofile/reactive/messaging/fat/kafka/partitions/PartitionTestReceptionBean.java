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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.partitions;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;

@ApplicationScoped
public class PartitionTestReceptionBean extends AbstractReceptionBean {

    public static final String CHANNEL_NAME = "partition-test-incoming-topic";

    @Incoming(CHANNEL_NAME)
    @Acknowledgment(Strategy.MANUAL)
    @Override
    public CompletionStage<Void> recieveMessage(Message<String> msg) {
        return super.recieveMessage(msg);
    }

}
