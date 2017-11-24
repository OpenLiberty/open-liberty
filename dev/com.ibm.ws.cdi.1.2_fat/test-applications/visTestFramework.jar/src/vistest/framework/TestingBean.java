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
package vistest.framework;


/**
 * This is a bean which tests which of the target beans it can see.
 * <p>
 * We can't call a bean directly from the test class so these beans are called from a servlet or application client main class
 */
public interface TestingBean {

    /**
     * Do the test
     * <p>
     * 
     * @return the test result which should be returned directly to the test class
     */
    public String doTest();

}
