/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

@RequestScoped
public class AsyncBean {

    public static final String CONNECT_A_DATA = "AsyncBean.connectA";
    public static final String CONNECT_B_DATA = "AsyncBean.connectB";
    public static final String CONNECT_C_DATA = "AsyncBean.connectC";

    private boolean wasInterrupted = false;

    @Asynchronous
    public Future<Connection> connectA() throws InterruptedException {
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_A_DATA + " started");
        Thread.sleep(TestConstants.WORK_TIME);
        Connection conn = new Connection() {
            @Override
            public String getData() {
                return CONNECT_A_DATA;
            }
        };
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_A_DATA + " returning");
        return CompletableFuture.completedFuture(conn);
    }

    @Timeout(TestConstants.TIMEOUT)
    @Asynchronous
    public Future<Connection> connectB() throws InterruptedException {
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_B_DATA + " started");
        Thread.sleep(TestConstants.WORK_TIME);
        Connection conn = new Connection() {
            @Override
            public String getData() {
                return CONNECT_B_DATA;
            }
        };
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_B_DATA + " returning");
        return CompletableFuture.completedFuture(conn);
    }

    @Asynchronous
    public Future<Void> connectC() throws InterruptedException {
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_C_DATA + " started");
        Thread.sleep(TestConstants.WORK_TIME);
        System.out.println(System.currentTimeMillis() + " - " + CONNECT_C_DATA + " returning");
        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous
    @Timeout(TestConstants.TIMEOUT)
    public Future<Void> waitNoInterrupt() {
        long waitNanos = TimeUnit.MILLISECONDS.toNanos(TestConstants.WORK_TIME);
        long startTime = System.nanoTime();

        // This is a loop which will not respond to a thread interruption
        // This is necessary to test that the future returned to the caller completes when
        // the timeout expires, even if the method running doesn't respect it.
        while (System.nanoTime() - startTime < waitNanos) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous
    public Future<Void> waitCheckCancel() {
        try {
            Thread.sleep(TestConstants.WORK_TIME);
        } catch (InterruptedException e) {
            wasInterrupted = true;
        }
        return CompletableFuture.completedFuture(null);
    }

    public boolean wasInterrupted() {
        return wasInterrupted;
    }
}
