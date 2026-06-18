/*******************************************************************************n * Copyright (c) 2026 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package test.server.quiesce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 *
 */
@Component(service = QuiesceListenerOrderRecordingService.class)
public class QuiesceListenerOrderRecordingService {
    private final AtomicInteger listenerOrder = new AtomicInteger();
    private final List<String> listenersCalled = new ArrayList<>();

    private class TestOrderQuiesceListener implements ServerQuiesceListener {
        private final String type;
        private final Runnable stopServerAction;

        public TestOrderQuiesceListener(String type, Runnable stopServeAction) {
            this.type = type;
            this.stopServerAction = stopServeAction;
        }

        @Override
        public void serverStopping() {
            try {
                // add small delay to all test listeners to help test that all PRE listeners are called first
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // sync on recorder to ensure we print the order number "in order"
            synchronized (QuiesceListenerOrderRecordingService.this) {
                listenersCalled.add(" called type=" + type + ": " + listenerOrder.getAndIncrement());
            }
            stopServerAction.run();
        }

        @Override
        public String toString() {
            return "TestOrderQuiesceListener: type=" + type + " runnable=" + stopServerAction;
        }
    }

    @Deactivate
    void deactivate() {
        StringBuilder sb = new StringBuilder("QuiesceListenerOrderRecordingService:");
        listenersCalled.forEach(sb::append);
        System.out.println(sb);
    }

    public ServerQuiesceListener createQuiesceListener(String type, Runnable stopServerAction) {
        return new TestOrderQuiesceListener(type, stopServerAction);
    }
}
