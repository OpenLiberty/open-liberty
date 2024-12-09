/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 * Channel handler which is added to the pipeline to terminates new connections once the 
 * quiesce period is started.
 */
public class QuiesceHandler extends ChannelDuplexHandler{

	
	
	public static final QuiesceEvent QUIESCE_EVENT = new QuiesceEvent();

	private boolean completed = false;


	private static final TraceComponent tc = Tr.register(QuiesceHandler.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);

	private static final Callable<Void> NO_OP_TASK = () -> null;
	private Callable quiesceTask;



	public QuiesceHandler(){
		this.quiesceTask = NO_OP_TASK;
	 }

	public QuiesceHandler(Callable quiesceTask) {
		if(quiesceTask == null){
			this.quiesceTask = NO_OP_TASK;
		}else{
			this.quiesceTask = quiesceTask;
		}
	}

	public void setQuiesceTask(Callable task){
		if(task == null){
			this.quiesceTask = NO_OP_TASK;
		}else{
			this.quiesceTask= task;
		}
		
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

            handleQuiesce(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void handleQuiesce(ChannelHandlerContext ctx) throws Exception {
		if (quiesceTask != NO_OP_TASK) {
        	quiesceTask.call();
		}
    }

    static class QuiesceEvent {
    }
}