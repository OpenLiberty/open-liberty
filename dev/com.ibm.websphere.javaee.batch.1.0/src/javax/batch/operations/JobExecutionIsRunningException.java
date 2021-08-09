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

/**
 * JobExecutionIsRunningException is thrown when {@link JobOperator#abandon(long)} 
 * is called on a job execution which is currently running (i.e. has a 
 * {@link BatchStatus} of STARTING or STARTED).
 */
public class JobExecutionIsRunningException extends BatchRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JobExecutionIsRunningException() {
		// TODO Auto-generated constructor stub
	}

	public JobExecutionIsRunningException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public JobExecutionIsRunningException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public JobExecutionIsRunningException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
