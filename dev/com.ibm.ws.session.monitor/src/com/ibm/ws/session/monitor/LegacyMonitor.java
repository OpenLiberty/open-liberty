/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class LegacyMonitor extends StatisticActions {

    //For Legacy PMI
    //Define stats ID here.
    public static final int ACTIVE_SESSIONS = 6;
    public static final int LIVE_SESSIONS = 7;
    public static final int CREATE_SESSIONS = 1;
    public static final int INVALIDATED_SESSIONS = 2;
    public static final int INVALIDATED_SESSIONS_BYTIMEOUT = 16;
    //-----------------------
    
    private SPICountStatistic createCount;
    private SPIRangeStatistic activeCount;
    private SPIRangeStatistic liveCount;
    private SPICountStatistic invalidatedCount;
    private SPICountStatistic invalidatedCountbyTimeout;    
    private StatsInstance si;

    
    public LegacyMonitor(String appName, StatsGroup grp) {
		// TODO Auto-generated constructor stub
    	

    	if(grp!=null){
		try {

			si = StatsFactory.createStatsInstance(appName, grp, null, this);

		} catch (StatsFactoryException e) {
			// TODO Auto-generated catch block
			FFDCFilter.processException(e, getClass().getName(), "SessionMonitor");
		}
    	}
	}
    @Override
    public void statisticCreated(SPIStatistic s) {    	
        if (s.getId() == ACTIVE_SESSIONS) {
            activeCount = (SPIRangeStatistic) s;
        } else if (s.getId() == LIVE_SESSIONS) {
            liveCount = (SPIRangeStatistic) s;
        } else if (s.getId() == CREATE_SESSIONS) {
            createCount = (SPICountStatistic) s;
        } else if (s.getId() == INVALIDATED_SESSIONS) {
            invalidatedCount = (SPICountStatistic) s;
        } else if (s.getId() == INVALIDATED_SESSIONS_BYTIMEOUT) {        	
            invalidatedCountbyTimeout = (SPICountStatistic) s;
        }else {

        }
    }
    /** {@inheritDoc} */
    @Override
    public void updateStatisticOnRequest(int dataId) {
    	if(si!=null){    		
    		SessionStats temp =SessionMonitor.getSessionStatsOB(si.getName());
    		if (dataId == ACTIVE_SESSIONS) {        	
                activeCount.set(temp.getActiveCount());
            }
            if (dataId == LIVE_SESSIONS) {
                liveCount.set(temp.getLiveCount());
            }
            if (dataId == CREATE_SESSIONS) {
                createCount.set(temp.getCreateCount(),0,temp.getCreateCount());
            }
            if (dataId == INVALIDATED_SESSIONS) {            	
                invalidatedCount.set(temp.getInvalidatedCount(),0,temp.getInvalidatedCount());
            }
            if (dataId == INVALIDATED_SESSIONS_BYTIMEOUT) {     
                invalidatedCountbyTimeout.set(temp.getInvalidatedCountbyTimeout(),0,temp.getInvalidatedCountbyTimeout());
            }
    }
    }
    
}
