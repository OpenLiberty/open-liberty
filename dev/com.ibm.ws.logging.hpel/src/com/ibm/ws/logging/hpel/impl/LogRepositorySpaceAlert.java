/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.logging.hpel.LogRepositoryManager;

/**
 * Periodically check all repositories and warn if space appears to be an issue
 * log repository.
 */
public class LogRepositorySpaceAlert {
	// Be careful with the logger since the code in this class is used in logging logic itself
	// and may result in an indefinite loop.
	public final static long   CHECK_INTERVAL = 2 * 60 * 1000 ;	// 2 minutes in milliseconds between checks
	private final static LogRepositorySpaceAlert logRepositorySpaceAlert = new LogRepositorySpaceAlert();		// Singleton, so this is instance
	
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = LogRepositorySpaceAlert.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);

	private final static HashMap<LogRepositoryManager, RepositoryInfo> repositoryInfo = new HashMap<LogRepositoryManager, RepositoryInfo>();	// Repositories to check
	private final static HashMap<File, FileSystemInfo> fsInfo = new HashMap<File, FileSystemInfo>(); // File system roots

	private final Timer     fileSystemCheckTimer = AccessHelper.createTimer() ;			// Timer infrastructure
	private final TimerTask fileSystemCheckTask = new FileSystemCheckTask();

	private class FileSystemCheckTask extends TimerTask {					// Class with timer task
		public void run() {
			if (logger.isLoggable(Level.FINER))
				logger.logp(Level.FINER, className, "FileSystemCheckTask.run", "Checking w/repository cnt {0} and filesystem cnt {1}", new Object[]{repositoryInfo.size(), fsInfo.size()}) ;
			checkFileSystems() ;
		}
	}

	/**
	 * <code>RepositoryInfo</code> is an internal class holding key information for each repository for which we are verifying space.
	 * The actual monitoring is done on the fileSystem ... but the repositories provides the thresholds for which we monitor.
	 * @author mcasile
	 *
	 */
	private static class RepositoryInfo {			// For each repository, keep running information
		// File system this repository is located on.
		final FileSystemInfo fs ;
		// Fee space requirement by this repository
		long   repositorySpaceNeeded = 0;
		RepositoryInfo(FileSystemInfo fs) {
			this.fs = fs ;
		}
		
		/**
		 * Update free space requirement of this repository. This call also updates
		 * free space requirement for the file system this repository is located on
		 * @param repositorySpaceNeeded new free space requirement
		 */
		void setRespositorySpaceNeeded(long repositorySpaceNeeded) {
			this.fs.fsSpaceNeeded += repositorySpaceNeeded - this.repositorySpaceNeeded;
			this.repositorySpaceNeeded = repositorySpaceNeeded;
		}
	}
	
	/**
	 * <code>FileSystemInfo</code> is an internal class holding information for each file system
	 */
	private static class FileSystemInfo {
		// Free space requirement by all repositories located on this file system
		long fsSpaceNeeded = 0;
		// Number of lack of free space noticed in a row
		private int numberOfNotifications = 0;
		// Number of file system checks skipped so far
		private int cyclesSkipped = 0;
		
		/**
		 * process through information to determine if we should put out a warning at this time.  This is an attempt to not
		 * put out a warning every n seconds .. but that after a few warnings are put out, we increase time between warnings
		 * @return true if we should process this time
		 */
		boolean processThisCycle() {
			if (cyclesSkipped < numberOfNotifications) {
				cyclesSkipped++;
				return false;
			} else {
				cyclesSkipped = 0;
				return true;
			}
		}
		
		/**
		 * Checks if free space requirement is met.
		 * @param freeSpace currently available free space
		 * @return <code>true</code> if there's enough free space, <code>false</code> otherwise.
		 */
		boolean isWarning(long freeSpace) {
			if (freeSpace < fsSpaceNeeded) {
				numberOfNotifications++;
				return true;
			} else {
				numberOfNotifications = 0;
				return false;
			}
		}
	}

	/**
	 * get the singleton instance of <code>LogRepositorySpaceAlert</code>
	 * @return the instance
	 */
	public static LogRepositorySpaceAlert getInstance() {
		return logRepositorySpaceAlert ;
	}
	
	private LogRepositorySpaceAlert() {		// On construction, start timer task
		fileSystemCheckTimer.schedule(fileSystemCheckTask, new Date(System.currentTimeMillis()+CHECK_INTERVAL), CHECK_INTERVAL) ;
	}

	/**
	 * Set a repository into the space alert for checking. In HPEL, the LogManager for log and the LogManager for
	 * Trace should set themselves as repositories to watch.  This is done on the opening of each file.  Thus, if
	 * a destination is moved, a file is rolled, or a server is started, the info will be updated
	 * @param repositoryType Unique identifier of this repository (in HPEL, it is log/trace/text). Since managers will
	 * reSet with each file they create or any time they change locations .. if the same type drives set again, the row
	 * in the arrayList is updated
	 * @param repositoryLocation location where this repository will log
	 * @param repositorySpaceNeeded Current amount of space needed. This is determined by the caller.  For our purposes,
	 * if the repository is using space, then it is the maxSpace - currently used space.  If it is going by time, then
	 * it is based on a fixed value which is a constant in this class.
	 */
	public synchronized void setRepositoryInfo(LogRepositoryManager manager, File repositoryLocation, long repositorySpaceNeeded) throws IllegalArgumentException {
		if (manager == null)   throw new IllegalArgumentException("Null manager passed to LogRepositorySpaceAlert.setRepositoryInfo") ;
		if (repositoryLocation == null)   throw new IllegalArgumentException("Null repositoryLocation passed to LogRepositorySpaceAlert.setRepositoryInfo") ;
		/* Don't do logging here since it would cause infinite loop with *=all trace spec.
		if (logger.isLoggable(Level.FINER))
			logger.logp(Level.FINER, className, "setRepositoryInfo", "Args: manager: "+manager+" loc: "+
				repositoryLocation.getPath()+" rSpc: "+repositorySpaceNeeded) ;
		 */
		RepositoryInfo curRI = repositoryInfo.get(manager);
		if (curRI == null) {
			File fsRoot = calculateFsRoot(repositoryLocation);
			FileSystemInfo fs = fsInfo.get(fsRoot);
			if (fs == null) {
				fs = new FileSystemInfo();
				fsInfo.put(fsRoot, fs);
			}
			curRI = new RepositoryInfo(fs);
			repositoryInfo.put(manager, curRI);
		}
		curRI.setRespositorySpaceNeeded(repositorySpaceNeeded);
	}

	/**
	 * remove a type from the array.  One use here would be if/when textLog is disabled
	 * or when repository location is changed
	 * @param repositoryType type of repository
	 */
	public synchronized void removeRepositoryInfo(LogRepositoryManager manager) {
		RepositoryInfo curRI = repositoryInfo.remove(manager);
		if (curRI != null) {
			curRI.setRespositorySpaceNeeded(0);
		}
	}
	
	public synchronized void stop() {
		fileSystemCheckTimer.cancel() ;
		repositoryInfo.clear();
		fsInfo.clear();
	}
	
	/**
	 * check the file systems to see if there is ample room
	 * Gets information from configuration and correlates it to the space left on the volume to see if it looks like we are
	 * in jeopardy.  An attempt is made to determine if log and trace are on the same fs. If so, then the combined impact
	 * is assessed.
	 * @throws Exception
	 */
	private synchronized void checkFileSystems() {
		for(Entry<File, FileSystemInfo> fsEntry: fsInfo.entrySet()) {
			if (fsEntry.getValue().processThisCycle()) {
				long fsFreeSpace = AccessHelper.getFreeSpace(fsEntry.getKey()) ;
				if (logger.isLoggable(Level.FINE)) {
					logger.logp(Level.FINE, className, "checkFileSystems", "fileSystem: "+fsEntry.getKey().getPath()+
							" Need: "+fsEntry.getValue().fsSpaceNeeded+" Available Spc: "+fsFreeSpace) ;
				}
				if (fsEntry.getValue().isWarning(fsFreeSpace)) {
					logger.logp(Level.WARNING, className, "checkFileSystems", "HPEL_FileSystem_Space_Warning",
							new Object [] { fsEntry.getKey().getPath(), fsEntry.getValue().fsSpaceNeeded, fsFreeSpace } ) ;
				}
			}
		}
	}
	
	/**
	 * determine if the repository location is fine as is, or if it may be shortened. Shortening is hpel specific in
	 * that we look for logdata or tracedata as the suffix and, if it is there AND the parent directory is of about
	 * the same size, then we keep the parent directory.  This allows us to combine needs of different repositories
	 * that may be in the same fileSystem.  ie: If logdata, tracedata, and textLog all need just 20 meg each and they
	 * are on the same fs which has 50 meg left, the only way we know that this is a problem is if we consider the
	 * combined need.  By modifying the repositoryLoc, this should work for the vast majority of cases.
	 * @param repositoryLocation incoming repository location
	 * @return same or modified repository location depending on file system information
	 */
	private File calculateFsRoot(File repositoryLocation) {
		// Find first existing directory among ancestors
		while (!AccessHelper.isDirectory(repositoryLocation)) {
			repositoryLocation = repositoryLocation.getParentFile();
		}
		if (repositoryLocation.getName().equals(LogRepositoryBaseImpl.DEFAULT_LOCATION) ||
				repositoryLocation.getName().equals(LogRepositoryBaseImpl.TRACE_LOCATION)) {
			File parentFile = repositoryLocation.getParentFile() ;
			if (AccessHelper.getTotalSpace(repositoryLocation) == AccessHelper.getTotalSpace(parentFile)) {
				long thisFsSpace = AccessHelper.getFreeSpace(repositoryLocation) ;
				long parentFsSpace = AccessHelper.getFreeSpace(parentFile) ;
				// If it ends in tracedata or logdata and parent directory and this directory are highly similar (within 5%)
				// in free space ... assume we can cut off the tracedata or logdata because on same fs. This means that
				// in the case where the logs are the same fs ... we will consider combined needs.
				if (thisFsSpace > 0 && parentFsSpace > 0 && thisFsSpace > (long)(parentFsSpace * .95) &&
						thisFsSpace < (long)(parentFsSpace * 1.05)) {
					repositoryLocation = parentFile ;
				}
			}
		}
		return repositoryLocation ;
	}
}
