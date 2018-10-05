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
package trace.source.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;

public class TraceHandlerImpl implements Handler {

    // private static final TraceComponent tc = Tr.register(TraceHandlerImpl.class);

    private volatile ExecutorService executorSrvc = null;

    private static final String customLogFile = TrConfigurator.getLogLocation() + File.separator + "tracehandlerimpl.log";
    private static SimpleFileLogger customLogger;

    static final String HANDLER_NAME = "TraceHandler";

    private volatile CollectorManager collectorMgr;

    private volatile Future<?> handlerTaskRef = null;

    private BufferManager bufferMgr = null;

    private final List<String> sourceIds = new ArrayList<String>() {
        {
            add("com.ibm.ws.logging.source.trace|memory");
        }
    };

    protected void activate(Map<String, Object> configuration) {
        try {
            customLogger = SimpleFileLogger.start(customLogFile);
        } catch (IOException e) {
            System.out.println("Failed starting custom logger : " + e.getMessage());
        }
        customLogger.event("Activating " + this);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
//            Tr.event(tc, "Activating " + this);
//        }
    }

    protected void deactivate(int reason) {
        customLogger.event(" Deactivating " + this + ", reason = " + reason);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
//            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
//        }
        customLogger.close();
        customLogger = null;
    }

    protected void modified(Map<String, Object> configuration) {
        customLogger.event(" Modified");
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
//            Tr.event(tc, " Modified");
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
        return HANDLER_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        customLogger.event("Setting buffer manager " + this);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
//            Tr.event(tc, "Setting buffer manager " + this);
//        }
        this.bufferMgr = bufferMgr;
        startHandler();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(String sourcdId, BufferManager bufferMgr) {
        customLogger.event("Setting buffer manager " + this);
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
//            Tr.event(tc, "Un-setting buffer manager " + this);
//        }
        stopHandler();
        this.bufferMgr = null;
    }

    /** Starts the handler task **/
    public void startHandler() {
        if (handlerTaskRef == null) {
            customLogger.debug("Starting handler task");
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Starting handler task", this);
//            }

            handlerTaskRef = executorSrvc.submit(handlerTask);

        }
    }

    /** Stops the handler task **/
    public void stopHandler() {
        if (handlerTaskRef != null) {
            customLogger.debug("Stopping handler task");
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Stopping handler task", this);
//            }
            handlerTaskRef.cancel(true);
            handlerTaskRef = null;

        }
    }

    private String getAttribute(GenericData genData, String key) {
        KeyValuePair[] pairs = genData.getPairs();
        for (KeyValuePair p : pairs) {
            if (p != null && !p.isList()) {
                KeyValuePair kvp = p;
                if (kvp.getKey().equals(key)) {
                    if (kvp.isInteger()) {
                        return Integer.toString(kvp.getIntValue());
                    } else if (kvp.isLong()) {
                        return Long.toString(kvp.getLongValue());
                    } else {
                        return kvp.getStringValue();
                    }

                }
            }
        }

        return "";
    }

    private final Runnable handlerTask = new Runnable() {
        @FFDCIgnore(value = { InterruptedException.class })
        @Trivial
        @Override
        public void run() {
            int counter = 1;
            while (counter <= 500) {
                try {
                    GenericData event = (LogTraceData) bufferMgr.getNextEvent(HANDLER_NAME);

                    // String eventString = event.toString();
                    // if (eventString.contains("testTraceSourceForLibertyLogging") || eventString.contains("testTraceSourceForJUL")) {
                    if (getAttribute(event, "module").contains("collector.manager_fat")) {
                        customLogger.debug("[" + counter + "]Received Trace event: " + event);
                    }

//                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                      Tr.debug(tc, "Received Trace event: " + event, this);
//                  }
                } catch (InterruptedException exit) {
                    // System.out.println("@@@@@ bufferMgr.getNextEvent Interrupted : " + exit.getMessage());
                    customLogger.debug("bufferMgr.getNextEvent Interrupted : " + exit.getMessage());
                    break; //Break out of the loop; ending thread
                } catch (Exception e) {
                    // System.out.println("@@@@@ bufferMgr.getNextEvent Exception : " + e.getMessage());
                    customLogger.debug("bufferMgr.getNextEvent Exception : " + e.getMessage());
                    stopHandler();
                }
                counter++;
            }
            // customLogger.debug("@@@@@ $$$ counter:" + counter);
        }
    };

    /**
     *
     */
    private static class SimpleFileLogger {

        private final File file;
        private static final String lineSeparator = "\n"; // System.lineSeparator()
        private static long count = 0;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        private final DecimalFormat decimalFormat = new DecimalFormat("00000000000000"); // 14 digit

        private final FileWriter fileStore;

        private SimpleFileLogger(String logFilePath) throws IOException {
            file = new File(logFilePath);
            file.createNewFile();

            // Appended & written synchronously
            fileStore = new FileWriter(file, true);
        }

        public static SimpleFileLogger start(String customlogfile) throws IOException {
            return new SimpleFileLogger(customlogfile);
        }

        protected void close() {
            try {
                fileStore.close();
            } catch (Exception e) {
                System.out.println("Error closing file :" + e.getMessage());
            }
        }

        protected String format(Date date, String type, String message, Object... args) {
            String timestamp = dateFormat.format(date);
            String logMsg = String.format(message, args);
            String counter = decimalFormat.format(++count);
            return String.format("[%s] [%s] [%s] %s %s", timestamp, counter, type, logMsg, lineSeparator);
        }

        private void log(String type, String message, Object... args) {
            Date date = new Date(System.currentTimeMillis());
            String formatedMsg = format(date, type, message, args);
            addRecord(formatedMsg);
        }

        private void addRecord(String logRecord) {
            try {
                fileStore.write(logRecord);
                fileStore.flush();
            } catch (IOException e) {
                System.out.println("@@@@@ Failed adding log to " + file + " : " + e.getMessage());
            }
        }

        public void info(String message, Object... args) {
            log("INFO", message, args);
        }

        public void warning(String message, Object... args) {
            log("WARNING", message, args);
        }

        public void debug(String message, Object... args) {
            fine(message, args);
        }

        public void error(String message, Object... args) {
            severe(message, args);
        }

        public void event(String message, Object... args) {
            log("EVENT", message, args);
        }

        public void fine(String message, Object... args) {
            log("FINE", message, args);
        }

        public void severe(String message, Object... args) {
            log("SEVERE", message, args);
        }

    }

}
