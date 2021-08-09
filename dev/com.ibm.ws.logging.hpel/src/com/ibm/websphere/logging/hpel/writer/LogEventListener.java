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
package com.ibm.websphere.logging.hpel.writer;

import java.util.Date;

/**
 * Interface for event listener including eNums on types.  The listener will implement the method and be called any time a log
 * event occurs. 
 *
 * @ibm-api
 */
public interface LogEventListener {
	public static final String EVENTTYPEROLL = "WLNEventRoll" ;
	public static final String EVENTTYPEDELETE = "WLNEventDelete" ;
	public static final String REPOSITORYTYPELOG = "WLNLog" ;
	public static final String REPOSITORYTYPETRACE = "WLNTrace" ;
	public void onLogFileAction(String eventType, String repositoryType, Date dateOldestLogRecord) ;
}