/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

/**
 * A unique, checked exception for use with try-with-resources.
 * 
 * Throw this exception at the end of a try-with-resources block,
 * and catch it in the first catch block.
 *
 * This will gather all the exceptions that are thrown during closing as 
 * suppressed exceptions on this exception.
 *
 * If some other exception occurs first, this exception will never be thrown, 
 * and therefore will never be caught.
 *
 * The earlier exception will not match the catch block for this exception 
 * and will therefore be propagated or dealt with by subsequent catch blocks.
 */
public class ClosingException extends Exception {
    private static final long serialVersionUID = 1L;
}
