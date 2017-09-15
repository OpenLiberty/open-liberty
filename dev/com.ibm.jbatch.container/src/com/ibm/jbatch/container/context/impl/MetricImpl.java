/*
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
package com.ibm.jbatch.container.context.impl;

import javax.batch.runtime.Metric;

public class MetricImpl implements Metric {
	
	private MetricType name;
	
	private long value;
	
	public MetricImpl(MetricType name, long value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public MetricType getType() {
		return name;
	}

	@Override
	public long getValue() {
		return this.value;
	}
	
	public void incValue() {
		++this.value;
	}
	
	public void incValueBy(long incValue) {
		this.value = this.value + incValue;
	}
	
	@Override
	public String toString() {
		return "(" + name.toString() + "," + value + ")";
	}
}
