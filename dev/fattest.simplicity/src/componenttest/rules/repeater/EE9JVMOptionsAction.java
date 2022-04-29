/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import componenttest.custom.junit.runner.RepeatTestFilter;

/**
 * Test repeat action that aside from all the logic done by JVMOptionsAction, it's also used
 * to enable EE9 transformation for test applications.
 */
public class EE9JVMOptionsAction extends JVMOptionsAction {

    public static final String ID = "JVM_OPTIONS_EE9";

    public EE9JVMOptionsAction() {
        super();
        withID(ID);
    }

    public EE9JVMOptionsAction(boolean useBeta) {
        super(true);
        withID(ID);
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

}
