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
package com.ibm.jbatch.container.jsl;


import com.ibm.jbatch.container.jsl.impl.JobModelSerializerImpl;
import com.ibm.jbatch.container.jsl.impl.StepModelSerializerImpl;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;

public class ModelSerializerFactory {

	public static ModelSerializer<JSLJob> createJobModelSerializer() {
		return new JobModelSerializerImpl();
	}
	
	public static ModelSerializer<Step> createStepModelSerializer() {
		return new StepModelSerializerImpl();
	}

}
