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
package com.ibm.ws.sip.container.servlets;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.seqlog.LogEvent;
import com.ibm.sip.util.seqlog.SequenceLogger;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.parser.util.ObjectPool;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author Amir Perlman, Jun 3, 2005
 * 
 * Sequence logger for a SIP Session/Dialog.  
 */
public class SipSessionSeqLog extends SequenceLogger {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipSessionSeqLog.class);
    
    //
    //Normal states constants
    //
    public static final int INIT          = 				LogEvent.NORMAL | 1;
    public static final int SIPLET_DESC   =  				LogEvent.NORMAL | 2; 
    public static final int PROCESS_REQ   =  				LogEvent.NORMAL | 3;
    public static final int PROCESS_RESP  = 				LogEvent.NORMAL | 4;
    public static final int SENDING_RES   = 				LogEvent.NORMAL | 5;
    public static final int SENDING_REQ   = 				LogEvent.NORMAL | 6;
    public static final int IS_PROXY 	  =     			LogEvent.NORMAL | 7;
    public static final int IS_UAS 		  =     			LogEvent.NORMAL | 8;
    public static final int PROXY_BRANCH_CREATED = 			LogEvent.NORMAL | 9;
    public static final int PROXY_BRANCH_RESP 	 = 			LogEvent.NORMAL | 10;
    public static final int PROXY_SUBSEQUENT_REQ = 			LogEvent.NORMAL | 11;
    public static final int PROXY_SUBSEQUENT_RESP= 			LogEvent.NORMAL | 12;
    public static final int PROXY_IS_RR 		 = 			LogEvent.NORMAL | 13;
    public static final int PROXY_IS_PARALELL 	= 			LogEvent.NORMAL | 14;
    public static final int PROXY_IS_RECURSE 	= 			LogEvent.NORMAL | 15;
    public static final int PROXY_UPDATE_BEST_RESP =		LogEvent.NORMAL | 16;
    
    
    //
    //Warnings
    //
    public static final int PROCESS_TIMEOUT = LogEvent.WARNING | 1;
    public static final int PROXY_TRANSACTION_TIMED_OUT = 	LogEvent.WARNING | 2;
    
    
    //
    //Completed (expected end of life)
    //
    public static final int INVALIDATED = LogEvent.COMPLETED | 1;
    public static final int EXPIRED = LogEvent.COMPLETED | 2;
    public static final int REUSED = LogEvent.COMPLETED | 3;
    
    
    //
    //Errors
    //
    public static final int ERROR_SESSION_TERMINATED 	= LogEvent.ERROR | 1;
    public static final int ERROR_TRANSACTION_TERMINATED= LogEvent.ERROR | 2;
    public static final int ERROR_DISPATCH_EXCEPTION 	= LogEvent.ERROR | 3;;
    public static final int ERROR_SEND_REQ 				= LogEvent.ERROR | 4;;
    public static final int ERROR_SEND_RESP 			= LogEvent.ERROR | 5;;
    /**
     * Pool of reusable context loggers. 
     */
    private static ObjectPool c_pool = new ObjectPool(SipSessionSeqLog.class, null, -1);

    /**
     * Indicates if the logger is currently enabled by the config. 
     */ 
    private static boolean c_isEnabled;

    //
    //Static initializer
    // 
    static {
        PropertiesStore store = PropertiesStore.getInstance();
        String value =
            store.getProperties().getString(CoreProperties.SIP_SESSION_SEQ_LOG_LEVEL);

        if (value != null)
        {
            value = value.trim().toLowerCase();
            if(value.equals("warning"))
            {
                setThreshold(LogEvent.WARNING);
                c_isEnabled = true;
            }
            else if(value.equals("error"))
            {
                setThreshold(LogEvent.ERROR);
                c_isEnabled = true;
            }
            else if(value.equals("all"))
            {
                setThreshold(LogEvent.COMPLETED);
                c_isEnabled = true;
            }
            
            if (c_isEnabled && c_logger.isInfoEnabled()) {
                Object[] args = { value }; 
                c_logger.info("info.sequence.logger.enabled", "Config", args);
            }
        }
    }

    /**
     * @see com.ibm.sip.util.seqlog.SequenceLogger#dumpStateDesc(int, java.lang.StringBuffer)
     */
    public void dumpStateDesc(int state, StringBuffer buf) {
        switch(state)// TODO Auto-generated method stub
        {
        	case INIT:
        	    buf.append("Init");
        	    break;
        	case SIPLET_DESC:
        	    buf.append("Siplet Description");
        	    break;
        	case PROCESS_REQ:
        	    buf.append("Received Request");
        	    break;
        	case PROCESS_RESP:
        	    buf.append("Received Response");
        	    break;
        	case SENDING_REQ:
        	    buf.append("Sending Request");
        	    break;
        	case SENDING_RES:
        	    buf.append("Sending Response");
        	    break;
        	case PROCESS_TIMEOUT:
                buf.append("Process Timeout");
                break;
            case INVALIDATED:
                buf.append("Invalidated");
                break;
            case IS_UAS:
                buf.append("Is UAS");
                break;
            case IS_PROXY:
                buf.append("Is Proxy");
                break;
            case ERROR_SESSION_TERMINATED:
                buf.append("Session has terminated");
                break;
            case ERROR_TRANSACTION_TERMINATED:
                buf.append("Transaction has terminated");
                break;
            case ERROR_DISPATCH_EXCEPTION:
                buf.append("Exception in dispatch to siplet");
                break;
            case PROXY_BRANCH_CREATED:
                buf.append("Proxy branch created");
                break;
            case PROXY_BRANCH_RESP:
                buf.append("Proxy branch response");
                break;
            case PROXY_SUBSEQUENT_REQ:
                buf.append("Proxying subsequent request");
                break;
            case PROXY_SUBSEQUENT_RESP:
                buf.append("Proxy received response");
                break;
            case PROXY_UPDATE_BEST_RESP:
                buf.append("Proxy best response");
                break;
            case PROXY_IS_RECURSE:
                buf.append("Proxy is recurse");
                break;
            case PROXY_IS_PARALELL:
                buf.append("Proxy is parallel");
                break;
            case PROXY_IS_RR:
                buf.append("Proxy is record-route");
                break;
            case ERROR_SEND_REQ:
                buf.append("Error sending request");
                break;
            case ERROR_SEND_RESP:
                buf.append("Error sending response");
                break;                
            case PROXY_TRANSACTION_TIMED_OUT:
                buf.append("Failed to send response upstream");
                break;
            default :
                super.dumpStateDesc(state, buf);
            	break;
        }
    }
    
    
    /**
     * Get an instance of the logger from pool 
     * @return
     */
    public static SipSessionSeqLog getInstance()
    {
        if(!isEnabled())
        {
            return null;
        }
        return (SipSessionSeqLog) c_pool.get();
    }

    
    /**
     * Return this instance to the pool of available loggers.  
     */
    public void returnToPool() {
        clear();
        c_pool.putBack(this);
    }
    
    /**
     * Set the current threshold for dumping message to log.
     */
    public static void setThreshold(int threshold)
    {
        c_logThreshold = threshold;
    }
    
    /**
     * Get the Logger's description
     * @return
     */
    protected static String getDesc() {
        return  "SIP Session Log";
    }
    
    /**
     * Indicates if the logger is currentlly enabled. Enabling/Disabling is 
     * done by default by the config settings.
     */
    public static boolean isEnabled()
    {
        return c_isEnabled;
    }
}
