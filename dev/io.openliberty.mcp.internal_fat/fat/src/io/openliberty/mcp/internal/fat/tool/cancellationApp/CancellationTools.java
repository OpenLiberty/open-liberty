/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.cancellationApp;

import static io.openliberty.mcp.internal.fat.utils.TestConstants.NEGATIVE_TIMEOUT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.POSITIVE_TIMEOUT;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.messaging.Cancellation.OperationCancellationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Tools for CancellationTest
 */
@ApplicationScoped
public class CancellationTools {

    private static final Logger LOG = Logger.getLogger(CancellationTools.class.getName());

    @Inject
    private ToolStatus toolStatus;

    @Tool(name = "cancellationTool", title = "Cancellable tool", description = "A tool that waits to be cancelled")
    public String cancellationTool(Cancellation cancellation, @ToolArg(name = "latchName", description = "name of countdown latch to use for test") String latchName)
                    throws InterruptedException {
        LOG.info("[cancellationTool] Starting");
        toolStatus.signalStarted(latchName);
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < POSITIVE_TIMEOUT.toNanos()) {
            TimeUnit.MILLISECONDS.sleep(100);
            LOG.info("[cancellationTool] Checking if tool is cancelled");
            if (cancellation.check().isRequested()) {
                LOG.info("[cancellationTool] tool is cancelled");
                throw new OperationCancellationException();
            }
        }
        LOG.info("[cancellationTool] the tool was not cancelled");
        return "If this String is returned, then the tool was not cancelled";
    }

    @Tool(name = "cancellationToolMinimalWait", title = "Cancellable tool MinimalWait", description = "A tool that does not waits to be cancelled")
    public String cancellationToolMinimalWait(Cancellation cancellation) throws InterruptedException {
        LOG.info("[cancellationToolMinimalWait] Starting");
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < NEGATIVE_TIMEOUT.toNanos()) {
            TimeUnit.MILLISECONDS.sleep(100);
            LOG.info("[cancellationToolMinimalWait] Checking if tool is cancelled");
            if (cancellation.check().isRequested()) {
                LOG.info("[cancellationToolMinimalWait] tool is cancelled");
                throw new OperationCancellationException();
            }
        }
        LOG.info("[cancellationToolMinimalWait] the tool was not cancelled");
        return "If this String is returned, then the tool was not cancelled";
    }

    @Tool(name = "cancellationToolForStatelessMinimalWait", title = "Cancellable tool", description = "A tool that does not to be cancelled, for stateless test")
    public String cancellationToolForStatelessMinimalWait(Cancellation cancellation,
                                                          @ToolArg(name = "latchName", description = "name of countdown latch to use for test") String latchName)
                    throws InterruptedException {
        LOG.info("[cancellationToolForStatelessMinimalWait] Starting");
        toolStatus.signalStarted(latchName);
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < NEGATIVE_TIMEOUT.toNanos()) {
            TimeUnit.MILLISECONDS.sleep(100);
            LOG.info("[cancellationToolForStatelessMinimalWait] Checking if tool is cancelled");
            if (cancellation.check().isRequested()) {
                LOG.info("[cancellationToolForStatelessMinimalWait] tool is cancelled");
                throw new OperationCancellationException();
            }
        }
        LOG.info("[cancellationToolForStatelessMinimalWait] the tool was not cancelled");
        return "If this String is returned, then the tool was not cancelled";
    }

}
