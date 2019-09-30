/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.jdbc;

import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;
import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;
import com.ibm.wsspi.requestContext.ContextInfoArray;

public class JdbcTOBridgePSExecuteQuery implements RequestProbeTransformDescriptor {

    private static final String classToInstrument = "com/ibm/ws/rsadapter/jdbc/WSJdbcPreparedStatement";
    private static final String methodToInstrument = "executeQuery";
    private static final String descOfMethod = "all";
    private static final String requestProbeType = "websphere.datasource.psExecuteQuery";
    
    @Override
    public String getClassName() {
        return classToInstrument;
    }

    @Override
    public String getMethodName() {
        return methodToInstrument;
    }

    @Override
    public String getEventType() {
        return requestProbeType;
    }
    
    @Override
    public String getMethodDesc() {
        return descOfMethod;
    };
    
    @Override
	public boolean isCounter() {
		return false;
	}

    @Override
    public Object getContextInfo(final Object instanceOfThisClass, final Object methodArgs) {
    	return new ContextInfoArray() {
    		private String[] array = null;
    		
    		@Override
    		public String[] getContextInfoArray() {
    			if (array == null) {
    		    	String jndiName = "";
    		        String sql = "";
    		        if (instanceOfThisClass != null){
    		            //Do something here with "this" object and get the right meta data String and return
    		            jndiName = com.ibm.ws.jdbc.timedoperations.TimedOpsAccessor.getDataSourceIdentifier(instanceOfThisClass);
                        sql = com.ibm.ws.jdbc.timedoperations.TimedOpsAccessor.getSql(instanceOfThisClass);
    		        }

    		        array = new String[] {jndiName, sql};
    			}
    			
    			return array;
    		}
    		
    		@Override
    		public String toString() {
    			String[] array = getContextInfoArray();
    			String jndiName = array[0];
    			String sql = array[1];
    	        return jndiName + RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR + sql;
    		}
    	};
    }



    
   
 

}

