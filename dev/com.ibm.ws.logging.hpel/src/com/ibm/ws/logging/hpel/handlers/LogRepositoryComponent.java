/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.hpel.handlers;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.websphere.logging.hpel.writer.LogEventNotifier;
import com.ibm.ws.logging.hpel.LogFileWriter;
import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositorySubProcessCommunication;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.impl.LogFileWriterTextImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBaseImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerTextImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositorySpaceAlert;
import com.ibm.ws.logging.hpel.impl.LogRepositorySubManagerImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterCBuffImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterImpl;

/**
 * Central utility class into HPEL enabled logging service.
 */
public class LogRepositoryComponent {
	private static LogRecordHandler binaryHandler = null;
	private static LogRecordTextHandler textHandler = null;
	private static String svPid;
	private static String svSuperPid = null ;		// Only not null when we are a logical child process in the logging scope
	private static String svLabel;
	/** Integer value of the logging level separating log and trace records */
	public static final int TRACE_THRESHOLD = 625 ;	// 16890.20771


	private static synchronized LogRecordHandler getBinaryHandler() {
		if (binaryHandler == null) {
			binaryHandler = new LogRecordHandler(TRACE_THRESHOLD, LogRepositoryManagerImpl.KNOWN_FORMATTERS[0]);
		}
		return binaryHandler;
	}

	/**
	 * remove files from appropriate destination.  Called by receiver of interProcess communication on parent process
	 * @param destinationType Type of destination/repository
	 * @return boolean, true = success, false = failed to remove files
	 */
	public static boolean removeFiles(String destinationType) {
		LogRepositoryManager destManager = getManager(destinationType) ;
		if (destManager instanceof LogRepositoryManagerImpl)
			return destManager.purgeOldFiles() ;
		else
			return false ;
	}

	public static void setLogEventNotifier (LogEventNotifier logEventNotifier) {
		LogRepositoryManager destManager = getManager(LogRepositoryBaseImpl.TRACETYPE) ;
		destManager.setLogEventNotifier(logEventNotifier) ;
		destManager = getManager(LogRepositoryBaseImpl.LOGTYPE) ;
		destManager.setLogEventNotifier(logEventNotifier) ;
		// TextLog not necessary as we focus on log content with standard binary log and trace
	}

	/**
	 * set notification agent in child processes. To communicate with parent on file creation and need to remove files, there must
	 * be an agent capable of some for of interProcessComm.  This agent implements the <code>LogRepositorySubProcessCommunication</code>
	 * interface and, when ready is communicated to the manager in the child processes.  The calls also require each manager to know
	 * the managed type it is working with, so this is also set here.
	 * TODO: It is not now needed, but the managedType is exposed at the <code>LogRepositoryBaseImpl</code> level and only currently
	 * populated and used on the child processes.  So may want to move it up to <code>LogRepositorySubManagerImpl</code> or also
	 * populate it in <code>LogRepositoryManagerImpl</code>
	 * @param commAgent an implementation of an interface that can send appropriate messages to the parent process.
	 */
	public static void setNotificationAgent(LogRepositorySubProcessCommunication commAgent) {
		if (svSuperPid != null) {
			LogRepositoryManager destManager = getManager(LogRepositoryBaseImpl.TRACETYPE) ;
			((LogRepositorySubManagerImpl)destManager).setSubProcessCommunicationAgent(commAgent) ;
			destManager = getManager(LogRepositoryBaseImpl.LOGTYPE) ;
			((LogRepositorySubManagerImpl)destManager).setSubProcessCommunicationAgent(commAgent) ;
/*			destManager = getManager(LogRepositoryBaseImpl.TEXTLOGTYPE) ;  TODO: Issue where no TextLog yet
			((LogRepositorySubManagerImpl)destManager).setSubProcessCommunicationAgent(commAgent) ; */
		}
	}

	/**
	 * add a remote file to the cache of files currently considered for retention on the parent. The child process uses
	 * some form of interProcessCommunication to notify receiver of file creation, and this method is driven
	 * @param destinationType Type of destination/repository
	 * @param spTimeStamp timeStamp to be associated with the file
	 * @param spPid Process Id of the creating process
	 * @param spLabel Label of the creating process
	 * @return String with fully qualified name of file to create
	 */
	public static String addRemoteProcessFile(String destinationType, long spTimeStamp, String spPid, String spLabel) {
		LogRepositoryManager destManager = getManager(destinationType) ;
		String subProcessFileName = null ;
		if (destManager instanceof LogRepositoryManagerImpl)
			subProcessFileName = ((LogRepositoryManagerImpl)destManager).addNewFileFromSubProcess(spTimeStamp, spPid, spLabel) ;
		return subProcessFileName ;
	}

	/**
	 * process notification that child process has exited.
	 * @param spPid Process Id of the exiting process
	 */
	public static void inactivateSubProcess(String spPid) {
		LogRepositoryManager destManager = getManager(LogRepositoryBaseImpl.TRACETYPE) ;
		((LogRepositoryManagerImpl)destManager).inactivateSubProcess(spPid) ;
		destManager = getManager(LogRepositoryBaseImpl.LOGTYPE) ;
		((LogRepositoryManagerImpl)destManager).inactivateSubProcess(spPid) ;
	}
	
	private static LogRepositoryManager getManager(String destinationType) {
		DestinationChanger destinationChanger = getDestination(destinationType) ;
		if (destinationChanger == null)
			return null ;
		
		LogRepositoryWriter destWriter = destinationChanger.getWriter() ;
		return destWriter.getLogRepositoryManager() ;
	}
	
	private static DestinationChanger getDestination(String destinationType) {
		String lowType = destinationType.toLowerCase() ;
		if (LogRepositoryBaseImpl.TRACETYPE.equals(lowType))  
			return TRACE_DESTINATION_CHANGER ;
		if (LogRepositoryBaseImpl.LOGTYPE.equals(lowType))  
			return LOG_DESTINATION_CHANGER ;
		if (LogRepositoryBaseImpl.TEXTLOGTYPE.equals(lowType))  
			return TEXT_DESTINATION_CHANGER ;
		return null ;
	}

	private static abstract class DestinationChanger {
		
		/**
		 * Returns a writer uses for this message type records.
		 * 
		 * @return currently configured writer
		 */
		protected abstract LogRepositoryWriter getWriter();

		/**
		 * Sets a writer to use for this message type records.
		 * 
		 * @param writer the new writer to use
		 */
		protected abstract void setWriter(LogRepositoryWriter writer);

		/**
		 * @param manager
		 * @return new LogRepositoryWriter
		 */
		// protected abstract LogRepositoryWriterImpl createNewWriter(LogRepositoryManagerImpl manager); 18055
		protected abstract LogRepositoryWriterImpl createNewWriter(	LogRepositoryManager manager);

		/**
		 * @param location the location to manager.
		 * @param managedType type of this manager
		 * @return LogRepositoryManager
		 */
		// protected abstract LogRepositoryManagerImpl createNewManager(String
		// location); 18055
		protected abstract LogRepositoryManager createNewManager(String location, String managedType);

		/**
		 * Sets writing of the log records to a directory.
		 * 
		 * @param location
		 * @param enablePurgeBySize <code>true</code> if record purging due to the size constraint should be enabled.
		 * @param enablePurgeByTime <code>true</code> if record purging due to the the age should be enabled.
		 * @param enableFileSwitch <code>true</code> if file switching at a given hour of day should be enabled
		 * @param enableBuffering <code>true</code> if buffering of file writing should be enabled.
		 * @param maxRepositorySize
		 * @param retentionTime
		 * @param fileSwitchHour the hour of day the file switching is to occur value range: 0-23 where 0=midnight
		 * @param outOfSpaceAction
		 * @param managedType type of the manager
		 */
		public void setDirectoryDestination(String location, boolean enablePurgeBySize, boolean enablePurgeByTime,
				boolean enableFileSwitch, boolean enableBuffering, long maxRepositorySize, long retentionTime, int fileSwitchHour,
				String outOfSpaceAction, String managedType) {
			LogRepositoryWriterImpl newWriter = null;
			LogRepositoryManager manager = null; // 18055 interface, not BaseImpl
			if (location != null) {
				manager = createNewManager(location, managedType);
			} else {
				manager = getWriter().getLogRepositoryManager();
				// Verify manager before doing anything else
				if (manager == null) {
					throw new IllegalArgumentException("Argument 'location' can't be null if log writer was not setup by this class.");
				} else {
					if (svSuperPid == null && !(manager instanceof LogRepositoryManagerImpl))  // 18055
						throw new IllegalArgumentException("The Manager in the writer is not expected LogRepositoryManagerImpl.");
					else
						if (svSuperPid != null && !(manager instanceof LogRepositorySubManagerImpl))  // 18055
							throw new IllegalArgumentException("The Manager in the writer is not expected LogRepositorySubManagerImpl.");
				}
			}
			// Don't reuse existing writer to keep old configuration in case there's a problem with new values.
			newWriter = createNewWriter(manager);

			try {
				setOutOfSpaceAction(newWriter, outOfSpaceAction);
				newWriter.setBufferingEnabled(enableBuffering);

				if (enableFileSwitch) {
					newWriter.enableFileSwitch(fileSwitchHour);
				} else {
					newWriter.disableFileSwitch();
				}

				// Update manager after successful configuration of the writer it will use
				if (manager instanceof LogRepositoryManagerImpl) {
					setRetention((LogRepositoryManagerImpl) manager, enablePurgeBySize, enablePurgeByTime,
							maxRepositorySize, retentionTime);
				} else {  	// F017049-22453 this added so that child processes would could do config calculations for retention
					// child process does not do final retention, but must know when to contact parent for retention services 
					((LogRepositorySubManagerImpl)manager).configure(maxRepositorySize) ;
				}
				// Make one last test of the writer in case manager didn't attempt to write into the destination
				testWriter(newWriter);
			} catch (IllegalArgumentException ex) {
				// Stop new writer before re-throwing exception.
				newWriter.stop();
				throw ex;
			}
			
			// Stop old manager only after successful configuration of the new writer.
			LogRepositoryWriter oldWriter = getWriter();
			if (oldWriter != null) {
				LogRepositoryManager oldManager = oldWriter.getLogRepositoryManager();
				if (oldManager != null && oldManager != newWriter.getLogRepositoryManager()) {
					// Stop writer before manager in case it still needs manager to flush its buffer
					oldWriter.stop();
					oldManager.stop();
				}
			}
			// Update writer only after successful configuration of both writer and manager
			setWriter(newWriter);
		}
		
		/**
		 * Tests if writer can be safely used.
		 * 
		 * @param writer repository writer to verify
		 * @throws IllegalArgumentException if there's a problem with using this writer.
		 */
		protected void testWriter(LogRepositoryWriterImpl writer) throws IllegalArgumentException {
			// Succeed by default
		}
		
		private static void setRetention(LogRepositoryManagerImpl manager, boolean enablePurgeBySize, boolean enablePurgeByTime,
				long maxRepositorySize, long retentionTime) {
			if (enablePurgeBySize == false)	maxRepositorySize = -1L;
			if (enablePurgeByTime == false)	retentionTime = -1L;

			manager.configure(maxRepositorySize, retentionTime);
		}

		private static void setOutOfSpaceAction(LogRepositoryWriterImpl writer, String type) {
			int value;
			if ("StopLogging".equalsIgnoreCase(type)) {
				value = 2;
			} else if ("PurgeOld".equalsIgnoreCase(type)) {
				value = 1;
			} else if ("StopServer".equalsIgnoreCase(type)) {
				value = 0;
			} else {
				 // Throw exception to cancel all other changes as well.
				throw new IllegalArgumentException("Unknown outOfSpaceAction value: " + type);
			}
			writer.setOutOfSpaceAction(value);
		}
	}

	private final static BinaryDestinationChanger LOG_DESTINATION_CHANGER = new BinaryDestinationChanger() {
		@Override
		protected LogRepositoryWriter getWriter() {
			return getBinaryHandler().getLogWriter();
		}

		@Override
		protected void setWriter(LogRepositoryWriter writer) {
			getBinaryHandler().setLogWriter(writer);
		}

		@Override
		protected String getDirName() {
			return LogRepositoryManagerImpl.DEFAULT_LOCATION;
		}
	};
	
	private final static BinaryDestinationChanger TRACE_DESTINATION_CHANGER = new BinaryDestinationChanger() {
		@Override
		protected LogRepositoryWriter getWriter() {
			return getBinaryHandler().getTraceWriter();
		}

		@Override
		protected void setWriter(LogRepositoryWriter writer) {
			getBinaryHandler().setTraceWriter(writer);
		}

		@Override
		protected String getDirName() {
			return LogRepositoryManagerImpl.TRACE_LOCATION;
		}
	};

	/**
	 * Sets directory destination for log messages.
	 * 
	 * @param location the base directory to use for log file repository. Value 'null' means to keep using current directory.
	 * @param enablePurgeBySize <code>true</code> if record purging due to the size constrain should be enabled.
	 * @param enablePurgeByTime <code>true</code> if record purging due to the the age should be enabled.
	 * @param enableFileSwitch <code>true</code> if file switching at a given hour of day should be enabled
	 * @param enableBuffering <code>true</code> if buffering of file writing should be enabled.
	 * @param maxRepositorySize the maximum size of the repository in bytes.
	 * @param retentionTime the mininum time to store log records in milliseconds.
	 * @param fileSwitchHour the hour of day the file switching is to occur value range: 0-23 where 0=midnight
	 * @param outOfSpaceAction the action to do in case of IOException during log file write.
	 *            values: "StopLogging", "PurgeOld", "StopServer".
	 */
	public static synchronized void setLogDirectoryDestination(String location,	boolean enablePurgeBySize, boolean enablePurgeByTime,
			boolean enableFileSwitch, boolean enableBuffering, long maxRepositorySize, long retentionTime, int fileSwitchHour,
			String outOfSpaceAction) {
		LOG_DESTINATION_CHANGER.setDirectoryDestination(location, enablePurgeBySize, enablePurgeByTime, enableFileSwitch,
				enableBuffering, maxRepositorySize, retentionTime, fileSwitchHour, outOfSpaceAction, LogRepositoryBaseImpl.LOGTYPE);
	}

	/**
	 * Sets directory destination for trace messages.
	 * 
	 * @param location the base directory to use for trace file repository. Value 'null' means to keep using current directory.
	 * @param enablePurgeBySize  <code>true</code> if record purging due to the size constrain should be enabled.
	 * @param enablePurgeByTime <code>true</code> if record purging due to the the age should be enabled.
	 * @param enableFileSwitch <code>true</code> if file switching at a given hour of day should be enabled
	 * @param enableBuffering <code>true</code> if buffering of file writing should be enabled.
	 * @param maxRepositorySize the maximum size of the repository in bytes.
	 * @param retentionTime the mininum time to store log records in milliseconds.
	 * @param fileSwitchHour the hour of day the file switching is to occur value range: 0-23 where 0=midnight
	 * @param outOfSpaceAction the action to do in case of IOException during trace file
	 *            write. values: "StopLogging", "PurgeOld", "StopServer".
	 */
	public static synchronized void setTraceDirectoryDestination(String location, boolean enablePurgeBySize,
			boolean enablePurgeByTime, boolean enableFileSwitch, boolean enableBuffering, long maxRepositorySize,
			long retentionTime, int fileSwitchHour, String outOfSpaceAction) {
		TRACE_DESTINATION_CHANGER.setDirectoryDestination(location,	enablePurgeBySize, enablePurgeByTime, enableFileSwitch,
				enableBuffering, maxRepositorySize, retentionTime, fileSwitchHour, outOfSpaceAction, LogRepositoryBaseImpl.TRACETYPE);
	}


	/**
	 * Sets log stream destination for log messages.
	 * 
	 * @param location the name of the LogStream to use.
	 */
	public static synchronized void setLogStreamDestination(String location) {
		LOG_DESTINATION_CHANGER.setStreamDestination(location);
	}

	/**
	 * Sets log stream destination for trace messages.
	 * 
	 * @param location the name of the LogStream to use.
	 */
	public static synchronized void setTraceStreamDestination(String location) {
		TRACE_DESTINATION_CHANGER.setStreamDestination(location);
	}

	private abstract static class BinaryDestinationChanger extends	DestinationChanger {
		/**
		 * Returns name of the subdirectory to use for this message type records
		 * 
		 * @return the String to append to the directory location.
		 */
		protected abstract String getDirName();

		@Override
		protected LogRepositoryManager createNewManager(String location, String managedType) {
			LogRepositoryManager logRepositoryManager ;
			if (svSuperPid != null) {
				logRepositoryManager = new LogRepositorySubManagerImpl(new File(location, getDirName()), svPid, svLabel, svSuperPid) ;
				logRepositoryManager.setManagedType(managedType) ;
			} else { 
				logRepositoryManager = new LogRepositoryManagerImpl(new File(location, getDirName()), svPid, svLabel, true);
				logRepositoryManager.setManagedType(managedType) ;
			}
			return logRepositoryManager ;
		}

		@Override
		protected LogRepositoryWriterImpl createNewWriter(LogRepositoryManager manager) {
			return new LogRepositoryWriterImpl(manager);
		}

		/**
		 * Sets writing of the log records to a log stream.
		 * 
		 * @param location
		 */
		public void setStreamDestination(String location) {
			// TODO: Implement creating writers for z/OS LogStream
		}

	}

	/**
	 * Registers LogRepository handler. Should be issued when
	 * RasHelper.getServerName() returns a real name.
	 */
	public static synchronized void start() {
		// if (!WsLogManager.isHpelEnabled()) {
		// return;
		// }

		Logger.getLogger("").addHandler(getBinaryHandler());
		// ManagerAdmin.getWsHandlerManager().addWsHandler(getBinaryHandler(),
		// false);

	}

	/**
	 * Sets memory destination for trace messages.
	 * 
	 * @param location the base directory to use for trace file repository when in-memory records are dumped to the disk. Value 'null' means
	 *            to keep using current directory.
	 * @param maxSize the maximum size of the in-memory buffer in bytes.
	 */
	public static synchronized void setTraceMemoryDestination(String location, long maxSize) {
		LogRepositoryWriter old = getBinaryHandler().getTraceWriter();
		LogRepositoryWriterCBuffImpl writer;
		// Check if trace writer need to be changed.
		if (location == null && old instanceof LogRepositoryWriterCBuffImpl) {
			writer = (LogRepositoryWriterCBuffImpl) old;
		} else {
			// Get the repository manager to use for the dump writer.
			LogRepositoryManager manager = null;
			if (location == null && old != null) {
				manager = old.getLogRepositoryManager();
			} else if (location != null) {
				if (svSuperPid != null)
					manager = new LogRepositorySubManagerImpl(new File(location,
						LogRepositoryManagerImpl.TRACE_LOCATION), svPid, svLabel, svSuperPid);
				else
					manager = new LogRepositoryManagerImpl(new File(location,
						LogRepositoryManagerImpl.TRACE_LOCATION), svPid, svLabel, true);
			}
			if (manager == null) {
				throw new IllegalArgumentException(
						"Argument 'location' can't be null if log writer was not setup by this class.");
			}
			writer = new LogRepositoryWriterCBuffImpl(
					new LogRepositoryWriterImpl(manager));
		}
		if (maxSize > 0) {
			((LogRepositoryWriterCBuffImpl) writer).setMaxSize(maxSize);
		} else {
			// Stop new writer before throwing exception.
			if (old != writer) {
				writer.stop();
			}
			throw new IllegalArgumentException("Argument 'maxSize' should be more than zero");
		}
		// Stop old manager only after successful configuration of the new writer.
		if (old != null) {
			LogRepositoryManager oldManager = old.getLogRepositoryManager();
			if (oldManager != null && oldManager != writer.getLogRepositoryManager()) {
				// Stop old writer before stopping its manager
				if (old != writer) {
					old.stop();
				}
				oldManager.stop();
			}
		}
		// Update writer as a last call after all data verification is done.
		getBinaryHandler().setTraceWriter(writer);
	}

	/**
	 * Dumps trace records stored in the memory buffer to disk. This action
	 * happens only if trace destination was set to memory.
	 */
	public static synchronized void dumpTraceMemory() {
		LogRepositoryWriter writer = getBinaryHandler().getTraceWriter();
		if (writer instanceof LogRepositoryWriterCBuffImpl) {
			((LogRepositoryWriterCBuffImpl) writer).dumpItems();
		}
	}

	private final static DestinationChanger TEXT_DESTINATION_CHANGER = new DestinationChanger() {
			// TODO: MJC If/when we add TextLogManager to the distributed retention model (via RMI) ... will want managedType to be set
		@Override
		protected LogRepositoryManager createNewManager(String location, String managedType) {
			LogRepositoryManager logRepositoryManager = new LogRepositoryManagerTextImpl(new File(location), svPid, svLabel, false);
			logRepositoryManager.setManagedType(managedType);
			return logRepositoryManager;
		}

		@Override
		protected LogRepositoryWriterImpl createNewWriter(LogRepositoryManager manager) {
			return new LogRepositoryWriterImpl(manager) {
				@Override
				protected LogFileWriter createNewWriter(File file) throws IOException {
					return new LogFileWriterTextImpl(file, bufferingEnabled);
				}
			};
		}
		
		@Override
		protected void testWriter(LogRepositoryWriterImpl writer) {
			// Attempt to writer header in Text file here to have IllegalArgumentException early on.
			textHandler.copyHeader(writer);
			try {
				writer.writeHeader(System.currentTimeMillis());
			} catch (IOException ex) {
				throw new IllegalArgumentException("Failed to write header into destination file", ex);
			}
		}

		@Override
		protected LogRepositoryWriter getWriter() {
			return textHandler.getWriter();
		}

		@Override
		protected void setWriter(LogRepositoryWriter writer) {
			textHandler.setWriter(writer);
		}

	};

	/**
	 * Sets directory destination for text logging.
	 * 
	 * @param location the base directory to use for text log files. Value 'null' means to keep using current directory.
	 * @param enablePurgeBySize <code>true</code> if record purging due to the size constraint should be enabled.
	 * @param enablePurgeByTime <code>true</code> if record purging due to the the age should be enabled.
	 * @param enableFileSwitch <code>true</code> if file switching at a given hour of day should be enabled
	 * @param enableBuffering <code>true</code> if buffering of file writing should be enabled.
	 * @param maxRepositorySize the maximum size of the repository in bytes.
	 * @param retentionTime the mininum time to store log records in milliseconds.
	 * @param fileSwitchHour the hour of day the file switching is to occur value range: 0-23 where 0=midnight
	 * @param outOfSpaceAction the action to do in case of IOException during text file write. values: "StopLogging", "PurgeOld", "StopServer".
	 * @param outputFormat the output format to use. Supported values 'Advanced' and 'Basic'. Value 'null' means to keep that value unchanged.
	 * @param includeTrace the indicator if trace printed in the text log.
	 */
	public static synchronized void setTextDestination(String location, boolean enablePurgeBySize, boolean enablePurgeByTime,
			boolean enableFileSwitch, boolean enableBuffering, long maxRepositorySize, long retentionTime, int fileSwitchHour,
			String outOfSpaceAction, String outputFormat, boolean includeTrace) {		// F001340-16890

		if (svSuperPid != null) {
			// No TextLog for children sub-processes yet.
			return;
		}
		if (location == null && textHandler == null) {
			throw new IllegalArgumentException(
					"Argument 'location' can't be null if text logging is not enabled.");
		}
		
		// Let it fail with IllegalArgumentException here
		HpelFormatter.getFormatter(outputFormat);

		boolean addedHere = false;
		if (textHandler == null) {
			textHandler = new LogRecordTextHandler(TRACE_THRESHOLD) ;	// F001340-16890
			// ManagerAdmin.getWsHandlerManager().addWsHandler(textHandler, false);
			Logger.getLogger("").addHandler(textHandler);
			addedHere = true;
		}

		try {
			TEXT_DESTINATION_CHANGER.setDirectoryDestination(location, enablePurgeBySize, enablePurgeByTime, enableFileSwitch,
					enableBuffering, maxRepositorySize, retentionTime, fileSwitchHour, outOfSpaceAction, LogRepositoryBaseImpl.TEXTLOGTYPE);
		} catch (RuntimeException ex) { // Handle any runtime exception, not just the IllegalArgumentException
			if (addedHere) {
				disableTextDestination();
			}
			throw ex;
		}
		
		// These last settings should not fail
		textHandler.setFormat(outputFormat);
		textHandler.setIncludeTrace(includeTrace) ;		// F001340-16890
	}

	/**
	 * Disable text logging.
	 */
	public static synchronized void disableTextDestination() {
		if (textHandler != null) {
			Logger.getLogger("").removeHandler(textHandler);
			// ManagerAdmin.getWsHandlerManager().removeWsHandler(textHandler);
			//735356 Before deleting textHandler, we should close the file. Otherwise file remains open and causes hang during log rotation.
			textHandler.stop();			
			textHandler = null;
		}
	}

	 /**
 	  * identify whether this is a logical subProcess of another process or not
	  * @param pid processId of current process
	  * @param label label for current process (used in generating file names)
	  * @param superPid processId of logical parent process
	  */
	public static void setProcessInfo(String pid, String label, String superPid) {
		svPid = pid ;
		svLabel = label ;
		svSuperPid = superPid ;
	}

	/**
	 * Stops repository manager and unregisters LogRepository handler registered
	 * during {@link #start()} call. This method should be called on server
	 * shutdown.
	 */
	public static synchronized void stop() {
		// WsHandlerManager manager = ManagerAdmin.getWsHandlerManager();
		if (svSuperPid != null) {
			LogRepositoryManager destManager = getManager(LogRepositoryBaseImpl.LOGTYPE) ;
			((LogRepositorySubManagerImpl)destManager).inactivateSubProcess() ;
		}
			
		Logger manager = Logger.getLogger("");
		if (binaryHandler != null) {
			manager.removeHandler(binaryHandler);
			binaryHandler.stop();
			binaryHandler = null;
		}
		if (textHandler != null) {
			manager.removeHandler(textHandler);
			textHandler.stop();
			textHandler = null;
		}
		LogRepositorySpaceAlert.getInstance().stop() ;		// 694351
	}
}
