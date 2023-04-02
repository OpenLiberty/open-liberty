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

/**
 * @ibm-spi
 *
 * This class provides a SPI wrapper to the com.ibm.ws.sib.utils.ras.SibTr class
 * and is intended for internal infrastructure use only by components that
 * understand and operate with messaging engines.
 * <p>
 * Platform messaging code may use com.ibm.ws.sib.utils.ras.SibTr directly all
 * non-platform messaging code must use this SPI class.
 */

package com.ibm.wsspi.sib.utils.ras;

public final class SibTr extends com.ibm.ws.sib.utils.ras.SibTr {

}
