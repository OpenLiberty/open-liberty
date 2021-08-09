/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.jsp.runtime;

import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.jsp.tagext.Tag;

import com.ibm.ws.jsp.runtime.UnsynchronizedStack;

public class TagHandlerPool extends ThreadLocal
{
	protected Object initialValue()
	{
		return new HashMap()
		{
			/**
			 * Comment for <code>serialVersionUID</code>
			 */
			private static final long serialVersionUID = 3545240228030787633L;

			protected void finalize() throws Throwable
			{
				Iterator i = values().iterator();
				while (i.hasNext())
				{
					UnsynchronizedStack stack = (UnsynchronizedStack) i.next();
					Iterator j = stack.iterator();
					while (j.hasNext())
					{
						Tag tag = (Tag) j.next();
						tag.release();
						tag = null;
					}
					stack.clear();
					stack = null;
				}
				clear();
				super.finalize();
			}
		};
	}
	public HashMap getPool()
	{
		return ((HashMap) get());
	}
}