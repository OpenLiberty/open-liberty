/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.faulttolerance.exceptions;

/**
 * The exception should be thrown when the maximum retries is reached or overall retry duration is reached,
 * circuit breaker is open, timeout occurred. 
 * <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class FaultToleranceException extends RuntimeException {
    
    private static final long serialVersionUID = 958116453839967874L;
    
    public FaultToleranceException() {
        super();
    }
    
    public FaultToleranceException(Throwable t){
        super(t) ;
    }
    
    public FaultToleranceException(String message){
        super(message) ;
    }
    
    public FaultToleranceException(String message, Throwable t) {
        super (message, t);
    }


}
