/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Generated from C:\SIB\Code\WASX.SIB\dd\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\JsHdrSchema.schema: do not edit directly
package com.ibm.ws.sib.mfp.schema;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
public final class JsHdrAccess {
  public final static JSchema schema = new JsHdrSchema();
  public final static int DISCRIMINATOR = 0;
  public final static int ARRIVALTIMESTAMP = 1;
  public final static int SYSTEMMESSAGESOURCEUUID = 2;
  public final static int SYSTEMMESSAGEVALUE = 3;
  public final static int SECURITYUSERID = 4;
  public final static int SECURITYSENTBYSYSTEM = 5;
  public final static int MESSAGETYPE = 6;
  public final static int SUBTYPE = 7;
  public final static int HDR2 = 8;
  public final static int API = 10;
  public final static int IS_API_EMPTY = 0;
  public final static int IS_API_DATA = 1;
  public final static int API_DATA = 9;
}
