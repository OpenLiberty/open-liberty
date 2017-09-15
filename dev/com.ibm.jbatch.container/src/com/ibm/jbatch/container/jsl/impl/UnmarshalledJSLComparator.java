/**
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.container.jsl.impl;

import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Listener;
import com.ibm.jbatch.jsl.model.Listeners;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class UnmarshalledJSLComparator {

	public static boolean equals(JSLJob job1, JSLJob job2) {
		if(job1 == null && job2 == null) return true;
		if(job1 == null || job2 == null) return false;
		
		boolean stillEqual = true;
		//for each attribute, stillEqual && equal(that attribute)
		stillEqual = stillEqual &&
				equals(job1.getListeners(), job2.getListeners()) &&
				equals(job1.getProperties(), job2.getProperties()) &&
				true;
		return stillEqual;
	}
	
	/**
	 * Compare two <properties> elements and their contents. The properties need not
	 * be in the same order.
	 * @param jslprops1 first <properties> element, could be null
	 * @param jslprops2 second <properties> element, could be null
	 * @return whether JSLProperties contain the same property names/values
	 */
	public static boolean equals(JSLProperties jslprops1, JSLProperties jslprops2) {
		//TODO: test cases
		if(jslprops1 == null && jslprops2 == null) return true;
		if(jslprops1 == null || jslprops2 == null) return false;
		
		boolean stillEqual = true;
		for(Property prop1 : jslprops1.getPropertyList()) {
			boolean isListenerInOtherList = false;
			for(Property prop2 : jslprops2.getPropertyList()) {
				if(equals(prop1, prop2))
					isListenerInOtherList = true;
			}
			stillEqual = stillEqual && isListenerInOtherList;
		}
		return stillEqual;
	}
	
	/**
	 * Checks parameters for null before .equals() comparison between them. To be used
	 * on leaf nodes of JSLJob hierarchy, e.g.; Java Strings or other built-ins.
	 * @param o1
	 * @param o2
	 * @return true if o1 == o2 == null or if o1.equals(o2), and false otherwise.
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if(o1 == null && o2 == null) return true; // null == null
		if(o1 == null || o2 == null) return false; // null != non-null
		return o1.equals(o2); //evaluate
	}
	
	public static boolean equals(Property prop1, Property prop2) {
		if(prop1 == null && prop2 == null) return true;
		if(prop1 == null || prop2 == null) return false;
		
		return nullSafeEquals(prop1.getName(), prop2.getName()) &&
				nullSafeEquals(prop1.getValue(), prop2.getValue());
	}
	
	public static boolean equals(Listeners listeners1, Listeners listeners2) {
		if(listeners1 == null && listeners2 == null) return true;
		if(listeners1 == null || listeners2 == null) return false;
		
		boolean stillEqual = true;
		for(Listener listener1 : listeners1.getListenerList()) {
			boolean isListenerInOtherList = false;
			for(Listener listener2 : listeners2.getListenerList()) {
				if(equals(listener1, listener2))
					isListenerInOtherList = true;
			}
			stillEqual = stillEqual && isListenerInOtherList;
		}
		return stillEqual;
	}
	
	public static boolean equals(Listener listener1, Listener listener2) {
		if(listener1 == null && listener2 == null) return true;
		if(listener1 == null || listener2 == null) return false;
		
		boolean stillEqual = true;
		stillEqual = stillEqual &&
				nullSafeEquals(listener1.getRef(), listener2.getRef()) &&
				equals(listener1.getProperties(), listener2.getProperties());
		return stillEqual;
	}
	
	public static boolean equals(Step step1, Step step2) {
		if(step1 == null && step2 == null) return true;
		if(step1 == null || step2 == null) return false;

		//TODO: finish with remaining fields
		return equals(step1.getChunk(), step2.getChunk()) &&
				equals(step1.getListeners(), step2.getListeners()) &&
				equals(step1.getProperties(), step2.getProperties()) &&
				nullSafeEquals(step1.getAllowStartIfComplete(), step2.getAllowStartIfComplete()) &&
				nullSafeEquals(step1.getNextFromAttribute(), step2.getNextFromAttribute());
	}
	
	public static boolean equals(Chunk chunk1, Chunk chunk2) {
		if(chunk1 == null && chunk2 == null) return true;
		if(chunk1 == null || chunk2 == null) return false;
		
		return 	nullSafeEquals(chunk1.getCheckpointPolicy(), chunk2.getCheckpointPolicy()) &&
				nullSafeEquals(chunk1.getItemCount(), chunk2.getItemCount()) &&
				nullSafeEquals(chunk1.getTimeLimit(), chunk2.getTimeLimit()) &&
				nullSafeEquals(chunk1.getProcessor(), chunk2.getProcessor()) &&
				nullSafeEquals(chunk1.getReader(), chunk2.getReader()) &&
				nullSafeEquals(chunk1.getRetryLimit(), chunk2.getRetryLimit()) &&
				nullSafeEquals(chunk1.getSkipLimit(), chunk2.getSkipLimit()) &&
				nullSafeEquals(chunk1.getWriter(), chunk2.getWriter());
	}
	
	
}
