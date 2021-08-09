/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip.ar;

/**
 * That enum defines which application selection 
 * direction will be selected by aplication router.
 *   
 * @author Roman Mandeleil
 */
public enum SipApplicationRoutingDirective {
	
	/**
	 * continue the selection process.
	 */
	CONTINUE , 
	
	/**
	 * new selection process.
	 */
	NEW ,
	
	/**
	 * start the selection chain from the same application 
	 * in another region.
	 */
	REVERSE
}
