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
package javax.batch.operations;

/**
 * JobRestartException is thrown when an error occurs during the JobOperator
 * restart operation.
 */

public class JobRestartException extends BatchRuntimeException {
	public JobRestartException() {
	}

	public JobRestartException(String message) {
		super(message);
	}

	public JobRestartException(Throwable cause) {
		super(cause);
	}

	public JobRestartException(String message, Throwable cause) {
		super(message, cause);
	}
	
	private static final long serialVersionUID = 1L;

}
