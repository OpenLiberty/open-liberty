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
package com.ibm.ws.sib.api.jms;

import javax.jms.JMSException;

/**
 * Provides an interface so that delete() of JmsTemporaryQueueImpl
 * and JmsTemporaryTopicImpl can be reached by common code
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 */
public interface JmsTemporaryDestinationInternal {

	public void delete() throws JMSException;

}
