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
package defaultbeanvalidationcdi.beans;

import javax.enterprise.context.ApplicationScoped;

import defaultbeanvalidationcdi.validation.TestAnnotation;

/**
 * Simple test CDI managed bean that can be injected into other CDI managed
 * beans.
 */
@ApplicationScoped
public class TestBean {

    @TestAnnotation
    String testAnnotation1 = "testAnnotation";

}
