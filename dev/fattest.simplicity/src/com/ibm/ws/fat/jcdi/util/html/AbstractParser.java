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
package com.ibm.ws.fat.jcdi.util.html;

import java.nio.CharBuffer;
import java.util.List;

/**
 * This interface represent an abstract parser.
 * 
 * @author yingwang
 *
 */
public interface AbstractParser {

	/**
	 * reset the event handler.
	 * 
	 * @param handler
	 */
	public void reset(Object handler);
	
	/**
	 * Parse and rewrite an array of input CharBuffer and output the result into a list of
	 * CharBuffer.
	 * 
	 * @param inputBuffers input buffers.
	 * @param outputBuffers output buffers.
	 * @throws Exception the exception.
	 */
	public void parse(CharBuffer inputBuffers[], List<CharBuffer> outputBuffers) throws Exception ;
	
}
