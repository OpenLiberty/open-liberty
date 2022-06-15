/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.fat.eventListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.transaction.Transaction;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.tx.UOWEventEmitter;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 *
 */
@Component(service = { UOWEventListener.class })
public class TestUOWEventListener implements UOWEventListener {

    private static final TraceComponent tc = Tr.register(TestUOWEventListener.class);

    private static Hashtable<Long, List<UOWEventDetails>> got = new Hashtable<Long, List<UOWEventDetails>>();

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tx.UOWEventListener#UOWEvent(com.ibm.wsspi.tx.UOWEventEmitter, int, java.lang.Object)
     */
    @Override
    public void UOWEvent(UOWEventEmitter uow, int event, Object data) {

        final UOWEventDetails ed = new UOWEventDetails(uow, event, data);

        synchronized (got) {
            long key = Thread.currentThread().getId();
            if (got.get(key) == null) {
                final List<UOWEventDetails> l = new ArrayList<UOWEventDetails>();
                l.add(ed);
                System.out.println("Adding event to new list for thread " + key + " which now has " + l.size() + " elements");
                got.put(key, l);
            } else {
                got.get(key).add(ed);
                System.out.println("Adding event to existing list for thread " + key + " which now has " + got.get(key).size() + " elements");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            final String e;

            switch (event) {
                case UOWEventListener.POST_BEGIN:
                    e = "POST_BEGIN";
                    break;
                case UOWEventListener.POST_END:
                    e = "POST_END";
                    break;
                case UOWEventListener.REGISTER_SYNC:
                    e = "REGISTER_SYNC";
                    break;
                case UOWEventListener.RESUME:
                    e = "RESUME";
                    break;
                case UOWEventListener.SUSPEND:
                    e = "SUSPEND";
                    break;
                default:
                    e = "UNKNOWN";
                    break;
            }

            System.out.println("UOWEvent on thread " + Thread.currentThread().getId());
            System.out.println(ed);
            Tr.entry(tc, "UOWEvent", Thread.currentThread().getId(), ed);
            Tr.exit(tc, "UOWEvent");
        }
    }

    /**
     *
     */
    public static void start() {
        got = new Hashtable<Long, List<UOWEventDetails>>();
    }

    /**
     * @param integers
     * @throws Exception
     */
    public static void check(Integer[] need) throws Exception {

        // First find the ArrayList. We'll assume that only the test thread has global tran activity on it
        int needIndex = 0;

        System.out.println("Checking " + got.keySet().size() + " threads");

        synchronized (got) {
            for (List<UOWEventDetails> al : got.values()) {
                System.out.println("Checking new thread");
                if (need.length > al.size()) {
                    System.out.println("Not enough events on this thread");
                    continue;
                }

                for (UOWEventDetails ed : al) {
                    if (ed.uow instanceof Transaction) {
                        System.out.println("Got a global tran event");
                        if (need[needIndex] == ed.event) {
                            needIndex++;
                        } else {
                            throw new Exception("Expected: " + need[needIndex] + ", got: " + ed.event);
                        }

                        if (needIndex == need.length) {
                            System.out.println("Matched");
                            return;
                        }
                    } else {
                        System.out.println("Not a global tran event");
                    }
                }
            }
        }

        throw new Exception("No match on any thread");
    }

    private class UOWEventDetails {
        UOWEventEmitter uow;
        int event;
        Object data;

        /**
         * @param uow
         * @param event
         * @param data
         */
        public UOWEventDetails(UOWEventEmitter uow, int event, Object data) {
            this.uow = uow;
            this.event = event;
            this.data = data;
        }

        @Override
        public String toString() {
            final String e;

            switch (event) {
                case UOWEventListener.POST_BEGIN:
                    e = "POST_BEGIN";
                    break;
                case UOWEventListener.POST_END:
                    e = "POST_END";
                    break;
                case UOWEventListener.REGISTER_SYNC:
                    e = "REGISTER_SYNC";
                    break;
                case UOWEventListener.RESUME:
                    e = "RESUME";
                    break;
                case UOWEventListener.SUSPEND:
                    e = "SUSPEND";
                    break;
                default:
                    e = "UNKNOWN";
                    break;
            }

            return "uow: " + uow + ", event: " + e + ", data: " + data;
        }
    }
}
