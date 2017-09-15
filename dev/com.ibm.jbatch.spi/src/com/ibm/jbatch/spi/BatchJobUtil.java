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
package com.ibm.jbatch.spi;

/**
 *
 * Implemented by the jbatch 352 RI to allow the host environment to
 * instruct the RI to purge job repository entries associated with 
 * a given "owner".
 * 
 * @author skurz
 */
public interface BatchJobUtil {

    /**
     * This method will purge all JobExecution, JobInstance, and job
     * data "owned" by a given "tag".   Job purge only happens, however,
     * if there are no other JobInstance(s) "owned" by other "tag"(s).
     * 
     * It does not guarantee a consistent view of the job repository,
     * so this method should only be issued when no jobs are being executed
     * "owned by" this tag.   If this type of external synchronization is not
     * used, the behavior is undefined.
     * 
     * @param tag A "tag" (or "app name", generically speaking).
     */ 
    public void purgeOwnedRepositoryData(String tag);
}
