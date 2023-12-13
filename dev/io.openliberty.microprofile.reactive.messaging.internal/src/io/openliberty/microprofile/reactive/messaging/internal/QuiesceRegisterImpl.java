/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.QuiesceParticipant;
import io.openliberty.microprofile.reactive.messaging.internal.interfaces.QuiesceRegister;

/**
 * The singleton implementation of {@link QuiesceRegister}
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class QuiesceRegisterImpl implements ServerQuiesceListener, QuiesceRegister {

    private final Set<QuiesceParticipant> participants = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void serverStopping() {
        Set<QuiesceParticipant> participantsCopy;
        // Avoid holding a lock on participants while calling quiesce() to avoid deadlock in case remove() is called at the same time
        synchronized (participants) {
            participantsCopy = new HashSet<>(participants);
        }

        for (QuiesceParticipant participant : participantsCopy) {
            participant.quiesce();
        }
    }

    @Override
    public void register(QuiesceParticipant participant) {
        participants.add(participant);
    }

    @Override
    public void remove(QuiesceParticipant participant) {
        participants.remove(participant);
    }

}
