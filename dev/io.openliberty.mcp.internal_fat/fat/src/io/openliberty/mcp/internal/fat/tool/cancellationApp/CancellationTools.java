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
        toolStatus.setRunning(latchName);
        int counter = 0;
        while (counter++ < 20) {
            TimeUnit.MILLISECONDS.sleep(500);
            LOG.info("[cancellationTool] Checking if tool is cancelled");
            if (cancellation.check().isRequested()) {
                LOG.info("[cancellationTool] tool is cancelled");
                throw new OperationCancellationException();
            }
        }
        LOG.info("[cancellationTool] the tool was not cancelled");
        return "If this String is returned, then the tool was not cancelled";
    }

    @Tool(name = "cancellationToolNoWait", title = "Cancellable tool NoWait", description = "A tool that does not waits to be cancelled")
    public String cancellationToolNoWait(Cancellation cancellation) {
        LOG.info("[cancellationToolNoWait] Starting");
        int counter = 0;
        while (counter++ < 5) {
            LOG.info("[cancellationToolNoWait] Checking if tool is cancelled");
            if (cancellation.check().isRequested()) {
                LOG.info("[cancellationToolNoWait] tool is cancelled");
                throw new OperationCancellationException();
            }
        }
        LOG.info("[cancellationToolNoWait] the tool was not cancelled");
        return "If this String is returned, then the tool was not cancelled";
    }

}
