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
package com.ibm.ws.sib.processor.utils;

import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.matching.TopicAuthorization;
import com.ibm.ws.util.ObjectPool;

/**
 * The Search Results object pool is responsible for creating
 * a MessageProcessorSearchResults when one can't be 
 * obtained from the object pool
 */
public final class SearchResultsObjectPool extends ObjectPool
{

  /** Support for discriminator access control */
  private TopicAuthorization topicAuthorization;

  /**
   * @param name  The name of the object pool
   * @param size  The size of the object pool
   */
  public SearchResultsObjectPool(String name, int size)
  {
    super(name, size);
  }
  
  protected Object createObject()
  {
    return new MessageProcessorSearchResults(topicAuthorization);
  }

  /**
   * @param authorization
   */
  public void setTopicAuthorization(TopicAuthorization authorization) 
  {
	  topicAuthorization = authorization;
  }

}
