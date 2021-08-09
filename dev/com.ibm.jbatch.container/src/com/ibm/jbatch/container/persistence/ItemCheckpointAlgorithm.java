/*
 * Copyright 2012,2014 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.persistence;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.chunk.CheckpointAlgorithm;

public final class ItemCheckpointAlgorithm implements CheckpointAlgorithm {

	private static final String className = ItemCheckpointAlgorithm.class.getName();
	private static Logger logger  = Logger.getLogger(ItemCheckpointAlgorithm.class.getPackage().getName());;
		
    long itemsRead = 0;
    protected int itemCount;
    protected int timeLimitSeconds;
    long checkpointBeginTime = 0;
    
    public void setItemCount(int itemCount) {
    	this.itemCount = itemCount;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
    	this.timeLimitSeconds = timeLimitSeconds;
    }

    @Override
	public void endCheckpoint() throws Exception { }

	public boolean isReadyToCheckpointItem() {

        boolean itemready = (itemsRead >= itemCount);

        if ( itemready) {

        	logger.fine("ITEMCHKPT: item checkpoint hit");

        	if(logger.isLoggable(Level.FINER)) { 
        		long millis =  Long.valueOf( (new Date().getTime()) - checkpointBeginTime );
        		if ( millis>0 ) { 
        			String rate =  Integer.valueOf ( Long.valueOf( (itemsRead*1000/millis) ).intValue()).toString();
        			logger.finer(" - true [itemsReadAndProcessed/second " + rate + "]");
        		} else {
        			logger.finer(" - true [itemsReadAndProcessed " + itemsRead + "]");
        		}
        	}
        }

        return itemready;
	}
	
	public boolean isReadyToCheckpointTime() {
    	String method = "isReadyToCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

        boolean timeready = false;
        long curts = System.currentTimeMillis();
        long curdiff = curts - checkpointBeginTime;
        int diff = (int)curdiff / 1000;
        
        if (diff >= timeLimitSeconds) {
        	logger.fine("ITEMTIMECHKPT: time checkpoint hit");
            timeready = true;
            if(logger.isLoggable(Level.FINER)) { logger.finer("Num of itemsReadAndProcessed="+ itemsRead +" at a rate="+itemsRead/diff+" itemsReadAndProcessed/sec");}
        }

        if(logger.isLoggable(Level.FINER)) { logger.exiting(className, method, timeready); }

        return timeready;
	}
	

	@Override
	public boolean isReadyToCheckpoint() {

		itemsRead++;
			
		if (isReadyToCheckpointItem()){
			return true;
		}

		if (timeLimitSeconds > 0 && isReadyToCheckpointTime()) {
			return true;
		}
		
		return false;
	}

	@Override
	public void beginCheckpoint() throws Exception {
        checkpointBeginTime = System.currentTimeMillis();
        itemsRead = 0;
	}

	@Override
	public int checkpointTimeout() throws Exception {
		return 0;
	}

}
