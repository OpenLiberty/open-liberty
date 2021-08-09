/*
 * 
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
 * This package has not been reviewed for correctness.  This was part of the RI's implementation
 * of the TCK SPI.  Then we switched to polling from the TCK without requiring any RI-specific code.
 * Still, polling, while nice from the TCK perspective, has its disadvantages for a real runtime where 
 * some other mechanism might be preferred.  Since we spent some time getting synchronization correct 
 * we decided to hold onto this code and keep it in the RI, possibly for future use.
 *
 * @version 1.0.16
 */
@org.osgi.annotation.versioning.Version("1.0.16")
package com.ibm.jbatch.container.callback;
