/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

package com.ibm.ws.sib.mfp.jmf.impl;

import com.ibm.ws.sib.mfp.jmf.JMFList;

/**  This interface extends JMFList with the additional methods
 * common to both JSVaryingListImpl and JSCompatibleBoxList.   The box manager
 * and its ancillaries (JSBoxedListImpl and JSIndirectBoxedListImpl) need this
 * abstraction to function correctly in the presence or absence of a compatibility
 * layer.
 */
interface JSVaryingList extends JMFList {
  int getIndirection();
  JSField getElementType();
}
