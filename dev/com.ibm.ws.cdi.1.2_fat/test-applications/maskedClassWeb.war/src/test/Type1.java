/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test;

import javax.enterprise.context.ApplicationScoped;

/**
 * This class will not be loaded because it is masked by test.Type1 in the EJB jar.
 * <p>
 * The naming of these classes is important so that when CDI tries to validate this BDA, it will look at test.Type1 first.
 */
@ApplicationScoped
public class Type1 {

    public String getMessage() {
        return "This is Type1 in the war";
    }
}
