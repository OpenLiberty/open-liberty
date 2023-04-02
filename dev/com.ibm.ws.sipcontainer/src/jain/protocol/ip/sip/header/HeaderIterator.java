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

import java.util.NoSuchElementException;

/**
 * This interface provides an Iterator over a list of Headers. There are two differences
 * between HeaderIterator and java.util.Iterator:
 * <ol>
 * <li>HeaderIterator contains no remove() method</li>
 * <li>HeaderIterator's next() method can throw a HeaderParseException. This is because
 * the next header's value may not have been parsed until the next() method is invoked.
 * </li>
 * </ol>
 *
 * @version 1.0
 */
public interface HeaderIterator
{
    public boolean hasNext();
    public Header next() throws HeaderParseException,NoSuchElementException;
    public Header remove() throws NoSuchElementException;
}
