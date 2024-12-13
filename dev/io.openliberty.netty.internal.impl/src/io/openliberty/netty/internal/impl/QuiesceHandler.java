/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelPromise;
import io.openliberty.netty.internal.impl.QuiesceHandler.QuiesceEvent;
import io.openliberty.netty.internal.impl.QuiesceState;

/**
 * Channel handler added to the pipeline that reacts to quiesce events.
 * When a quiesce event occurs, this handler invokes a specified quiesce task 
 * that handles protocol-specific quiesce behavior.
 */
public class QuiesceHandler extends ChannelDuplexHandler{

	
	
	public static final QuiesceEvent QUIESCE_EVENT = new QuiesceEvent();

	private static final TraceComponent tc = Tr.register(QuiesceHandler.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);

	private static final Callable<Void> NO_OP_TASK = () -> null;
	private Callable<Void> quiesceTask;

	/**
     * Constructs a QuiesceHandler using the default no-op strategy. 
     * This ensures that if no other task is provided, invoking the quiesce event 
	 * will not change the state of the connection.
     */
	public QuiesceHandler(){
		this.quiesceTask = NO_OP_TASK;
	 }

	/**
     * Constructs a QuiesceHandler with a custom quiesce task.
     * If a null task is provided, it reverts to the no-op strategy task.
     *
     * @param quiesceTask The task to be executed during quiesce.
     */
    public QuiesceHandler(Callable<Void> quiesceTask) {
        this.quiesceTask = (quiesceTask == null) ? NO_OP_TASK : quiesceTask;
    }

	/**
     * Updates the quiesce task at runtime. If a null task is passed,
     * it defaults to the no-op task.
     *
     * @param task The new quiesce task to run on quiesce.
     */
    public void setQuiesceTask(Callable<Void> task) {
        this.quiesceTask = (task == null) ? NO_OP_TASK : task;
    }

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Added quiesce handler for channel " + ctx.channel() + " with callable: " + quiesceTask);
		}
		super.handlerAdded(ctx);
	}

	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof QuiesceEvent) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received Quiesce Event for " + ctx.channel() + " with callable: " + quiesceTask);
            }
            handleQuiesce();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * Invokes the configured quiesce task upon receiving the quiesce event.
     * This task is expected to handle halting new work or gracefully completing 
     * existing work as the server transitions into quiesce.
     */
    private void handleQuiesce() throws Exception {
        quiesceTask.call();
    }

    static class QuiesceEvent {
		//Empty class, used to trigger the Quiesce Event.
    }
}