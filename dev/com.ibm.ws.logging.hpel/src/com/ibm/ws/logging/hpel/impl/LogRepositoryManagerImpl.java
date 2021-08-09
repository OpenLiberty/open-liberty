/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.writer.LogEventListener;
import com.ibm.websphere.logging.hpel.writer.LogEventNotifier;
import com.ibm.ws.logging.hpel.LogRepositoryManager;

/**
 * Implementation of the {@link LogRepositoryManager} interface maintaining constrains on
 * the repository according to its maximum size and a minimum log record retention time.
 */
public class LogRepositoryManagerImpl extends LogRepositoryBaseImpl implements LogRepositoryManager {
	private static class EventContents {
		public String eventType ;
		public String repositoryType ;
		public Date dateOldestLogRecord ;
		public EventContents(String eventType, String repositoryType, Date dateOldestRecord) {
			this.eventType = eventType ;
			this.repositoryType = repositoryType ;
			this.dateOldestLogRecord = dateOldestRecord ;
		}
	}

	// Assume there's no space constrains by default.
	private long maxLogFileSize = MAX_LOG_FILE_SIZE;
	private long maxRepositorySize = -1;
	private final String svPid;

	private static String thisClass = LogRepositoryManagerImpl.class.getName() ;
	private static Logger debugLogger = LogRepositoryBaseImpl.getLogger() ; 
		// Assume there's no time constrains by default.
	private TimerThread timer = null;
	
	private File ivSubDirectory = null;		// F017049-16879.3 Need to construct this based on pids, labels, and child processes
	private final HashMap<String, FileDetails> activeFilesMap = new HashMap<String, FileDetails>() ;
	// Contains count of both files and subdirectories.
	private final HashMap<File, Integer> parentFilesMap = new HashMap<File, Integer>() ;
	private String repositoryType = null ;
	private boolean okToNotify = true ;		// Do not send notify until a roll is complete
	private static ArrayList<EventContents>notificationQueue = 
		new ArrayList<EventContents>() ;
	
	/**
	 * Creates LogRepositoryManager instance maintaining files in the given location
	 * for the specified process.
	 * 
	 * @param directory the location of all repository files.
	 * @param pid the identifier of the process writing into this repository.
	 * @param label additional identifier of the writing process.
	 * @param useDirTree value <code>true</code> means we need to use instance directories with children
	 * 		subdirectories in them; <code>false</code> means everything need to be stored in the root
	 * 		directory.
	 */
	public LogRepositoryManagerImpl(File directory, String pid, String label, boolean useDirTree) {
		super(directory);
		if (!AccessHelper.canMakeDirectories(directory)) {
			throw new IllegalArgumentException("Specified location can't be used as a log repository: " + directory);
		}
		
		//On iSeries platforms, the pid will be a qualified combination of job number, user, and jobname with a
		//qualifier of /.  The job number is a unique number assigned by the iSeries system that is always 6 digits.
		//Since job number is unique and numeric, we will strip off the rest and treat the job number as a pid value.
		int index = pid.indexOf("/");
		if(index > -1 ){
			pid = pid.substring(0, index);			
		}
		svPid = pid;
		if (useDirTree) {
			try {
				createLockFile(pid, label);
			} catch (IOException ex) {
				throw new IllegalArgumentException("Specified location can't be used as a log repository: " + directory, ex);
			}
		} else {
			ivSubDirectory = directory;
		}
	}
	
	/**
	 * stop logging and thus stop the timer retention thread
	 */
	public synchronized void stop() {
		if (timer != null) {
			timer.keepRunning = false;
			timer.interrupt();
			timer = null;
		}
		// Remove this manager from the space alert list
		LogRepositorySpaceAlert.getInstance().removeRepositoryInfo(this);
	}

	/**
	 * Configures constrain parameters of the repository.
	 * 
	 * @param maxRepositorySize maximum in bytes of the total sum of repository file sizes the manager should maintain.
	 * 			This limit is ignored if <code>maxRepositorySize</code> is less than or equal to zero.
	 * @param retentionTime the minimum time in milliseconds log records should be kept in the repository.
	 * 			This limit is ignored if <code>retentionTime</code> is less than or equal to zero.
	 */
	public synchronized void configure(long maxRepositorySize, long retentionTime) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "configure", "Log Records for Manager managing type: "+managedType+
				" for pid: "+svPid+" maxRepSz: "+maxRepositorySize+" retentionTm: "+retentionTime) ;
		}
		// Check only if reducing repository size
		boolean checkSpaceNow = maxRepositorySize > 0 && (this.maxRepositorySize <= 0 || maxRepositorySize < this.maxRepositorySize);
		
		this.maxLogFileSize = calculateFileSplit(maxRepositorySize);
		this.maxRepositorySize = maxRepositorySize;
		
		if (checkSpaceNow) {
			checkSpaceConstrain(maxLogFileSize);
		}
		
		if (timer == null) {
			if (retentionTime > 0) {
				timer = new TimerThread(retentionTime);
				timer.start();
			}
		} else {
			if (retentionTime <= 0) {
				stop();
			} else if (retentionTime != timer.retentionTime) {
				timer.retentionTime = retentionTime;
				timer.interrupt();
			}
		}
	}
	
	private final static long MIN_LOG_FILE_SIZE = 1024 * 1024; // 1MB
	private final static long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
	private final static long MIN_REPOSITORY_SIZE = 10 * 1024 * 1024; // 10MB
	private final static int SPLIT_RATIO = 20; // ratio of the repository size to the max file size
	
	/**
	 * calculates maximum size of repository files based on the required maximum limit
	 * on total size of the repository.
	 * 
	 * @param repositorySize space constrain on the repository.
	 * @return size constrain of an individual file in the repository.
	 */
	protected static long calculateFileSplit(long repositorySize) {
		if (repositorySize <= 0) {
			return MAX_LOG_FILE_SIZE;
		}
		if (repositorySize < MIN_REPOSITORY_SIZE) {
			throw new IllegalArgumentException("Specified repository size is too small");
		}
		
		long result = repositorySize / SPLIT_RATIO;
		
		if (result < MIN_LOG_FILE_SIZE) {
			result = MIN_LOG_FILE_SIZE;
		} else if (result > MAX_LOG_FILE_SIZE) {
			result = MAX_LOG_FILE_SIZE;
		}
		
		return result;
	}
	
	private static class FileDetails {
		final File file;
		final long timestamp;
		long size;
		String pid;
		FileDetails(File file, long timestamp, long size, String pid) {
			this.file = file;
			this.size = size;
			this.timestamp = timestamp;
			this.pid = pid;
		}
	}

	final LinkedList<FileDetails> fileList = new LinkedList<FileDetails>() ;
	
	long totalSize = -1L;
	
	/**
	 * Initializes file list from the list of files in the repository.
	 * This method should be called while holding a lock on fileList.
	 * 
	 * @param force indicator that the list should be initialized from the file
	 * 		system even if it was initialized before.
	 */
	private void initFileList(boolean force) {
		if (totalSize < 0 || force) {
			fileList.clear();
			parentFilesMap.clear();
			totalSize = 0L;
			File[] files = listRepositoryFiles();
			if (files.length > 0) {
				Arrays.sort(files, fileComparator);
				for (File file: files) {
					long size = AccessHelper.getFileLength(file);
					// Intentional here to NOT add these files to activeFilesMap since they are legacy
					fileList.add(new FileDetails(file, getLogFileTimestamp(file), size, null));
					totalSize += size;
					if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
						debugLogger.logp(Level.FINE, thisClass, "initFileList", "add: "+file.getPath()+" sz: "+size+
							" listSz: "+fileList.size()+" new totalSz: "+totalSize);
					}
					incrementFileCount(file);
				}
				debugListLL("fileListPrePop") ;
			}
				
			deleteEmptyRepositoryDirs();

			if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()){
				Iterator<File> parentKeys = parentFilesMap.keySet().iterator() ;
				while (parentKeys.hasNext()) {
					File parentNameKey = parentKeys.next() ;
					Integer fileCount = parentFilesMap.get(parentNameKey) ;
					debugLogger.logp(Level.FINE, thisClass, "initFileList", "  Directory: "+parentNameKey+" file count: "+ fileCount) ;									
				}
			}
		}		
	}
	
	/**
	 * Deletes all empty server instance directories, including empty servant directories
	 *
	 */
	protected void deleteEmptyRepositoryDirs() {
		File[] directories = listRepositoryDirs();
		
		//determine if the server/controller instance directory is empty
		for(int i = 0; i < directories.length; i++){
			// This is a directory we should not delete
			boolean currentDir = ivSubDirectory != null && ivSubDirectory.compareTo(directories[i])==0;
			//if a server instance directory does not have a key in parentFilesMap, then it does not have any files 
			if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
				debugLogger.logp(Level.FINE, thisClass, "deleteEmptyRepositoryDirs", "Instance directory name (controller): " + directories[i].getAbsolutePath());
			
			//now look for empty servant directories
			File[] childFiles = AccessHelper.listFiles(directories[i], subprocFilter) ;
			for (File curFile : childFiles) {
				if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
					debugLogger.logp(Level.FINE, thisClass, "deleteEmptyRepositoryDirs", "Servant directory name: " + curFile.getAbsolutePath());
				if (!currentDir && !parentFilesMap.containsKey(curFile)) {
					if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
		    			debugLogger.logp(Level.FINE, thisClass, "deleteEmptyRepositoryDirs", "Found an empty servant directory:  " + curFile);
					deleteDirectory(curFile);
				} else {
					incrementFileCount(curFile);
				}
			}
			//delete directory if empty
			if (!currentDir && !parentFilesMap.containsKey(directories[i])) {
				if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
					debugLogger.logp(Level.FINE, thisClass, "listRepositoryFiles", "Found an empty directory:  " + directories[i]);
				deleteDirectory(directories[i]);
			}
		}
	}
	
	/**
	 * Deletes the specified directory
	 * @param the name of the directory to be deleted
	 */
	protected void deleteDirectory(File directoryName){
		if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "deleteDirectory", "empty directory "+((directoryName == null) ? "None":
				directoryName.getPath()));
		}		
		if (AccessHelper.deleteFile(directoryName)) {  // If directory is empty, delete
			if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled()) {
				debugLogger.logp(Level.FINE, thisClass, "deleteDirectory", "delete "+directoryName.getName());
			}					
		} else {
			// Else the directory is not empty, and deletion fails
			if (isDebugEnabled()) {
				debugLogger.logp(Level.WARNING, thisClass, "deleteDirectory", "Failed to delete directory "+directoryName.getPath());
			}
		}
	}


	
	private void incrementFileCount(File file){
		File parentFile = file.getParentFile();
		Integer filecount = parentFilesMap.get(parentFile);
		if (filecount != null){
			parentFilesMap.put(parentFile, filecount+1);
		} else {
			parentFilesMap.put(parentFile, 1);
		}
	}
	
	private void decrementFileCount(File file){
		File parentFile = file.getParentFile();
		Integer filecount = parentFilesMap.get(parentFile);
		if (filecount != null){
			if (filecount == 1) {
				parentFilesMap.remove(parentFile);
				deleteDirectory(parentFile);
				decrementFileCount(parentFile);
			} else {
				parentFilesMap.put(parentFile, filecount-1);
			}
		}
	}
	
	/*
	 *  Make sure that repository has enough space for a new file
	 */
	private void checkSpaceConstrain(long logFileSize) {
		long spaceNeeded;
		if (maxRepositorySize > 0) {
			synchronized(fileList) {
				initFileList(false);
				long purgeSize = totalSize + logFileSize - maxRepositorySize;
				if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
					debugLogger.logp(Level.FINE, thisClass, "checkSpaceConstrain", "total: "+totalSize+
					" maxLog: "+logFileSize+" maxRepos: "+maxRepositorySize) ;
				}
				if (purgeSize > 0) {
					purgeOldFiles(purgeSize);
				}
				spaceNeeded = maxRepositorySize - totalSize;
			}
		} else {
			spaceNeeded = MAX_LOG_FILE_SIZE; // If no limit ensure that at least one log file can be written
		}
		LogRepositorySpaceAlert.getInstance().setRepositoryInfo(this, repositoryLocation, spaceNeeded) ;
	}
	
	/**
	 * Removes old files from the repository. This method does not remove
	 * the most recent file.
	 * This method should be called while holding a lock on fileList.
	 * 
	 * @param total the amount in bytes of required free space.
	 * @return <code>true</code> if at least one file was removed from the repository.
	 */
	private boolean purgeOldFiles(long total) {
		boolean result = false;
		// Should delete some files.
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "purgeOldFiles", "total: "+total+" listSz: "+fileList.size());
		}
		while(total > 0 && fileList.size() > 1) {
			FileDetails details = purgeOldestFile();
			if (details != null) {
				if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
					debugLogger.logp(Level.FINE, thisClass, "purgeOldFiles", "Purged: "+details.file.getPath()+" sz: "+details.size);
				}
				total -= details.size;
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Removes the oldest file from the repository. This method has logic to avoid removing currently active files
	 * This method should be called with a lock on filelist already attained	
	 * 
	 * @return instance representing the deleted file or <code>null</code> if
	 * 		fileList was reinitinialized.
	 */
	private FileDetails purgeOldestFile() {
		debugListLL("prepurgeOldestFile") ;
		debugListHM("prepurgeOldestFile") ;
		FileDetails  returnFD = getOldestInactive() ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "purgeOldestFile", "oldestInactive: "+((returnFD == null) ? "None":
				returnFD.file.getPath()));
		}
		if (returnFD == null)
			return null ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "purgeOldestFile", "fileList size before remove: "+fileList.size()) ;
		}
		
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "purgeOldestFile", "fileList size after remove: "+fileList.size()) ;
		}

		if (AccessHelper.deleteFile(returnFD.file)) {
			fileList.remove(returnFD) ;
			totalSize -= returnFD.size;
			if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
				debugLogger.logp(Level.FINE, thisClass, "purgeOldestFile", "delete: "+returnFD.file.getName());
			}
			decrementFileCount(returnFD.file);
			notifyOfFileAction(LogEventListener.EVENTTYPEDELETE) ;		// F004324
		} else {
			// Assume the list is out of sync.
			if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
				debugLogger.logp(Level.FINE, thisClass, "purgeOldestFile", "Failed to delete file: "+returnFD.file.getPath());
			}
			initFileList(true);
			returnFD = null;
		}
		debugListLL("postpurgeOldestFile") ;
		return returnFD;
	}
	
	private void debugListLL(String label) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "debugListLL", label+" w/fileList") ;
			synchronized(fileList) {
				for (FileDetails curFd : fileList) {
					debugLogger.logp(Level.FINE, thisClass, "debugListLL", "  Sz: "+curFd.size+" Pid: "+curFd.pid+" Nm: "+curFd.file.getPath()) ;
				}
			}
		}
	}

	private void debugListHM(String label) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "debugListHM", label+" hash") ;
			synchronized(fileList) {
				synchronized(activeFilesMap) {
					Iterator<String> activeKeys = activeFilesMap.keySet().iterator() ;
					while (activeKeys.hasNext()) {
						String pidKey = activeKeys.next() ;
						FileDetails thisFd = activeFilesMap.get(pidKey) ;
						debugLogger.logp(Level.FINE, thisClass, "debugListHM", "  Pid: "+pidKey+" timestamp: "+thisFd.timestamp) ;
					}
				}
			}
		}
	}
	
	public synchronized boolean purgeOldFiles() {
		synchronized(fileList) {
			initFileList(false);
			return purgeOldFiles(maxLogFileSize);
		}
	}

	/**
	 * add information about a new file being created by a subProcess in order to maintain retention information.  This is done for all
	 * files created by each subProcess. If IPC facility is not ready, subProcess may have to create first, then notify when IPC is up.
	 * @param spTimeStamp timestamp to associate with the file
	 * @param spPid ProcessId of the process that is creating the file (initiator of this action)
	 * @param spLabel Label to be used as part of the file name
	 * @return the full path of the file subprocess need to use for log records
	 */
	public synchronized String addNewFileFromSubProcess(long spTimeStamp, String spPid, String spLabel) {
		// TODO: It is theoretically possible that subProcess already created one of these (although it won't happen in our scenario.
		// Consider either pulling actual pid from the files on initFileList or looking for the file here before adding it.  If found,
		// adjust the pid to this pid.
		checkSpaceConstrain(maxLogFileSize) ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "addNewFileFromSubProcess", "Tstamp: "+spTimeStamp+
				" pid: "+spPid+" lbl: "+spLabel+" Max: "+maxLogFileSize);
		}
		
		if (ivSubDirectory == null)
			getControllingProcessDirectory(spTimeStamp, svPid) ;	// Note: passing, pid of this region, not sending child region
		
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "addNewFileFromSubProcess", "Got ivSubDir: "+ivSubDirectory.getPath());
		}
		File servantDirectory = new File(ivSubDirectory, getLogDirectoryName(-1, spPid, spLabel)) ;
		File servantFile = getLogFile(servantDirectory, spTimeStamp) ;
		FileDetails thisFile = new FileDetails(servantFile, spTimeStamp, maxLogFileSize, spPid) ;
		synchronized(fileList) {
			initFileList(false) ;
			fileList.add(thisFile) ;	// Not active as new one was created
			incrementFileCount(servantFile);
			synchronized(activeFilesMap) {	// In this block so that fileList always locked first
				activeFilesMap.put(spPid, thisFile) ;
			}
		}
		totalSize += maxLogFileSize ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "addNewFileFromSubProcess", "Added file: "+servantFile.getPath()+" sz:"+maxLogFileSize+" tstmp: "+spTimeStamp) ;
			debugListLL("postAddFromSP") ;
			debugListHM("postAddFromSP") ;
		}
		return servantFile.getPath() ;
	}

	/**
	 * inactivate active file for a given process.  Should only be one file active for a process
	 * @param spPid
	 */
	public void inactivateSubProcess(String spPid) {
		synchronized (fileList) {		// always lock fileList first to avoid deadlock
			synchronized(activeFilesMap) {		// Right into sync block because 99% case is that map contains pid
				activeFilesMap.remove(spPid) ;
			}
		}
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "inactivateSubProcess", "Inactivated pid: "+spPid) ;
		}
	}
	
	private final static long FAILED_WAIT_TIME = 1000; // sleep for 1sec.
	private final static long FAILED_MAX_COUNT = 5; // fall asleep 5 times.
	
	/*
	 * Calculates location of the new file with provided timestamp.
	 * It should be called with the instance lock taken.
	 */
	private File calculateLogFile(long timestamp) {
		if (ivSubDirectory == null)
			getControllingProcessDirectory(timestamp, svPid) ;
		
		return getLogFile(ivSubDirectory, timestamp);
	}
	
	public synchronized File startNewFile(long timestamp) {
		
		okToNotify = false ;
		checkSpaceConstrain(maxLogFileSize);
		
		File file = calculateLogFile(timestamp);
		
		File dir = file.getParentFile();
		if (!AccessHelper.isDirectory(dir)) {
			AccessHelper.makeDirectories(dir);
		}
		
		FileDetails thisFile = new FileDetails(file, timestamp, 0, svPid);
		synchronized(fileList) {
			initFileList(false);
			fileList.add(thisFile);
			incrementFileCount(file);
			synchronized(activeFilesMap) {		// This in this loop to make sure fileList always locked first
				activeFilesMap.put(svPid, thisFile) ;
			}
		}
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "startNewFile", "Added file: "+file.getPath()+" sz: 0  tstmp: "+timestamp) ;
			debugListLL("postAddLocal") ;
			debugListHM("postAddLocal") ;
		}
		
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "startNewFile", "roll");
		}
		// F004324, NOT notifying of roll here. If caller used this for a roll, then caller must register the roll
		okToNotify = true ;		// Callers can now send notifications. 

		if (timer != null) {
			// Let timer thread know about the new file.
			timer.interrupt();
		}
		
		return file;
	}
	
	/**
	 * get the directory of the controlling process (this process). Used for this process and for prefixing the paths of
	 * child process files (this process does the naming for those files).
	 * @param timestamp
	 * @param pid
	 */
	private void getControllingProcessDirectory(long timestamp, String pid) {
		int count = 0;
		while (ivSubDirectory == null) {
			ivSubDirectory = makeLogDirectory(timestamp, pid);
			if (ivSubDirectory == null) {
				if (++count > FAILED_MAX_COUNT) {
					ivSubDirectory = makeLogDirectory(timestamp, pid,true);
					if(ivSubDirectory==null){
						if (debugLogger.isLoggable(Level.FINE) && isDebugEnabled())
						{
							debugLogger.logp(Level.FINE, thisClass, "UnableToMakeDirectory", "Unable to create instance directory forcefully , throwing Runtime Exception");
						}
					  throw new RuntimeException("Failed to create instance log repository. See SystemOut.log for details.");
				    }
				}
				try {
					Thread.sleep(FAILED_WAIT_TIME);
				} catch (InterruptedException ex) {
					// Ignore it, assume that we had enough sleep.
				}
			}
		}
	}
	
	public synchronized File checkForNewFile(long total, long timestamp) {
		// Check if file rotation is necessary.
		synchronized(activeFilesMap) {		// This is a quick read w/no fileList access
			FileDetails pidCurrentFd = activeFilesMap.get(svPid) ; 
			// File rotation is not necessary if size hasn't reached maxLogFileSize or
			// if rotation would override currently active file.
			if (total <= maxLogFileSize || pidCurrentFd.file.equals(calculateLogFile(timestamp))) {
				pidCurrentFd.size = total;
                        // We exceeded the maximum log file size so guarantee we have enough space
                        if (total > maxLogFileSize) {
                            checkSpaceConstrain(total);
                         }

				return null;
			}
			totalSize += pidCurrentFd.size ;
		}
		
		return startNewFile(timestamp);
	}

		// F004324 related methods
	/**
	 * register a logEventNotifier with this logging manager.  This instructs the manager that, on file actions, a
	 * notification should be sent out (then the notifier is responsible for notifying all registered listeners).
	 * The notifier can be null which means that it is functionally deRegistering itself as there are no more
	 * listeners (ie: the last listener just deRegistered
	 */
       @Override
	public void setLogEventNotifier(LogEventNotifier logEventNotifier) {
		if (logEventNotifier == null) 
                return ;
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "setLogEventNotifier", "managedTp: "+managedType);
		}
		super.setLogEventNotifier(logEventNotifier) ;
			// the return is the oldest since it is not pid-specific.  At this point, in child processes (servants), this does not work
			// since child processes do not remove files
		if (TRACETYPE.equals(managedType) || LOGTYPE.equals(managedType)) {		// Only for log and trace
			repositoryType = (TRACETYPE.equals(managedType)) ?					// Set repository type
				LogEventListener.REPOSITORYTYPETRACE : LogEventListener.REPOSITORYTYPELOG ;
			logEventNotifier.setOldestDate(getOldestDate(), repositoryType) ;
			if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
				debugLogger.logp(Level.FINE, thisClass, "setLogEventNotifier", "repTp: "+repositoryType+" oldDt: "+getOldestDate());
			}
		}
	}
	@Override
	public void notifyOfFileAction(String eventType) {
		if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
			debugLogger.logp(Level.FINE, thisClass, "notifyOfFileAction", "rTp: "+repositoryType+" EvtTp: "+eventType+
				" listener: "+logEventNotifier+" Dt: "+getOldestDate()+" Ok to notify: "+okToNotify);
		}
		if (logEventNotifier != null) {
			if (okToNotify) {
				if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
					debugLogger.logp(Level.FINE, thisClass, "notifyOfFileAction", "clearing queue") ;
				}
				synchronized (notificationQueue) {
					if (notificationQueue.size() > 0) {
						for (EventContents eventContents : notificationQueue) {
							logEventNotifier.recordFileAction(eventContents.eventType, eventContents.repositoryType, 
								eventContents.dateOldestLogRecord) ;
						}
						notificationQueue.clear() ;
					}
				}
				logEventNotifier.recordFileAction(eventType, repositoryType, getOldestDate()) ;
			} else {
				if (debugLogger.isLoggable(Level.FINE) && LogRepositoryBaseImpl.isDebugEnabled()) {
					debugLogger.logp(Level.FINE, thisClass, "notifyOfFileAction", "queueing event") ;
				}
				synchronized (notificationQueue) {
					notificationQueue.add(new EventContents(eventType, repositoryType, getOldestDate())) ;
				}
			}
		}
	}

	/**
	 * get oldest file in cache that is not currently in use by a pid (ie: current log/trace/textLog for some process)
	 * This method assumes that fileList is already sync'd
	 * @return a FileDetails object representing the oldest inactive file in repository (prime candidate for removal)
	 */
	private FileDetails getOldestInactive() {
		for (FileDetails curFd : fileList) {
			if (curFd.pid == null)
				return curFd ;
			synchronized(activeFilesMap) {
				FileDetails cur4Pid = activeFilesMap.get(curFd.pid) ;
				if ( cur4Pid == null || cur4Pid != curFd) {
					return curFd ;
				}
			}
		}
		return null ;
	}

	private Date getOldestDate() {
		synchronized (fileList) {		// Capture oldest file in link list
			if (fileList.size() < 1)	// If nothing in the FileList (no files of this type created yet) ... return current time
				return null ;
			FileDetails oldFd = fileList.element() ;
			return new Date(oldFd.timestamp) ;
		}
	}

	/*
	 * Thread maintaining <code>retentionTime</code> limitation.
	 */
	private class TimerThread extends Thread {
		private final static long IDLE_SLEEP = 60 * 60 * 1000; // An hour
		private long retentionTime;
		private boolean keepRunning = true;
		
		TimerThread(long retentionTime) {
			this.retentionTime = retentionTime;
		}

		@Override
		public void run() {
			FileDetails target = null;
			long sleepTime = IDLE_SLEEP;
			while(keepRunning) {
				synchronized(fileList) {
					// Make sure the file list is initialized.
					initFileList(false);
					
					// Delete only if there's more than one file in the repository
					// and target is still the oldest file in the list.
					if (fileList.size() > 1 && target == getOldestInactive()) {
						purgeOldestFile();
					}
					
					// Find the next target and the duration of sleep.
					if (fileList.size() < 2) {
						target = null;
						sleepTime = IDLE_SLEEP;
					} else {
						target = getOldestInactive() ;
						long current = System.currentTimeMillis();
						sleepTime = target.timestamp + retentionTime - current;
					}
				}
				
				if (sleepTime > 0) {
					try {
						sleep(sleepTime);
					} catch (InterruptedException ex) {
						// We wake up too early. Need to check again if a file should be deleted.
						target = null;
					}
				}
			}
		}
		
	}

}
