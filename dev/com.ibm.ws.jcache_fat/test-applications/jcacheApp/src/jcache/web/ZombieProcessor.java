/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jcache.web;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

/**
 * Appends 'zombie' to a cache entry, if the entry does not contain 'zombie' *
 */
public class ZombieProcessor implements EntryProcessor<Integer, String, Boolean> {

	/**
	 * Checks for 'zombie' in a string and appends if it is not there.
	 * 
	 * Returns true if it appended 'zombie', otherwise false.
	 */
	@Override
	public Boolean process(MutableEntry<Integer, String> entry, Object... arrrrrg) throws EntryProcessorException {
		
		if(!entry.getValue().toLowerCase().contains("zombie")) {
			entry.setValue(entry.getValue()+"zombie");
			return true;
		}		
		return false;
	}
	
	

}
