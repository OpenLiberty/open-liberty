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
package sample.source.handler;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

/**
 *
 */
public class SampleSourceImpl implements Source {

    private static final TraceComponent tc = Tr.register(SampleSourceImpl.class, "sampleSourceHandler",
                                                         "sample.source.handler.SampleSourceImplMessages");

    private volatile ExecutorService executorSrvc = null;

    private volatile Future<?> sourceTaskRef = null;

    private volatile BufferManager bufferMgr = null;

    public Integer event = null;

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

    protected void setExecutor(ExecutorService executorSrvc) {
        this.executorSrvc = executorSrvc;
    }

    protected void unsetExecutor(ExecutorService executorSrvc) {
        this.executorSrvc = null;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return "com.ibm.ws.collector.manager.sample.source";
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return "memory";
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        //Buffer available start source task.
        this.bufferMgr = bufferMgr;
        startSource();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        stopSource();
        this.bufferMgr = null;
    }

    /** Starts source task **/
    public void startSource() {
        if (sourceTaskRef == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Starting source task", this);
            }
            sourceTaskRef = executorSrvc.submit(sourceTask);
        }
    }

    /** Stops source task **/
    public void stopSource() {
        if (sourceTaskRef != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Stopping source task", this);
            }
            sourceTaskRef.cancel(false);
            sourceTaskRef = null;
        }
    }

    private final Runnable sourceTask = new Runnable() {
        @Trivial
        @Override
        public void run() {
            for (int loop = 1; loop < 3; loop++) {
                try {
                    event = Integer.valueOf(loop);
                    //Add event to the buffer.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding event: " + event, this);
                    }
                    bufferMgr.add(event);
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSource();
                    FFDCFilter.processException(e, this.getClass().getName(), "126", this);
                } finally {
                    Tr.info(tc, "This is a info message");
                    Tr.warning(tc, "This is a warning message");
                    Tr.fatal(tc, "This is a fatal message");
                    Tr.error(tc, "This is an error message");
                    Tr.audit(tc, "This is an audit message");
                }
            }
        }
    };

}
