/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger; 	// MJC, this is for separate debugging

import com.ibm.ws.logging.hpel.LogRepositoryManager;
// import com.ibm.ws.logging.hpel.impl.LogRepositoryManagerImpl.FileDetails;
import com.ibm.ws.logging.hpel.LogRepositorySubProcessCommunication;

/**
 *
 */
public class LogRepositorySubManagerImpl extends LogRepositoryBaseImpl implements LogRepositoryManager {

	// These 4 constants should become part of the LogRepositoryManager interface.
	private final static long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
	
	private String ivProcessLabel ;
	private String ivSuperPid ;
	private String ivPid ;
				// 682033
	private long ivMaxListSize = -1 ;
	private ArrayList<File> createdFiles = new ArrayList<File>() ;
				// 682033
	
	private static String thisClass = LogRepositorySubManagerImpl.class.getName() ;
		// Unique logger as it does usepParentHandlers=false to avoid getting back into logging (and thus
		// risking recursion and stack overflow). No RBundle as only used for trace and write to sep file
		// To get this trace output, must include com.ibm.ws.logging.hpel.impl.*=fine in traceSpec
	private static Logger debugLogger = LogRepositoryBaseImpl.getLogger() ;
	
	private File ivSubDirectory = null ;		// F017049-16879.3 Need to construct this based on pids, labels, and child processes
			// Timestamps for file creations done before communication to the parent was established
	private final HashSet<Long> unsentFileNotifications = new HashSet<Long>() ;
	private boolean purgeFiles = false ;

	private LogRepositorySubProcessCommunication ivSubProcessCommAgent = null ;

	/**
	 * construct the manager of logging for this subProcess or logical child process
	 * @param directory Directory which is the base of all logging for the parent and child processes
	 * @param pid process ID for this subProcess
	 * @param label label to be used in naming files associated with this subProcess
	 * @param superPid process ID of the logical parent process 
	 */
	 // TODO: Consider a getLocation so that LogRepositoryComponent can verify location chgd before constructing new manager
	public LogRepositorySubManagerImpl(File directory, String pid, String label, String superPid) {
		super(directory);
		if (!AccessHelper.canMakeDirectories(directory)) {
			throw new IllegalArgumentException("Specified location can't be used as a log repository: " + directory);
		}
		ivPid = pid ;
		ivProcessLabel = label ;
		ivSuperPid = superPid ;
	}

	/**
	 * set the interface that will be used to communicate to the parent process when files are created or when there needs to be
	 * a request to purge additional files (F017049-22453)
	 * @param subProcessCommAgent implementer of this interface will notify parent processes of key activities
	 */
	public synchronized void setSubProcessCommunicationAgent(LogRepositorySubProcessCommunication subProcessCommAgent) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "setSubProcessCommAg", "type: "+managedType+
				" unSentSz: "+unsentFileNotifications.size()+" pid: "+ivPid+" parent pid: "+ivSuperPid);
		}
		ivSubProcessCommAgent = subProcessCommAgent ;
	}
	
	// Assume there's no space constrains by default.
	private long maxLogFileSize = MAX_LOG_FILE_SIZE;
	
	/**
	 * Configures constrain parameters of the repository.
	 * 
	 * @param maxRepositorySize maximum in bytes of the total sum of repository file sizes the manager should maintain.
	 * 			This limit is ignored if <code>maxRepositorySize</code> is less than or equal to zero.
	 */
	public synchronized void configure(long maxRepositorySize) {
		this.maxLogFileSize = LogRepositoryManagerImpl.calculateFileSplit(maxRepositorySize);
		ivMaxListSize = (maxRepositorySize < 0) ? -1 : maxRepositorySize / maxLogFileSize;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "configure", "inMax: "+maxRepositorySize+" outMax: "+this.maxLogFileSize+" Tp: "+managedType+
				" MaxFilesInList: "+ivMaxListSize);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryManager#checkForNewFile(long, long)
	 */
	public synchronized File checkForNewFile(long total, long timestamp) {
		// Check if file rotation is necessary.
		if (total <= maxLogFileSize) {
			return null;
		} 
		
		return startNewFile(timestamp);
	}

	/**
	 * close down this subManager as servant is going down.  If ORB agent is up, send any unsent file creation notifications
	 * and then notify the controller that this servant is no longer active
	 */
	public void inactivateSubProcess() {
		if (ivSubProcessCommAgent != null) {
			sendNotifications() ;
			ivSubProcessCommAgent.inactivateSubProcess(ivPid) ;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryManager#purgeOldFiles()
	 * This now does not do the purge, but notes the need.  This may be called with unwanted locks in place, so it will
	 * queue for the purging to be done at a later tim
	 */
	public synchronized boolean purgeOldFiles() {	// 671059 for servants, this will return null
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "purgeOldFiles", "Comm: "+ivSubProcessCommAgent+" Tp: "+managedType);
		}
		purgeFiles = true ;
		return false ;
	}

	public boolean purgeOldFilesAsync() {
		if (!purgeFiles)  return false ;
		boolean wasAnyFileRemoved = false;
		if (ivSubProcessCommAgent == null) 
			return false ;
		else {
			wasAnyFileRemoved = ivSubProcessCommAgent.removeFiles(managedType) ;
			purgeFiles = false ;
		}
		return wasAnyFileRemoved;
	}

	private final static long FAILED_WAIT_TIME = 1000; // sleep for 1sec.
	private final static long FAILED_MAX_COUNT = 5; // fall asleep 5 times.
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryManager#startNewFile(long)
	 * Modification to this method to queue requests so that the ORB calls are made when the thread is not holding crucial locks
	 */
	public synchronized File startNewFile(long timestamp) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "startNewFile", "unsentEmpty: "+unsentFileNotifications.isEmpty()+
				" tstamp: "+timestamp+" Tp: "+managedType+" subProcCommAgent: "+ivSubProcessCommAgent);
		}
		synchronized(unsentFileNotifications) {		// Do not make orb call while holding the LogRecordHandler
			unsentFileNotifications.add(timestamp) ;		// 671059 queue up for later notification
		}
		File file = null ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "startNewFile", "Add tstamp: "+timestamp+
				" is unsent entry: "+unsentFileNotifications.size()+" FileNm: "+"xx");
		}
		int count = 0;
		while (ivSubDirectory == null) {
			File parentDir = makeLogDirectory(timestamp, ivSuperPid);
			if (parentDir == null) {
				if (++count > FAILED_MAX_COUNT) {
					throw new RuntimeException("Failed to create instance log repository. See SystemOut.log for details.");
				}
				try {
					Thread.sleep(FAILED_WAIT_TIME);
				} catch (InterruptedException ex) {
					// Ignore it, assume that we had enough sleep.
				}
			} else {
				ivSubDirectory = new File(parentDir, getLogDirectoryName(-1, ivPid, ivProcessLabel));
			}
		}
		
		file = getLogFile(ivSubDirectory, timestamp);
			
		File dir = file.getParentFile();
		if (!AccessHelper.isDirectory(dir)) {
			AccessHelper.makeDirectories(dir);
		}
		
		// 682033 look at servant subDirectory and
		if (ivMaxListSize >= 0) {
			createdFiles.add(file);
			if (createdFiles.size() > ivMaxListSize) {
				AccessHelper.deleteFile(createdFiles.remove(0));
			}
		}
		// End of 682033 logic
		return file;
	}

	/**
	 * sent notifications of file creations to the controller.  This is here (the actual sending of the notifications) so
	 * that the requests can be queued and the actual orb calls can be made at a time when the thread is not holding
	 * critical locks
	 */
	public boolean sendNotifications() {
		long [] fileCre8TimeStamps ;
		synchronized(unsentFileNotifications) {
			int unsentSize = unsentFileNotifications.size() ;
			if (ivSubProcessCommAgent == null || unsentSize == 0) return false;		// Prepare to copy out notification tstamps
			fileCre8TimeStamps = new long[unsentSize] ;
			Iterator<Long> unsentIter = unsentFileNotifications.iterator() ;
			int curIdx = 0 ;
			while (unsentIter.hasNext()) {
				fileCre8TimeStamps[curIdx++] = unsentIter.next() ;
			}
			unsentFileNotifications.clear() ;
		}
		synchronized (ivSubProcessCommAgent) {
			for (long thisTstamp : fileCre8TimeStamps) {
				String tempStr = ivSubProcessCommAgent.notifyOfFileCreation(managedType, thisTstamp, ivPid, ivProcessLabel) ;
				if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
					debugLogger.logp(Level.FINE, thisClass, "setSubProcessCommAg", "Notifying of past timestamp: "+thisTstamp+
						" got file: "+tempStr);
				}
			}
		}
		return true ;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryManager#stop()
	 */
	public synchronized void stop() {
		inactivateSubProcess() ;
	}
	
}