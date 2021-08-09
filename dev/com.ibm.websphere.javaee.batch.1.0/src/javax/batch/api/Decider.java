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

package javax.batch.api;

import javax.batch.runtime.StepExecution;

/**
 * A Decider receives control as part of a decision element 
 * in a job. It is used to direct execution flow during job
 * processing. It returns an exit status that updates the 
 * current job execution's exit status. This exit status 
 * value also directs the execution transition based on 
 * next, end, stop, fail child elements configured on the 
 * same decision element as the decider.
 */
public interface Decider {
	/**
	 * The decide method sets a new exit status for a job. 
	 * It receives an array of StepExecution objects as input.
	 * These StepExecution objects represent the execution 
	 * element that transitions to this decider as follows:
     * <p>
     * <ul> 
     * <li>Step</li>
     * <p>
     * When the transition is from a step, the decide method
     * receives the StepExecution corresponding 
     * to the step as input.
     * <li>Split</li>
     * <p>
     * When the transition is from a split, the decide method
     * receives a StepExecution from each flow defined to the split
     * as input.   
     * <li>Flow</li>
     * <p>
     * When the transition is from a flow, the decide method
     * receives a StepExecution corresponding 
     * to the last execution element that completed in the flow. 
     * This will be a single StepExecution if the last element 
     * was a step and multiple StepExecutions if the last element 
     * was a split.
	 * </ul>
	 * @param executions specifies the StepExecution(s) of the preceding 
     * element.	 
     * @return updated job exit status
	 * @throws Exception is thrown if an error occurs. 
	 */
	public String decide(StepExecution[] executions) throws Exception;

}
