/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

/**
 * 
 * @author anat Common class which contians all common informaition used by
 *         PerformanceMgr for Server Weight counting and statistics updates.
 */
public class PerfUtil {
 
    //
    // constants that defined applications that influence on the server
    // weight
    //
    public final static int APP_WEIGHT_INT = 0;

    public final static int MPAP_WEIGHT_INT = 1;

    public final static int RESPONSE_WEIGHT_INT = 2;
    
    public final static int MSG_QUEUE_SIZE_INT = 3;
    
    final static int BEAN_ENABLE_INT = 999;
    
    final static int BEAN_DISABLE_INT = 1000;
    
    final static int INITIAL_INT = -1;
    
    final static String APP_WEIGHT_STR = "Max Application Weight";

    final static String TRAFFIC_WEIGHT_STR = "Max Traffic Weight";
    
    final static String RESPOSE_WEIGHT_STR = "Max Response Weight";
    
    final static String MSG_QUEUE_SIZE_STR = "Max Message Queue Weight";
    
    final static String BEAN_WEIGHT_STR = "Bean Weight";
    
    final static String BEAN_DISABLED_STR = "Bean Weight Disabled";
    
    final static String INITIAL_STR = "Initial";
    
    public final static int INITIAL_WEIGHT = 2;
    
    /**
     * Array size that represent the count of the parameters that 
     * influence on the server weight 
     */
    final static int WEIGHT_ARRAY_SIZE = 4;
    
    
    /**
     * Returns Overloaded message according to the type
     * @param type
     * @return
     */
    static String getOverloadedMsgByType(int type){
    	String typeStr;
		switch (type) {
		case APP_WEIGHT_INT:
			typeStr = "warn.server.overloaded.too.many.sessions";
			break;
		case MPAP_WEIGHT_INT:
			typeStr = "warn.server.overloaded.too.many.messages";
			break;
		case RESPONSE_WEIGHT_INT:
			typeStr = "warn.server.overloaded.slow.response";
			break;
		case MSG_QUEUE_SIZE_INT:
			typeStr = "warn.server.overloaded.message.queue.overloaded";
			break;
		default:
			typeStr = "warn.server.overloaded";
			break;
		}
		return typeStr;
	}
    
    /**
	 * Helper method which converts the int type to string
	 * 
	 * @param type
	 * @return
	 */
    public static String getTypeStr(int type){
    	
    	String typeStr;
        switch (type) {
        case INITIAL_INT:
            typeStr = INITIAL_STR;
            break;
            
        case APP_WEIGHT_INT:
            typeStr = APP_WEIGHT_STR;
            break;

        case MPAP_WEIGHT_INT:
            typeStr = TRAFFIC_WEIGHT_STR;
            break;

        case RESPONSE_WEIGHT_INT:
            typeStr = RESPOSE_WEIGHT_STR;
            break;
            
        case MSG_QUEUE_SIZE_INT:
            typeStr = MSG_QUEUE_SIZE_STR;
            break;

        case BEAN_ENABLE_INT:
            typeStr = BEAN_WEIGHT_STR; // 999 indicating JMX was enabled
            break;

        case BEAN_DISABLE_INT:
            typeStr = BEAN_DISABLED_STR; // 1000 indicating JMX was disabled
            break;

        default:
            typeStr = "Unknown";
            break;
        }
        return typeStr;
    }

}
