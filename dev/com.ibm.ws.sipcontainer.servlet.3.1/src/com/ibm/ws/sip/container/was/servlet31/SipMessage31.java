/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.servlet31;

import com.ibm.websphere.servlet31.request.IRequest31;
import com.ibm.websphere.servlet31.response.IResponse31;
import com.ibm.ws.sip.container.was.message.SipMessage;

/**
 * A version of SIP message that implements servlet31 request\response.
 * Only used to add the inheritance without any actual content.
 */
public class SipMessage31 extends SipMessage implements IRequest31 , IResponse31{

	public SipMessage31() {
		super();

	}

	@Override
	public void setContentLengthLong(long length) {
	
		
	}

	@Override
	public long getContentLengthLong() {
		
		return 0;
	}


}
