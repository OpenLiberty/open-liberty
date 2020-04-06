/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.load.Weighable;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author anat, Jan 1, 2005
 *
 * Class that responsible to find the most high server weight 
 * and update the server weight.
 */
public class LoadManager {
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(LoadManager.class);
    
    /**
	 * Defines the size of low water mark . By default is 50% of the stepSize.
	 * Used By LoadCounterAbs class.
	 */
	private int _lowWaterMarkSize = CoreProperties.LOW_WATER_MARK_SIZE_DEFAULT;
	
	 /**
	 * Defines the weight which will switch the container to the "overload" state.
	 * Meaning - when the weight will be 0 - the attribute "overloaded" will be
	 * passed to UCF. After the overload state only when the 3 weight will be 
	 * reached - the "overloaded" attribute will be removed.
	 */
	private int _weightOverloadMark = CoreProperties.WEIGHT_OVERLOAD_MARK_DEFAULT;

    /**
     * The last weight of the server
     */
    private int _lastLoad = 10;
    
    private boolean _isOverloaded = false;

	/**
	 * Flag which defines if container should throw all incoming messages
	 * or not. Used when container is overloaded. Then overloaded the
	 * notification will be passed to the proxy but meanwhile sip Contianer
	 * will received new message to get the change for the proxy to reject the
	 * messages of forward them to another container.
	 */
    private boolean _throwMessagedInOverload;
   
    /** Singlton */
    private static LoadManager s_singelton;
    
    public static final int QUIESCE_MODE = 0;
    
    public static final int SET_REAL_CURRENT_WEIGHT = -1;


    /**
     * Gets the single instance of Performance Manager
     * 
     * @return pointer to the PerformanceMgr
     */
    public static LoadManager getInstance() {
        if (s_singelton == null) {
            s_singelton = new LoadManager();
        }
        return s_singelton;
    }

    /**
     * Ctor
     * Created the Load object and add it to the _table and _sortedList;
     */
    private LoadManager()
    {
    	_lowWaterMarkSize = 
			PropertiesStore.getInstance().getProperties().getInt(
					CoreProperties.LOW_WATER_MARK_SIZE);
    	
    	_weightOverloadMark = 
			PropertiesStore.getInstance().getProperties().getInt(
					CoreProperties.WEIGHT_OVERLOAD_MARK);
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "LoadManager", 
					"Low watermark size that will used is = " + _lowWaterMarkSize);
		}

        _lastLoad = 11;
        
       	//  defect 448307.1
		// We will not update server weight anymore as Proxy is not
		// reading this parameter and we passed the
		// customer performance tests without this update
        // Start with a lower load to let the server warm up.
    	// After the first message, it will change,
        
// 		updateServer(PerfUtil.INITIAL_INT,PerfUtil.INITIAL_WEIGHT);       
    }
    
    
    /**
     * 
     * Updates load state and announces or clears overload message 
     * 
     * @param type
     * @param newWeight
     * @return true if the weight was changed
     */
    public synchronized boolean updateNewWeight(Weighable counter,
    													int newWeight,
    													long currentLoad) {
    	
    	if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("Try to update new weight from ");
			buff.append(PerfUtil.getTypeStr(counter.getCounterID()));
			buff.append(" New Weight = ");
			buff.append(newWeight);
			buff.append(" Was overloaded = ");
			buff.append(_isOverloaded);
			c_logger.traceDebug(this, "updateNewWeight", buff.toString());
		}
        
       // Handle weight change
        if(newWeight != _lastLoad){
           
        	_lastLoad = newWeight;
         
           boolean stateChaged = changeOverloadWithWaterMark();
           
           if (stateChaged) {
				if (_isOverloaded) {
					if (c_logger.isErrorEnabled()) {
						c_logger.error(PerfUtil.getOverloadedMsgByType(counter.getCounterID()),Situation.SITUATION_REPORT_STATUS, Long.valueOf(currentLoad));
					}
				} 
            	else  {
					if (c_logger.isInfoEnabled()) {
						c_logger.info("info.server.overload.cleared",
								Situation.SITUATION_REPORT_STATUS, Long.valueOf(currentLoad));
					}
					setThrowMsgInOverload(false);
				}
            	
            }
            return stateChaged;
        }
        return false;
    }
     
    /**
     * Check if the new load will change _isOverloaded flag.
     * @return true if the state was changed
     */
    private boolean changeOverloadWithWaterMark() {
        if (_isOverloaded == true && _lastLoad >= _weightOverloadMark) {
            _isOverloaded = false;
            return true;
        }
        if(_isOverloaded == false && _lastLoad == 0){
            _isOverloaded = true;
            return true;
        }

        return false;
    }

    /**
     * @return Returns the lastWeight.
     */
    public int getLastWeight() {
        return _lastLoad;
    }

    /**
     * Get the current weight that was set for container
     * @return
     */
    public int getCurrentWeight() {
        return _lastLoad;
    }

	/**
	 * Returns the value of the Low Watermark Size
	 * @return
	 */
    public int getLowWaterMarkSize() {
		return _lowWaterMarkSize;
	}

	/**
	 * Sets the _throwMessagedInOverload flag. When true container will 
	 * throw all incoming messages;
	 * @param throwMsg
	 */
    public void setThrowMsgInOverload(boolean throwMsg) {
		_throwMessagedInOverload = throwMsg;
	}
    
    /**
     * Returns the _throwMessagedInOverload flag.
     * @return
     */
    public boolean shouldThrowMsgs() {
		return _throwMessagedInOverload;
	}
}
