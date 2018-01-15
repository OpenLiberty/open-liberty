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
package jp.test.bean;

import java.io.PrintWriter;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class MyExecutor {
    @Inject
    private MyBeanHolder accessor;

    public void execute(PrintWriter out) {
        out.print(accessor.test1()); // public method
        out.print(":");
        out.println(accessor.test2()); // package private method
    }

}
