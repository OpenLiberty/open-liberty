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
package com.ibm.jbatch.container.artifact.proxy;

import javax.batch.api.chunk.ItemProcessor;

public class ItemProcessorProxy  extends AbstractProxy<ItemProcessor> implements ItemProcessor {

	ItemProcessorProxy(ItemProcessor delegate) { 
		super(delegate);
	}

	/*
	 * In order to provide skip/retry logic, these exceptions
	 * are thrown as-is rather than beeing wrapped.
	 * @see javax.batch.api.ItemReader#readItem()
	 */
	@Override
	public Object processItem(Object item) throws Exception {
		return this.delegate.processItem(item);
	}

}
