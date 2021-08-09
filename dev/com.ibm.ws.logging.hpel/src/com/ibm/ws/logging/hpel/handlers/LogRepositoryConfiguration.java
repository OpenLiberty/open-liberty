/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.hpel.handlers;

import com.ibm.websphere.logging.hpel.reader.HpelFormatter;

/**
 * Manage the configuration data for High Performance Extensible Logging (log, trace, and textLog).  Bulk set operations
 * are for startup or any time when a single change should not ripple through the runtime until all changes from a set
 * are done.  <code>LogRepositoryConfiguration</code> is a singleton class.
 */
public class LogRepositoryConfiguration {
	private static final LogRepositoryConfiguration ivLogRepositoryConfiguration = new LogRepositoryConfiguration() ;
	public static final int MILLIS_IN_HOURS = 1000 * 60 * 60 ;
	public static final int ONE_MEG = 1024*1024;
	// control
	private boolean ivControlRawTraceFilterEnabled;
	// log
	private final LogState ivLog = new LogState();

	public static final String DIRECTORY_TYPE = "DIRECTORY";
	public static final String MEMORYBUFFER_TYPE = "MEMORYBUFFER";
	// trace 
	private final TraceState ivTrace = new TraceState();
	// textLog
	private final TextState ivText = new TextState();
	
	/**
	 * Parameters related to log
	 */
	private class LogState {
		String ivDataDirectory;
		boolean ivPurgeBySizeEnabled ;
		long ivPurgeMaxSize;         //in MB
		boolean ivPurgeByTimeEnabled ;
		long ivPurgeMinTime;       //in hours
		String ivOutOfSpaceAction = "StopLogging";
		boolean ivBufferingEnabled ;
		boolean ivFileSwitchEnabled ;
		int  ivFileSwitchTime;     // Hour of the day
		
		/**
		 * Copy parameters of this instance into <code>other</code> instance
		 * @param other instance to copy parameters to
		 */
		protected void copyTo(LogState other) {
			other.ivDataDirectory = ivDataDirectory;
			other.ivPurgeBySizeEnabled = ivPurgeBySizeEnabled;
			other.ivPurgeMaxSize = ivPurgeMaxSize;
			other.ivPurgeByTimeEnabled = ivPurgeByTimeEnabled;
			other.ivPurgeMinTime = ivPurgeMinTime;
			other.ivOutOfSpaceAction = ivOutOfSpaceAction;
			other.ivBufferingEnabled = ivBufferingEnabled;
			other.ivFileSwitchEnabled = ivFileSwitchEnabled;
			other.ivFileSwitchTime = ivFileSwitchTime;
		}
		
		/**
		 * Create new instance of this type. Subclasses should override this method.
		 * @return new instance of the correct type
		 */
		protected LogState newInstance() {
			return new LogState();
		}
		
		/**
		 * Creates exact copy of this instance.
		 */
		protected LogState clone() {
			LogState copy = newInstance();
			copyTo(copy);
			return copy;
		}
		
		LogState setDataDirectory(String dataDirectory) {
			if (dataDirectory != null && !dataDirectory.equals(ivDataDirectory)) {
				LogState copy = clone();
				copy.ivDataDirectory = dataDirectory;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setPurgeBySizeEnabled(boolean purgeBySizeEnabled) {
			if (purgeBySizeEnabled != ivPurgeBySizeEnabled) {
				LogState copy = clone();
				copy.ivPurgeBySizeEnabled = purgeBySizeEnabled;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setPurgeMaxSize(long purgeMaxSize) {
			if (purgeMaxSize != ivPurgeMaxSize) {
				LogState copy = clone();
				copy.ivPurgeMaxSize = purgeMaxSize;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setPurgeByTimeEnabled(boolean purgeByTimeEnabled) {
			if (purgeByTimeEnabled != ivPurgeByTimeEnabled) {
				LogState copy = clone();
				copy.ivPurgeByTimeEnabled = purgeByTimeEnabled;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setPurgeMinTime(long purgeMinTime) {
			if (purgeMinTime != ivPurgeMinTime) {
				LogState copy = clone();
				copy.ivPurgeMinTime = purgeMinTime;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setOutOfSpaceAction(String outOfSpaceAction) {
			if (outOfSpaceAction != null && !outOfSpaceAction.equals(ivOutOfSpaceAction)) {
				LogState copy = clone();
				copy.ivOutOfSpaceAction = outOfSpaceAction;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setBufferingEnabled(boolean bufferingEnabled) {
			if (bufferingEnabled != ivBufferingEnabled) {
				LogState copy = clone();
				copy.ivBufferingEnabled = bufferingEnabled;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setFileSwitchEnabled(boolean fileSwitchEnabled) {
			if (fileSwitchEnabled != ivFileSwitchEnabled) {
				LogState copy = clone();
				copy.ivFileSwitchEnabled = fileSwitchEnabled;
				return copy;
			} else {
				return null;
			}
		}
		
		LogState setFileSwitchTime(int fileSwitchTime) {
			if (fileSwitchTime != ivFileSwitchTime) {
				LogState copy = clone();
				copy.ivFileSwitchTime = fileSwitchTime;
				return copy;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Parameters related to trace
	 */
	private class TraceState extends LogState {
		String ivStorageType=DIRECTORY_TYPE;   //   DIRECTORY	MEMORYBUFFER
		long ivMemoryBufferSize;
		
		protected void copyTo(LogState other) {
			super.copyTo(other);
			if (other instanceof TraceState) {
				((TraceState)other).ivStorageType = ivStorageType;
				((TraceState)other).ivMemoryBufferSize = ivMemoryBufferSize;
			}
		}
		
		protected LogState newInstance() {
			return new TraceState();
		}
		
		TraceState setStorageType(String storageType) {
			if (storageType != null && !storageType.equals(ivStorageType)) {
				TraceState copy = (TraceState)clone();
				copy.ivStorageType = storageType;
				return copy;
			} else {
				return null;
			}
		}
		
		TraceState setMemoryBufferSize(long memoryBufferSize) {
			if (memoryBufferSize != ivMemoryBufferSize) {
				TraceState copy = (TraceState)clone();
				copy.ivMemoryBufferSize = memoryBufferSize;
				return copy;
			} else {
				return null;
			}
		}
		
	}
	
	/**
	 * Parameters related to text log
	 */
	private class TextState extends LogState {
		boolean ivEnabled = false;
		boolean ivTraceIncluded;	// F001340-16890
		String ivOutputFormat = HpelFormatter.FORMAT_BASIC;
		
		protected void copyTo(LogState other) {
			super.copyTo(other);
			if (other instanceof TextState) {
				((TextState)other).ivEnabled = ivEnabled;
				((TextState)other).ivTraceIncluded = ivTraceIncluded;
				((TextState)other).ivOutputFormat = ivOutputFormat;
			}
		}
		
		protected LogState newInstance() {
			return new TextState();
		}
		
		TextState setEnabled(boolean enabled) {
			if (enabled != ivEnabled) {
				TextState copy = (TextState)clone();
				copy.ivEnabled = enabled;
				return copy;
			} else {
				return null;
			}
		}
		
		TextState setTraceIncluded(boolean traceIncluded) {
			if (traceIncluded != ivTraceIncluded) {
				TextState copy = (TextState)clone();
				copy.ivTraceIncluded = traceIncluded;
				return copy;
			} else {
				return null;
			}
		}
		
		TextState setOutputFormat(String outputFormat) {
			if (outputFormat != null && !outputFormat.equals(ivOutputFormat)) {
				TextState copy = (TextState)clone();
				copy.ivOutputFormat = outputFormat;
				return copy;
			} else {
				return null;
			}
		}
	}

	/**
	 * private Constructor and static Getter to get a reference to the singleton
	 */
	private LogRepositoryConfiguration() {
	}

	public static LogRepositoryConfiguration getLogRepositoryConfiguration() {
		return ivLogRepositoryConfiguration ;
	}

	private void updateLogConfiguration(LogState state){
		LogRepositoryComponent.setLogDirectoryDestination(state.ivDataDirectory, state.ivPurgeBySizeEnabled, state.ivPurgeByTimeEnabled,
			state.ivFileSwitchEnabled, state.ivBufferingEnabled, state.ivPurgeMaxSize * ONE_MEG , state.ivPurgeMinTime * MILLIS_IN_HOURS, 
			state.ivFileSwitchTime, state.ivOutOfSpaceAction);	  
	}

	// getter and setter for control.  2 legacy vals in control not used, and traceSpec tracked in ManagerAdmin.
	public boolean isRawTraceFilterEnabled() { return ivControlRawTraceFilterEnabled ; } 
	
	public void setRawTraceFilterEnabled(boolean rawTraceFilterEnabled) {
		ivControlRawTraceFilterEnabled = rawTraceFilterEnabled ;
	}
	/* (non-Javadoc)
	 * @see com.ibm.ws.runtime.mbean.HPELLogInterface#setLogRepository(java.lang.String, boolean, boolean, long, long, java.lang.String, boolean, boolean, int)
	 */
	public void setLog(String dataDirectory, boolean enablePurgeBySize, boolean enablePurgeByTime, long maxSize  , long minPurgeTime,
		String outOfSpaceAction, boolean enableBuffering, boolean fileSwitchEnabled, int fileSwitchTime){
		LogState state = ivLog.clone();
		state.ivDataDirectory = dataDirectory;
		state.ivPurgeBySizeEnabled = enablePurgeBySize ;
		state.ivPurgeByTimeEnabled = enablePurgeByTime ;
		state.ivPurgeMaxSize = maxSize;
		state.ivPurgeMinTime = minPurgeTime;
		state.ivOutOfSpaceAction = outOfSpaceAction;
		state.ivBufferingEnabled = enableBuffering ;
		state.ivFileSwitchEnabled = fileSwitchEnabled ;
		state.ivFileSwitchTime = fileSwitchTime ;

		updateLogConfiguration(state);
		
		state.copyTo(ivLog);
	}

    // Getters for Log Data
    public String getLogDataDirectory() {  return ivLog.ivDataDirectory; }
    public boolean isLogPurgeBySizeEnabled() {  return ivLog.ivPurgeBySizeEnabled; }
    public long getLogPurgeMaxSize() {  return ivLog.ivPurgeMaxSize; }
    public boolean isLogPurgeByTimeEnabled () {  return ivLog.ivPurgeByTimeEnabled ; }
    public long getLogPurgeMinTime() {  return ivLog.ivPurgeMinTime; }
    public String getLogOutOfSpaceAction() {  return ivLog.ivOutOfSpaceAction; }
    public boolean isLogBufferingEnabled () {  return ivLog.ivBufferingEnabled ; }
    public boolean isLogFileSwitchEnabled () {  return ivLog.ivFileSwitchEnabled ; }
    public int getLogFileSwitchTime() {  return ivLog.ivFileSwitchTime; }
    
    // Setters for Log Data
    public void setLogDataDirectory(String logDataDirectory) {
    	LogState state = ivLog.setDataDirectory(logDataDirectory);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogPurgeBySizeEnabled(boolean logPurgeBySizeEnabled) {
    	LogState state = ivLog.setPurgeBySizeEnabled(logPurgeBySizeEnabled);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogPurgeMaxSize(long logPurgeMaxSize) {
    	LogState state = ivLog.setPurgeMaxSize(logPurgeMaxSize);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogPurgeByTimeEnabled(boolean logPurgeByTimeEnabled) {
    	LogState state = ivLog.setPurgeByTimeEnabled(logPurgeByTimeEnabled);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogPurgeMinTime(long logPurgeMinTime) {
    	LogState state = ivLog.setPurgeMinTime(logPurgeMinTime);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogOutOfSpaceAction(String logOutOfSpaceAction) {
    	LogState state = ivLog.setOutOfSpaceAction(logOutOfSpaceAction);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogBufferingEnabled(boolean logBufferingEnabled) {
    	LogState state = ivLog.setBufferingEnabled(logBufferingEnabled);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogFileSwitchEnabled(boolean logFileSwitchEnabled) {
    	LogState state = ivLog.setFileSwitchEnabled(logFileSwitchEnabled);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        
    public void setLogFileSwitchTime(int logFileSwitchTime) {
    	LogState state = ivLog.setFileSwitchTime(logFileSwitchTime);
    	if (state != null) {
    		updateLogConfiguration(state);
    		state.copyTo(ivLog);
    	}
    }
        

	/**
	 * update all info for Trace Repository
	 */
	private void updateTraceConfiguration(TraceState state) {
		if (DIRECTORY_TYPE.equals(state.ivStorageType)) {
			LogRepositoryComponent.setTraceDirectoryDestination(state.ivDataDirectory, state.ivPurgeBySizeEnabled, state.ivPurgeByTimeEnabled, 
				state.ivFileSwitchEnabled, state.ivBufferingEnabled, state.ivPurgeMaxSize * ONE_MEG , state.ivPurgeMinTime * MILLIS_IN_HOURS, 
				state.ivFileSwitchTime, state.ivOutOfSpaceAction);
		} else if (MEMORYBUFFER_TYPE.equals(state.ivStorageType)) {
			LogRepositoryComponent.setTraceMemoryDestination(state.ivDataDirectory, state.ivMemoryBufferSize * ONE_MEG);
		} else {
			throw new IllegalArgumentException("Unknown value for trace storage type: " + state.ivStorageType);
		}
	}
	
	public void setTrace(String dataDirectory, boolean enablePurgeBySize, boolean enablePurgeByTime, long maxSize, long minPurgeTime, 
			String outOfSpaceAction, boolean enableBuffering, boolean fileSwitchEnabled, int fileSwitchTime){
		TraceState state = (TraceState)ivTrace.clone();
		state.ivStorageType = DIRECTORY_TYPE;
		state.ivDataDirectory = dataDirectory;
		state.ivPurgeBySizeEnabled = enablePurgeBySize ;
		state.ivPurgeByTimeEnabled = enablePurgeByTime ;
		state.ivPurgeMaxSize = maxSize;
		state.ivPurgeMinTime = minPurgeTime;
		state.ivOutOfSpaceAction = outOfSpaceAction;
		state.ivBufferingEnabled = enableBuffering ;
		state.ivFileSwitchEnabled = fileSwitchEnabled ;
		state.ivFileSwitchTime = fileSwitchTime ;

		updateTraceConfiguration(state);
		
		state.copyTo(ivTrace);

	}

	/**
	 * Modify the trace to use a memory buffer
	 * @param dataDirectory directory where buffer will be dumped if requested
	 * @param memoryBufferSize amount of memory (in Mb) to be used for this circular buffer
	 */
	public void setTraceMemory(String dataDirectory, long memoryBufferSize) {
		TraceState state = (TraceState)ivTrace.clone();
		state.ivStorageType = MEMORYBUFFER_TYPE;
		state.ivDataDirectory = dataDirectory;
		state.ivMemoryBufferSize = memoryBufferSize;
		
		updateTraceConfiguration(state);
		
		state.copyTo(ivTrace);
	}

    // Trace getters
    public String getTraceStorageType() {  return ivTrace.ivStorageType; }
    public String getTraceDataDirectory() {  return ivTrace.ivDataDirectory; }
    public boolean isTracePurgeBySizeEnabled() {  return ivTrace.ivPurgeBySizeEnabled; }
    public long getTracePurgeMaxSize() {  return ivTrace.ivPurgeMaxSize; }
    public boolean isTracePurgeByTimeEnabled() {  return ivTrace.ivPurgeByTimeEnabled; }
    public long getTracePurgeMinTime() {  return ivTrace.ivPurgeMinTime; }
    public long getTraceMemoryBufferSize() {  return ivTrace.ivMemoryBufferSize; }
    public String getTraceOutOfSpaceAction() {  return ivTrace.ivOutOfSpaceAction; }
    public boolean isTraceBufferingEnabled() {  return ivTrace.ivBufferingEnabled; }
    public boolean isTraceFileSwitchEnabled() {  return ivTrace.ivFileSwitchEnabled; }
    public int getTraceFileSwitchTime() {  return ivTrace.ivFileSwitchTime; }

    // Trace setters
    public void setTraceStorageType(String trcStorageType) {
    	TraceState state = ivTrace.setStorageType(trcStorageType);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceDataDirectory(String trcDataDirectory) {
    	TraceState state = (TraceState)ivTrace.setDataDirectory(trcDataDirectory);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTracePurgeBySizeEnabled(boolean trcPurgeBySizeEnabled) {
    	TraceState state = (TraceState)ivTrace.setPurgeBySizeEnabled(trcPurgeBySizeEnabled);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTracePurgeMaxSize(long trcPurgeMaxSize) {
    	TraceState state = (TraceState)ivTrace.setPurgeMaxSize(trcPurgeMaxSize);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTracePurgeByTimeEnabled(boolean trcPurgeByTimeEnabled) {
    	TraceState state = (TraceState)ivTrace.setPurgeByTimeEnabled(trcPurgeByTimeEnabled);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTracePurgeMinTime(long trcPurgeMinTime) {
    	TraceState state = (TraceState)ivTrace.setPurgeMinTime(trcPurgeMinTime);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceMemoryBufferSize(long trcMemoryBufferSize) {
    	TraceState state = (TraceState)ivTrace.setMemoryBufferSize(trcMemoryBufferSize);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceOutOfSpaceAction(String trcOutOfSpaceAction) {
    	TraceState state = (TraceState)ivTrace.setOutOfSpaceAction(trcOutOfSpaceAction);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceBufferingEnabled(boolean trcBufferingEnabled) {
    	TraceState state = (TraceState)ivTrace.setBufferingEnabled(trcBufferingEnabled);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceFileSwitchEnabled(boolean trcFileSwitchEnabled) {
    	TraceState state = (TraceState)ivTrace.setFileSwitchEnabled(trcFileSwitchEnabled);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

    public void setTraceFileSwitchTime(int trcFileSwitchTime) {
    	TraceState state = (TraceState)ivTrace.setFileSwitchTime(trcFileSwitchTime);
		if (state != null) {
			updateTraceConfiguration(state);
			state.copyTo(ivTrace);
		}
    }

	private void updateTextConfiguration(TextState state) {
		if (state.ivEnabled) 
			LogRepositoryComponent.setTextDestination(state.ivDataDirectory, state.ivPurgeBySizeEnabled, state.ivPurgeByTimeEnabled, state.ivFileSwitchEnabled, 
					state.ivBufferingEnabled, state.ivPurgeMaxSize * ONE_MEG , state.ivPurgeMinTime * MILLIS_IN_HOURS, state.ivFileSwitchTime, state.ivOutOfSpaceAction, 
					state.ivOutputFormat, state.ivTraceIncluded);	// F001340-16890
		else
			LogRepositoryComponent.disableTextDestination() ;
	}

	public void setTextLog(boolean enabled, String dataDirectory, boolean enablePurgeBySize, boolean enablePurgeByTime, long purgeMaxSize ,
			long purgeMinTime,String outOfSpaceAction, String outputFormat, boolean traceIncluded,	// F001340-16890 
			boolean enableBuffering, boolean fileSwitchEnabled, int fileSwitchTime){
		TextState state = (TextState)ivText.clone();
		state.ivEnabled = enabled ;
		state.ivDataDirectory = dataDirectory ;
		state.ivPurgeBySizeEnabled = enablePurgeBySize ;
		state.ivPurgeByTimeEnabled = enablePurgeByTime ;
		state.ivPurgeMaxSize = purgeMaxSize ;
		state.ivPurgeMinTime = purgeMinTime ;
		state.ivOutOfSpaceAction = outOfSpaceAction ;
		state.ivTraceIncluded = traceIncluded ;
		state.ivOutputFormat = outputFormat ;
		state.ivBufferingEnabled = enableBuffering ;
		state.ivFileSwitchEnabled = fileSwitchEnabled ;
		state.ivFileSwitchTime = fileSwitchTime ;
		
		updateTextConfiguration(state) ;
		
		state.copyTo(ivText);
	}

    // TextLog Getters
    public boolean isTextEnabled() {  return ivText.ivEnabled; }
    public String getTextDataDirectory() {  return ivText.ivDataDirectory; }
    public boolean isTextPurgeBySizeEnabled() {  return ivText.ivPurgeBySizeEnabled; }
    public boolean isTextPurgeByTimeEnabled() {  return ivText.ivPurgeByTimeEnabled; }
    public long getTextPurgeMaxSize() {  return ivText.ivPurgeMaxSize; }
    public long getTextPurgeMinTime() {  return ivText.ivPurgeMinTime; }
    public String getTextOutOfSpaceAction() {  return ivText.ivOutOfSpaceAction; }
    public boolean isTextTraceIncluded() {  return ivText.ivTraceIncluded; }
    public String getTextOutputFormat() {  return ivText.ivOutputFormat; }
    public boolean isTextBufferingEnabled() {  return ivText.ivBufferingEnabled; }
    public boolean isTextFileSwitchEnabled() {  return ivText.ivFileSwitchEnabled; }
    public int getTextFileSwitchTime() {  return ivText.ivFileSwitchTime; }

    // TextLog Setters
    public void setTextEnabled(boolean txtEnabled) {
    	TextState state = ivText.setEnabled(txtEnabled);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextDataDirectory(String txtDataDirectory) {
    	TextState state = (TextState)ivText.setDataDirectory(txtDataDirectory);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextPurgeBySizeEnabled(boolean txtPurgeBySizeEnabled) {
    	TextState state = (TextState)ivText.setPurgeBySizeEnabled(txtPurgeBySizeEnabled);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextPurgeByTimeEnabled(boolean txtPurgeByTimeEnabled) {
    	TextState state = (TextState)ivText.setPurgeByTimeEnabled(txtPurgeByTimeEnabled);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextPurgeMaxSize(long txtPurgeMaxSize) {
    	TextState state = (TextState)ivText.setPurgeMaxSize(txtPurgeMaxSize);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextPurgeMinTime(long txtPurgeMinTime) {
    	TextState state = (TextState)ivText.setPurgeMinTime(txtPurgeMinTime);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextOutOfSpaceAction(String txtOutOfSpaceAction) {
    	TextState state = (TextState)ivText.setOutOfSpaceAction(txtOutOfSpaceAction);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextTraceIncluded(boolean txtTraceIncluded) {
    	TextState state = (TextState)ivText.setTraceIncluded(txtTraceIncluded);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextOutputFormat(String txtOutputFormat) {
    	TextState state = (TextState)ivText.setOutputFormat(txtOutputFormat);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextBufferingEnabled(boolean txtBufferingEnabled) {
    	TextState state = (TextState)ivText.setBufferingEnabled(txtBufferingEnabled);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextFileSwitchEnabled(boolean txtFileSwitchEnabled) {
    	TextState state = (TextState)ivText.setFileSwitchEnabled(txtFileSwitchEnabled);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }

    public void setTextFileSwitchTime(int txtFileSwitchTime) {
    	TextState state = (TextState)ivText.setFileSwitchTime(txtFileSwitchTime);
    	if (state != null) {
    		updateTextConfiguration(state);
    		state.copyTo(ivText);
    	}
    }
    
    public void restartHpel() {
    	updateLogConfiguration(ivLog) ;
    	updateTraceConfiguration(ivTrace) ;
    	LogRepositoryComponent.start() ;
    	if (ivText.ivEnabled)
    		updateTextConfiguration(ivText) ;
    }
}
