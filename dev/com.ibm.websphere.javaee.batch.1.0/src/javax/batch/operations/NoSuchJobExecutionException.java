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
 * Used in JobOperator methods when referencing a job execution value
 * which does not correspond to a job execution recognized by the 
 * implementation's repository.
 */
public class NoSuchJobExecutionException extends BatchRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoSuchJobExecutionException() {
		// TODO Auto-generated constructor stub
	}

	public NoSuchJobExecutionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoSuchJobExecutionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public NoSuchJobExecutionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
