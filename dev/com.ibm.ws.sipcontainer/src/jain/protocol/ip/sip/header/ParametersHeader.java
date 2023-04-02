/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.Parameters;

/**
 * <p>
 * This interface represents any header that contains parameters. It is
 * a super-interface of ContactHeader, ContentTypeHeader, SecurityHeader and ViaHeader.
 * </p>
 *
 * @see Parameters
 * @see ContactHeader
 * @see ContentTypeHeader
 * @see SecurityHeader
 * @see ViaHeader
 *
 * @version 1.0
 *
 */
public interface ParametersHeader extends Header, Parameters
{
    
}
