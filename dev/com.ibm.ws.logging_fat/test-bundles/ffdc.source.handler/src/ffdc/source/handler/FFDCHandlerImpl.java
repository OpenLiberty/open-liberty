/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ffdc.source.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;

/**
 *
 */
public class FFDCHandlerImpl implements Handler {

    private static final TraceComponent tc = Tr.register(FFDCHandlerImpl.class, "FFDCSourceHandler",
                                                         "ffdc.source.handler.FFDCHandlerImplMessages");

    private volatile CollectorManager collectorMgr;

    private volatile Future<?> handlerTaskRef = null;

    private BufferManager bufferMgr = null;

    private volatile ExecutorService executorSrvc = null;

    private final List<String> sourceIds = new ArrayList<String>() {
        {
            add("com.ibm.ws.logging.ffdc.source.ffdcsource|memory");
        }
    };

    protected void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    protected void modified(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, " Modified");
    }

    protected void setExecutor(ExecutorService executorSrvc) {
        this.executorSrvc = executorSrvc;
    }

    protected void unsetExecutor(ExecutorService executorSrvc) {
        this.executorSrvc = null;
    }

    /** {@inheritDoc} */
    @Override
    public void init(CollectorManager collectorMgr) {
        try {
            this.collectorMgr = collectorMgr;
            this.collectorMgr.subscribe(this, sourceIds);
        } catch (Exception e) {

        }
    }

    /** {@inheritDoc} */
    @Override
    public String getHandlerName() {
        return "FFDCHandler";
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
        startHandler();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(String sourcdId, BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        stopHandler();
        this.bufferMgr = null;
    }

    /** Starts the handler task **/
    public void startHandler() {
        if (handlerTaskRef == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Starting handler task", this);

            }

            handlerTaskRef = executorSrvc.submit(handlerTask);

        }
    }

    /** Stops the handler task **/
    public void stopHandler() {
        if (handlerTaskRef != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Stopping handler task", this);
            }
            handlerTaskRef.cancel(true);
            handlerTaskRef = null;

        }
    }

    private final Runnable handlerTask = new Runnable() {
        @FFDCIgnore(value = { InterruptedException.class })
        @Trivial
        @Override
        public void run() {
            int count = 1;
            while (count <= 3) {
                try {
                    Object event = bufferMgr.getNextEvent("FFDCHandler");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Received ffdc event: " + event, this);
                    }
                    count++;
                } catch (InterruptedException exit) {
                    break; //Break out of the loop; ending thread
                } catch (Exception e) {
                    stopHandler();
                }
            }
        }
    };
}
