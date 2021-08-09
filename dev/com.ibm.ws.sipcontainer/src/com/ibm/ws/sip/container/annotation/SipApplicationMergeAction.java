/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import com.ibm.ws.sip.container.annotation.thin.SipApplicationThinMergeAction;

/**
 * used in a full Websphere environment 
 * 
 * @author asafz
 *
 */
public class SipApplicationMergeAction extends SipApplicationThinMergeAction {
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#isPackageTargetsSupported()
	 */
	public boolean isPackageTargetsSupported() {
		return SipAnnotationUtil.supportAmm();
	}
}
