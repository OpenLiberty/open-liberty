/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader.filters;

import java.util.logging.Level;

import com.ibm.websphere.logging.hpel.reader.LogRecordHeaderFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecordHeader;

/**
 * Implementation of the {@link LogRecordHeaderFilter} interface for filtering out
 * records not falling into a specified Level range.
 * 
 * @ibm-api
 */
public class LevelFilter implements LogRecordHeaderFilter {
	private final int minLevel;
	private final int maxLevel;
	
	/**
	 * Creates a filter instance using integer values as the level range.
	 * 
	 * These level integers are as defined in the java.util.logging.Level class.
	 * 
	 * @param minLevel lower boundary of the level range.
	 * @param maxLevel upper boundary of the level range.
	 * @see java.util.logging.Level
	 */
	public LevelFilter(int minLevel, int maxLevel){
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
	}

	/**
	 * Creates a filter instance with a specified Level range.
	 * 
	 * @param minLevel lower boundary of the level range. Value <code>null</code> means that lower boundary won't be checked.
	 * @param maxLevel upper boundary of the level range. Value <code>null</code> means that upper boundary won't be checked.
	 */
	public LevelFilter(Level minLevel, Level maxLevel){
		this.minLevel = minLevel==null ? Level.ALL.intValue() : minLevel.intValue();
		this.maxLevel = maxLevel==null ? Level.OFF.intValue() : maxLevel.intValue();
	}

	public boolean accept(RepositoryLogRecordHeader record) {	
		Level recordLevel = record.getLevel();
	
		if(recordLevel.intValue()>= minLevel && recordLevel.intValue() <= maxLevel){
			return true;
		}
		
		return false;
	}

}
