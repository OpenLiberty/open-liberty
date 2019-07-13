/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import com.ibm.ws.sip.container.annotation.thin.SipServletThinMergeAction;

/**
 * used in a full Websphere environment 
 * 
 * @author asafz
 *
 */
public class SipServletMergeAction extends SipServletThinMergeAction {
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.amm.merge.AbstractMergeAction#isClassTargetsSupported()
	 */
	public boolean isClassTargetsSupported () {
		return SipAnnotationUtil.supportAmm();
	}
}
