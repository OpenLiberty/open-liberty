/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib.annotation;

import java.util.EventListener;

import javax.servlet.jsp.tagext.JspTag;

public class DefaultAnnotationHandler extends AnnotationHandler {
     public void doPostConstructAction (JspTag tag) {
     }
     
     public void doPostConstructAction (EventListener listener) {
     }
     
     public void doPreDestroyAction (JspTag tag) {
     }
}
