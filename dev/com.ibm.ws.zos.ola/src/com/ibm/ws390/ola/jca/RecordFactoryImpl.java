/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

public class RecordFactoryImpl implements RecordFactory {

	/**
	 * Package protected constructor to prevent unauthorized creation
	 */
	RecordFactoryImpl()
	{
	}
	
	public IndexedRecord createIndexedRecord(String arg0)
			throws ResourceException 
	{
		return new com.ibm.websphere.ola.IndexedRecordImpl();
	}

	public MappedRecord createMappedRecord(String arg0)
			throws ResourceException 
	{
		return new com.ibm.websphere.ola.MappedRecordImpl();    /* @F014448C */
	}

}
