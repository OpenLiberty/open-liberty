/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.logging.hpel.LogFileWriter;
import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.websphere.logging.hpel.writer.LogEventListener;


/**
 * Implementation of the {@link LogRepositoryWriter} interface storing files in a
 * log repository.
 */
public class LogRepositoryWriterImpl implements LogRepositoryWriter {
	// Be careful with the logger since the code in this class is used in logging logic itself
	// and may result in an indefinite loop.
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = LogRepositoryWriterImpl.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);

	private final LogRepositoryManager manager;
	/** byte array representing header information. */
	private byte[] headerBytes = null;  // value 'null' means that writer is closed.
	private int outOfSpaceAction = 0;
	protected boolean bufferingEnabled = true;


	private LogFileWriter writer = null;
	private int index = 0;	// index of a record in a file. -1 means that
							// close() was already issued.

	//fields for file switching (cutting)
	private static final long SWITCH_PERIOD = 24 * 60 * 60 * 1000;    //24 hours in terms of milliseconds
	private static final int MIN_SWITCH_HOUR=0;                       //0 = midnight
	private static final int MAX_SWITCH_HOUR=23;                      //11PM

	private Timer fileSwitchTimer = null;                             //timer daemon to schedule the file switching
	private Date fileSwitchTime = new Date();                         //time to indicate time of day to switch files
	private final TimerTask fileSwitchTask = new FileSwitchTask();



	private class FileSwitchTask extends TimerTask {
		public void run() {
			switchFile();
		}
	}


	/**
	 * Creates an instance to be used as a log record writer.
	 *
	 * @param manager the file manager to locate correct file to write to.
	 */
	public LogRepositoryWriterImpl(LogRepositoryManager manager) {
		this.manager = manager;
	}


	/**
	 * Returns manager used by this writer.
	 *
	 * @return manager configured during construction of this writer.
	 */
	public LogRepositoryManager getLogRepositoryManager() {
		return this.manager;
	}

	public synchronized void setHeader(byte[] headerBytes) {
		this.headerBytes = new byte[headerBytes.length];
		System.arraycopy(headerBytes, 0, this.headerBytes, 0, headerBytes.length);
	}

	/**
	 * Sets new outOfSpaceAction.
	 *
	 * @param type the type of the action to use when IOException occures.
	 */
	public synchronized void setOutOfSpaceAction(int type) {
		this.outOfSpaceAction = type;
	}

	/**
	 * Sets new bufferingEnabled.
	 *
	 * @param bufferingEnabled indicator if buffering should be enabled.
	 */
	public synchronized void setBufferingEnabled(boolean bufferingEnabled) {
		this.bufferingEnabled = bufferingEnabled;
	}

	private synchronized void switchFile(){
		if (writer != null){
			try{
				writer.close(headerBytes);
				index = 0;
				writer = null;
			}
			catch(IOException e){
				// No need to crash on this error even if the tail won't be written
				// since reading logic can take care of that.
			}
		}
	}

	private final static class RecordCache {
		final long timestamp;
		final byte[] bytes;
		RecordCache(long timestamp, byte[] bytes) {
			this.timestamp = timestamp;
			this.bytes = bytes;
		}
	}
	private boolean isInitializing = false;
	private final ArrayList<RecordCache> cache = new ArrayList<RecordCache>();

	public synchronized void logRecord(long timestamp, byte[] bytes) {
		
		// Prevent loop invocation.
		if (isInitializing) {
			cache.add(new RecordCache(timestamp, bytes));
			return;
		}

		// Try complete writing while handler is still open.
		while(headerBytes != null) {
			try {
				isInitializing = true;
				
				writeHeader(timestamp);

				// Don't check for rotation on the first record in the file to avoid creating files with a header but no records.
				if (index > 0) {
					File next = manager.checkForNewFile(writer.checkTotal(bytes, headerBytes), timestamp);
					if (next != null) {
						writer.close(headerBytes);
						index = 0;
						writer = createNewWriter(next);
						writer.write(headerBytes);
						manager.notifyOfFileAction(LogEventListener.EVENTTYPEROLL) ;
					}
				}

				writer.write(bytes);
				writer.flush();
				index++;

			} catch (IOException ex) {
				if (outOfSpaceAction == 2) { // StopLogging
					stop();
				} else if (outOfSpaceAction == 1) { // PurgeOld
					// If some files were removed try writing again.
					if (manager.purgeOldFiles()) {
						continue;
					}
				} else if (outOfSpaceAction == 0) { // StopServer
					disableFileSwitch();        // Cancel timer thread
					System.exit(-1) ;			// Allow signal catchers to capture exit for smooth shutDown
				}
				// Otherwise just loose some messages.
			} 
            catch(RuntimeException e)
            {
	         if (outOfSpaceAction == 2) { // StopLogging
         		stop();
           	 } else if (outOfSpaceAction == 0) { // StopServer
		        disableFileSwitch();        // Cancel timer thread
		        System.exit(-1) ;			// Allow signal catchers to capture exit for smooth shutDown
	           }
	// Otherwise just loose some messages.	
			} finally {
				isInitializing = false;
			}

			break;
		}
		
		if (!cache.isEmpty()) {
			ArrayList<RecordCache> copy = new ArrayList<RecordCache>();
			copy.addAll(cache);
			cache.clear();
			for (RecordCache record: copy) {
				logRecord(record.timestamp, record.bytes);
			}
		}
		
	}
	
	/**
	 * Publishes header if it wasn't done yet.
	 * 
	 * @param timestamp creation time to use on the new file if it needs to be created
	 */
	public synchronized void writeHeader(long timestamp) throws IOException {
		if (writer == null && headerBytes != null) {
			writer = createNewWriter(manager.startNewFile(timestamp));
			writer.write(headerBytes);
			manager.notifyOfFileAction(LogEventListener.EVENTTYPEROLL) ;
		}
	}

	/**
	 * Stops this writer and close its output stream.
	 */
	public synchronized void stop() {
		if (writer != null) {
			try {
				writer.close(headerBytes);
				writer = null;
			} catch (IOException ex) {
				// No need to crash on this error even if the tail won't be written
				// since reading logic can take care of that.
			}
		}
		// Ensure that timer is stopped as well.
		disableFileSwitch();
		
		headerBytes = null;
		// Don't stop manager here since it can be reused for a different repository writer.
		//manager.stop();
	}

	/**
	 * Creates new instance of a writer to write into given file.
	 *
	 * @param file File instance writer will write into.
	 * @return ILogFileWriter instance for writing bytes into the file
	 * @throws IOException
	 * @see LogFileWriter
	 */
	protected LogFileWriter createNewWriter(File file) throws IOException {
		return new LogFileWriterImpl(file, bufferingEnabled);
	}


	/**
	 * Enables file switching for the writer by configuring the timer to set a trigger based on the switchHour parm
	 *
	 * @param switchHour     the hour of the day for the file switching to occur, must be between the values of 0 through 23
	 */
	public void enableFileSwitch(int switchHour) {

		if(fileSwitchTimer == null){
			fileSwitchTimer = AccessHelper.createTimer();
		}

        //set calendar instance to the specified configuration hour for cutting
		//default to midnight when the passed in value is invalid, or midnight is specified (to avoid negative value when 1 is subtracted
		if(switchHour < MIN_SWITCH_HOUR || switchHour > MAX_SWITCH_HOUR ){
			// It's OK to use logger here since adding logging record will not result in changing
			// file switching configuration.
			logger.logp(Level.WARNING, className, "enableFileSwitch", "HPEL_IncorrectSwitchHour", new Object[]{switchHour,MIN_SWITCH_HOUR, MAX_SWITCH_HOUR,MIN_SWITCH_HOUR});
			switchHour = MIN_SWITCH_HOUR;
		}


		//Note:  We will set the file cutting time to match the exact hour that was specified, but it's possible for the timestamp attribute of the file
		//to go beyond the switchTime, as the final writes to close the file will alter the timestamp attribute.
		//For example, if a fileSwitch was set to midnight, log records that are written after midnight will go to a new file, but it's possible for the
		//previous file being closed to have a midnight timestamp attribute.
		Calendar currentTime = Calendar.getInstance();
		Calendar switchTime = currentTime;
		switchTime.set(Calendar.HOUR_OF_DAY, switchHour);
		switchTime.set(Calendar.MINUTE, 00);
		switchTime.set(Calendar.SECOND, 00);

		//if the time has already passed, then set to the next day.  Otherwise
		//the timer will catch up on the missed tasks that would've been executed.
		//For example:  If the switchTime was set to 11 (for 11AM), and the server gets started after 11AM, we do not want the file switch
		//to occur until 11AM of the following day.
		if(currentTime.after(switchTime)){
			switchTime.add(Calendar.DATE, 1);
		}

		fileSwitchTime.setTime(switchTime.getTimeInMillis());
		fileSwitchTimer.scheduleAtFixedRate(fileSwitchTask, fileSwitchTime, SWITCH_PERIOD);
	}

	/**
	 * Disables file switching for the writer by canceling scheduled tasks, if file switching was previously enabled.
	 */
	public void disableFileSwitch(){
		if(fileSwitchTimer != null ){
			fileSwitchTimer.cancel();
		}
	}

}
