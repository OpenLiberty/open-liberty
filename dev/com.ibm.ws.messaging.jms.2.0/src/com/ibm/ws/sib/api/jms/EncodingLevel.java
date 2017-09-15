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

/* ************************************************************************** */
/**
 * An EncodingLevel indicates how complete an encoding of a JmsDestination is required
 */
/* ************************************************************************** */
public enum EncodingLevel
{
  /** MINIMAL encoding includes the bare minimum of information (not even the name is included in this encoding, used for the JMSReplyTo originally from SIB) */
  MINIMAL,
  /** LIMITED encoding contains some of the information (used for JMSReplyTo's originally from MQ) */
  LIMITED,
  /** FULL encoding includes everything (used for JMSDestination's) */
  FULL
}
