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
/**
 * The interfaces in this package reflect the fine-grained modularity of the
 * batch runtime.   Though there is probably enough tight-coupling that 
 * a third-party implementation of one of these "services" might not work 
 * out of the box, there is at least enough of a well-defined interface here
 * that we can point to this package as the starting point for extending/modifying
 * the batch runtime implementation with different behaviors with perhaps different
 * qualities of service.
 * 
 * @version 1.0
 */
@org.osgi.annotation.versioning.Version("1.0")
package com.ibm.jbatch.spi.services;