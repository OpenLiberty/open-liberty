/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.handlers.LogRecordHandler;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBaseImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterCBuffImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterImpl;
import com.ibm.ws.logging.internal.hpel.HpelTraceServiceConfig.LogState;
import com.ibm.ws.logging.internal.hpel.HpelTraceServiceConfig.TraceState;

/**
 * Writer into HPEL repository instance. Handles configuration requests and publishing records
 * coming either from TrService or from JUL Loggers.
 *
 * Locking schema:
 * - Need to have write lock for the duration of configuration change or stop request.
 * - Need to have read lock for the duration of record publishing.
 */
public class HpelTraceServiceWriter {
    private volatile NoCloseLogRecordHandler hpelLog = null;
    private final HpelBaseTraceService service;
//    private LogRecordTextHandler textCopy = null;

    private LogState currentLogState = null;
    private TraceState currentTraceState = null;
//    private TextState currentTextState = null;

    // Configuration state lock. We need to for atomic changes to the writer state. Since 'configure' method is invoked
    // with the HpelBaseTraceService instance lock held it means that order of locks should always be service lock first
    // and configuration lock second. In short, don't hold to configuration lock if code flow will result in request to
    // the service lock.
    private final ReentrantReadWriteLock handlerLock = new ReentrantReadWriteLock();

    HpelTraceServiceWriter(HpelBaseTraceService service) {
        this.service = service;
    }

    public void configure(HpelTraceServiceConfig config) {
        handlerLock.writeLock().lock();
        try {
            String pid = HpelHelper.getProcessId();

            if (hpelLog == null) {
                hpelLog = new NoCloseLogRecordHandler();
            } else if (config.ivLog.equals(currentLogState) && config.ivTrace.equals(currentTraceState) /* && config.ivText.equals(currentTextState) */) {
                // No need to reconfigure if parameters are the same.
                return;
            }

            // grab the current writers and managers in case we decide we need to stop them
            LogRepositoryWriterImpl oldLogWriter = null;
            LogRepositoryManagerImpl oldLogManager = null;
            if (currentLogState != null) {
                oldLogWriter = (LogRepositoryWriterImpl) hpelLog.getLogWriter();
                if (oldLogWriter != null)
                    oldLogManager = (LogRepositoryManagerImpl) oldLogWriter.getLogRepositoryManager();
            }

            LogRepositoryWriterImpl oldTraceWriter = null;
            LogRepositoryManagerImpl oldTraceManager = null;
            if (currentTraceState != null) {
                // Disk writer can be hiding behind MemoryBuffer writer. Take that into account.
                if (currentTraceState.ivMemoryBufferSize > 0) {
                    oldTraceWriter = (LogRepositoryWriterImpl) ((LogRepositoryWriterCBuffImpl) hpelLog.getTraceWriter()).getWriter();
                } else {
                    oldTraceWriter = (LogRepositoryWriterImpl) hpelLog.getTraceWriter();
                }
                if (oldTraceWriter != null)
                    oldTraceManager = (LogRepositoryManagerImpl) oldTraceWriter.getLogRepositoryManager();
            }

            // Setup log and trace managers first since that's the only call which can fail and only
            // due to inability to use specified location for log writing.
            boolean isNewLog;
            final LogRepositoryManagerImpl logManager;
            final LogRepositoryWriterImpl logWriter;
            if (currentLogState == null || !currentLogState.ivDataDirectory.equals(config.ivLog.ivDataDirectory)) {
                isNewLog = true;
                logManager = new LogRepositoryManagerImpl(config.ivLog.getLocation(), pid, config.ivServerName, true);
                logWriter = new LogRepositoryWriterImpl(logManager);
            } else {
                isNewLog = false;
                logManager = oldLogManager;
                logWriter = oldLogWriter;
            }

            boolean isNewTrace;
            final LogRepositoryManagerImpl traceManager;
            final LogRepositoryWriterImpl writer;
            if (currentTraceState == null || !currentTraceState.ivDataDirectory.equals(config.ivTrace.ivDataDirectory)) {
                isNewTrace = true;
                traceManager = new LogRepositoryManagerImpl(config.ivTrace.getLocation(), pid, config.ivServerName, true);
                writer = new LogRepositoryWriterImpl(traceManager);
            } else {
                isNewTrace = false;
                traceManager = oldTraceManager;
                writer = oldTraceWriter;
            }

//            LogRepositoryWriterImpl textWriter = null;
//            // Check config.ivText.ivEnabled just once since it may get changed by another thread.
//            if (!config.ivText.ivEnabled) {
//                if (textCopy != null) {
//                    textCopy.stop();
//                    textCopy = null;
//                }
//            } else {
//                LogRepositoryManagerImpl textManager;
//                if (currentTextState == null || textCopy == null || !currentTextState.ivDataDirectory.equals(config.ivText.ivDataDirectory)) {
//                    textManager = new LogRepositoryManagerTextImpl(config.ivText.getLocation(), pid, config.ivServerName, false);
//                    final TextFileOutputStreamFactory outputStreamFactory = config.getTextFileOutputStreamFactory();
//                    textWriter = new LogRepositoryWriterImpl(textManager) {
//                        @Override
//                        protected LogFileWriter createNewWriter(File file) throws IOException {
//                            return new LogFileWriterTextImpl(new CustomOutputFile(file, outputStreamFactory), bufferingEnabled);
//                        }
//                    };
//                } else {
//                    textWriter = (LogRepositoryWriterImpl) textCopy.getWriter();
//                    textManager = (LogRepositoryManagerTextImpl) textWriter.getLogRepositoryManager();
//                }
//                if (textCopy == null) {
//                    textCopy = new LogRecordTextHandler(LogRepositoryComponent.TRACE_THRESHOLD);
//                }
//                // Set buffering selection earlier so that it's applicable for the first file created
//                textWriter.setBufferingEnabled(config.ivText.ivBufferingEnabled);
//                // Attempt to write header now to fail earlier if we pointed to a read-only location.
//                if (textWriter != textCopy.getWriter()) {
//                    textCopy.copyHeader(textWriter);
//                    try {
//                        textWriter.writeHeader(System.currentTimeMillis());
//                    } catch (IOException ex) {
//                        throw new IllegalArgumentException("Failed to write header into destination file", ex);
//                    }
//                }
//                if (currentTextState == null || currentTextState.ivPurgeMaxSize != config.ivText.ivPurgeMaxSize
//                        || currentTextState.ivPurgeMinTime != config.ivText.ivPurgeMinTime) {
//                    textManager.configure(config.ivText.getPurgeMaxSize(), config.ivText.getPurgeMinTime());
//                }
//
//                textWriter.setOutOfSpaceAction(config.ivText.ivOutOfSpaceAction.ordinal());
//                if (currentTextState == null || currentTextState.ivFileSwitchTime != config.ivText.ivFileSwitchTime) {
//                    if (currentTextState != null) {
//                        textWriter.disableFileSwitch();
//                    }
//                    if (config.ivText.ivFileSwitchTime >= 0) {
//                        textWriter.enableFileSwitch(config.ivText.ivFileSwitchTime);
//                    }
//                }
//
//                textCopy.setFormat(config.ivText.ivFormat.name());
//                textCopy.setIncludeTrace(config.ivText.ivIncludeTrace);
//            }

            if (isNewLog || currentLogState == null || currentLogState.ivPurgeMaxSize != config.ivLog.ivPurgeMaxSize
                || currentLogState.ivPurgeMinTime != config.ivLog.ivPurgeMinTime) {
                logManager.configure(config.ivLog.getPurgeMaxSize(), config.ivLog.getPurgeMinTime());
            }

            logWriter.setBufferingEnabled(config.ivLog.ivBufferingEnabled);
            logWriter.setOutOfSpaceAction(config.ivLog.ivOutOfSpaceAction.ordinal());
            if (isNewLog || currentLogState == null || currentLogState.ivFileSwitchTime != config.ivLog.ivFileSwitchTime) {
                if (currentLogState != null) {
                    logWriter.disableFileSwitch();
                }
                if (config.ivLog.ivFileSwitchTime >= 0) {
                    logWriter.enableFileSwitch(config.ivLog.ivFileSwitchTime);
                }
            }

            if (isNewTrace || currentTraceState == null || currentTraceState.ivPurgeMaxSize != config.ivTrace.ivPurgeMaxSize
                || currentTraceState.ivPurgeMinTime != config.ivTrace.ivPurgeMinTime) {
                traceManager.configure(config.ivTrace.getPurgeMaxSize(), config.ivTrace.getPurgeMinTime());
            }

            writer.setBufferingEnabled(config.ivTrace.ivBufferingEnabled);
            writer.setOutOfSpaceAction(config.ivTrace.ivOutOfSpaceAction.ordinal());
            if (isNewTrace || currentTraceState == null || currentTraceState.ivFileSwitchTime != config.ivTrace.ivFileSwitchTime) {
                if (currentTraceState != null) {
                    writer.disableFileSwitch();
                }
                if (config.ivTrace.ivFileSwitchTime >= 0) {
                    writer.enableFileSwitch(config.ivTrace.ivFileSwitchTime);
                }
            }

            LogRepositoryWriter traceWriter;
            if (config.ivTrace.ivMemoryBufferSize > 0) {
                // Reuse current MemoryBuffer writer if it's already in use.
                if (!isNewTrace && currentTraceState != null && currentTraceState.ivMemoryBufferSize > 0) {
                    traceWriter = hpelLog.getTraceWriter();
                } else {
                    traceWriter = new LogRepositoryWriterCBuffImpl(writer);
                }
                ((LogRepositoryWriterCBuffImpl) traceWriter).setMaxSize(config.ivTrace.getMemoryBufferSize());
            } else {
                traceWriter = writer;
            }

            // Stop old manager only after successful configuration of the new writer.
            if (oldLogWriter != null) {
                if (oldLogManager != null && oldLogManager != logWriter.getLogRepositoryManager()) {
                    // Stop writer before manager in case it still needs manager to flush its buffer
                    oldLogWriter.stop();
                    oldLogManager.stop();
                }
            }

            // Stop old manager only after successful configuration of the new writer.
            if (oldTraceWriter != null) {
                if (oldTraceManager != null && oldTraceManager != writer.getLogRepositoryManager()) {
                    // Stop writer before manager in case it still needs manager to flush its buffer
                    oldTraceWriter.stop();
                    oldTraceManager.stop();
                }
            }

            currentLogState = config.ivLog.clone();
            // Set writers only if we have new ones to avoid unnecessary close calls.
            if (hpelLog.getLogWriter() != logWriter) {
                hpelLog.setLogWriter(logWriter);
            }
            currentTraceState = config.ivTrace.clone();
            if (hpelLog.getTraceWriter() != traceWriter) {
                hpelLog.setTraceWriter(traceWriter);
            }

            // Set currentTextState to null if text is disabled instead of making a copy since code above
            // checks currentTextState for 'null' when it checks if text copy was previously disabled.
//            if (config.ivText.ivEnabled) {
//                currentTextState = config.ivText.clone();
//            } else {
//                currentTextState = null;
//            }
//            if (textCopy != null && textCopy.getWriter() != textWriter) {
//                textCopy.setWriter(textWriter);
//            }

        } finally {
            handlerLock.writeLock().unlock();
        }
    }

    public void stop() {
        handlerLock.writeLock().lock();
        try {
//            if (textCopy != null) {
//                textCopy.stop();
//                textCopy = null;
//            }
            if (hpelLog != null) {
                hpelLog.stop();
                hpelLog = null;
            }
            currentLogState = null;
            currentTraceState = null;
//            currentTextState = null;
        } finally {
            handlerLock.writeLock().unlock();
        }
    }

    public void repositoryPublish(LogRecord record) {
        Handler handler = getHandler();
        if (handler != null) {
            handler.publish(record);
        }
    }

    private class NoCloseLogRecordHandler extends LogRecordHandler {

        public NoCloseLogRecordHandler() {
            super(WsLevel.DETAIL.intValue(), LogRepositoryBaseImpl.KNOWN_FORMATTERS[0]);
        }

        /** {@inheritDoc} */
        @Override
        public void publish(LogRecord logRecord) {
            // Don't lock handlerLock for duration of 'notifyConsole' call since 'service' lock is acquired during that execution.
            boolean doPublish = service == null || service.notifyConsole(logRecord);

            handlerLock.readLock().lock();
            try {
                if (doPublish) {
                    // There's a chance a logger issues log() but hpelLog get stopped before that
                    // request comes to our handle. Check if handle is still open here.
                    if (hpelLog != null) {
                        super.publish(logRecord);
//                        if (textCopy != null) {
//                            textCopy.publish(logRecord);
//                        }
                    }
                }
            } finally {
                handlerLock.readLock().unlock();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            // Don't do anything to stop LogManager from closing handler's writers.
        }
    }

//    /**
//     * Implementation of File using custom way of creating output stream
//     */
//    private static class CustomOutputFile extends File implements GenericOutputFile {
//        private static final long serialVersionUID = 5593854362477981896L;
//        private final transient TextFileOutputStreamFactory outputStreamFactory;
//
//        /**
//         * @param delegate File instance for which we are acting as a proxy.
//         */
//        public CustomOutputFile(File delegate, TextFileOutputStreamFactory outputStreamFactory) {
//            super(delegate.getAbsolutePath());
//            this.outputStreamFactory = outputStreamFactory;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public FileOutputStream createOutputStream() throws IOException {
//            return outputStreamFactory.createOutputStream(this);
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public FileOutputStream createOutputStream(boolean append) throws IOException {
//            return outputStreamFactory.createOutputStream(this, append);
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = super.hashCode();
//            result = prime * result + ((outputStreamFactory == null) ? 0 : outputStreamFactory.hashCode());
//            return result;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//            if (!super.equals(obj))
//                return false;
//            if (!(obj instanceof CustomOutputFile))
//                return false;
//            CustomOutputFile other = (CustomOutputFile) obj;
//            if (outputStreamFactory == null) {
//                if (other.outputStreamFactory != null)
//                    return false;
//            } else if (!outputStreamFactory.equals(other.outputStreamFactory))
//                return false;
//            return true;
//        }
//
//    }

    public Handler getHandler() {
        return hpelLog;
    }
}
