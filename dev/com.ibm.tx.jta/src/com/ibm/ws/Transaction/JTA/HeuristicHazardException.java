package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.XAException;

public final class HeuristicHazardException extends XAException
{
    // This exception is only used internally by the Tx service and is not serialized
    private static final long serialVersionUID = 491157435531839611L; /* @274187A*/
}
