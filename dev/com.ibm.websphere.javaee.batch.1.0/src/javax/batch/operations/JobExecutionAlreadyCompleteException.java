/*
 * Copyright 2013 International Business Machines Corp.
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
package javax.batch.operations;

import javax.batch.runtime.BatchStatus;

/**
 * JobExecutionAlreadyCompleteException is thrown when restart is called for an already-completed job instance.
 * I.e. when {@link JobOperator#restart(long, java.util.Properties)} is called, and the most recent 
 * job execution for the corresponding job instance has ended with {@link BatchStatus} of COMPLETED.
 */
public class JobExecutionAlreadyCompleteException extends BatchRuntimeException {

	public JobExecutionAlreadyCompleteException() {
		// TODO Auto-generated constructor stub
	}

	public JobExecutionAlreadyCompleteException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public JobExecutionAlreadyCompleteException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public JobExecutionAlreadyCompleteException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;

}
