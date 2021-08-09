
/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.mxbean;

import java.beans.ConstructorProperties;


public class MessagingSubscription {

  /*
   * ===========================================================================
   *
   * ATTENTION --- THIS CLASS IS SERIALIZABLE.
   *
   * You should take care when modifying this class since doing so could break
   * serialization. Please analyze how your changes will affect serialization,
   * and consider overriding the inherited serialization methods if necessary.
   *
   * Thank you.
   *
   * ===========================================================================
   */

  private static final long serialVersionUID = -9047402482550648638L;

  private long _depth = 0;
  private String _id = null;
  private int _maxMsgs = 1000;
  private String _name = null;
  private String _selector = null;
  private String _subscriberId = null;
  private String[] _topics = null;

  @ConstructorProperties({ "name", "id", "depth", "subscriberId" })
  public MessagingSubscription(long depth, String id, int maxMsgs, String name, String selector, String subscriberId, String[] topics) {
    this._depth = depth;
    this._id = id;
    this._maxMsgs = maxMsgs;
    this._name = name;
    this._selector = selector;
    this._subscriberId = subscriberId;
    this._topics = topics;
  }

  public long getDepth() {
    return _depth;
  }

  public String getId() {
    return _id;
  }

  public String getName() {
    return _name;
  }

  /**
   * @deprecated
   * @return
   */
  public String getIdentifier() {
    return _name;
  }

  public String getSelector() {
    return _selector;
  }

  public String getSubscriberId() {
    return _subscriberId;
  }

  public String[] getTopics() {
    return _topics;
  }
  public String toString(){
	  String details="SIBSubscription name= "+_name+": SIBSubscription id= "+_id+": Subscription depth= "+_depth+": Subscription subscriber Id="+_subscriberId;
	  return details;
  }

}
