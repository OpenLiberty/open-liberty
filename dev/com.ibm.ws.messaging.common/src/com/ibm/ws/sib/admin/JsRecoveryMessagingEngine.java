/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

import com.ibm.ws.sib.utils.SIBUuid8;

public interface JsRecoveryMessagingEngine extends JsMessagingEngine {
	public void setUuid(SIBUuid8 meUUid);
	public void setMeName(String meName); 
	public void setBusName(String busName);
	public void setMessageStore(Object msgStore);
	public void setBus(JsEObject busConfigObject);
	public void setDataStoreExists(boolean dataStoreExists);
	public void setFileStoreExists(boolean fileStoreExists);
}
