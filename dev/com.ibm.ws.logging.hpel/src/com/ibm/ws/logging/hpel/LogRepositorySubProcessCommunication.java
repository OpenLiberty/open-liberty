/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel;

/**
 * <code>LogRepositorySubProcessCommunication</code> is an API to be implemented by some agent that can perform IPC starting at
 * a child process and serviced by the parent process.  It is intended to allow any form of IPC desired
 */
public interface LogRepositorySubProcessCommunication {
	/**
	 * notify the controlling process that this process is creating a file.  The implementation will return a string
	 * that represents the name of the file that should be created.  It will also notify the controlling process so
	 * that any retention and caching can be handled.
	 * @param spTimeStamp subProcess timestamp used as part of name generation.  This is passable here in case there are
	 * scenarios where records may already be somehow queued and the timeStamp for file creation should be different
	 * than the current timestamp
	 * @param spPid subProcess PID used as part of name generation
	 * @param spLabel subProcess label identifying info that should help a user identify the context of the subProcess
	 * @return String that is the name of the file which should be created
	 */
	public String notifyOfFileCreation(String destinationType, long spTimeStamp, String spPid, String spLabel) ;

	/**
	 * request that the controlling process remove log files from the repository to free up space.  One common cause of
	 * this call is an IOException where, for example, a repository was specified to allow a max size of 50Mb, but the
	 * file system had 40Mb of space (so even though we are < 50Mb, we can write no more).  Any calls to this method are
	 * an exception basis.  Normal processing should not involve calling this method.
	 * @return true if some files were removed ... false if no files were able to be removed
	 */
	public boolean removeFiles(String destinationType) ;

	/**
	 * request that the controlling process mark this subProcess as no longer active.  This makes all files associated with
	 * this process eligible for removal by retention processing (based on the retention criteria)
	 * @param spPid subProcess PID for which no logging type files are to be considered active
	 */
	public void inactivateSubProcess(String spPid) ;
}
