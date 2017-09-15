/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

/**
 * This interface is implemented by the JFap channel framework channels so that access to their 
 * meta data can be exploited in the common part of the JFap channel.
 * 
 * @author Gareth Matthews
 */
public interface MetaDataProvider
{
   /**
    * @return Returns ConversationMetaData from this provider.
    */
   ConversationMetaData getMetaData();
}
