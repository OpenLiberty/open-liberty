/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.user.prod.extensions;

/**
 * User product extension service interface.
 */
public interface UserProductExtension1 {

    /**
     * Say Hello.
     * 
     * @param input
     * @return
     */
    public String sayHello(String input);

    /**
     * Retrieves configured attribute 1.
     * 
     * @return A Long value.
     */
    public Long getAttribute1();

    /**
     * Retrieves configured attribute 1.
     * 
     * @return A String value.
     */
    public String getAttribute2();
}
