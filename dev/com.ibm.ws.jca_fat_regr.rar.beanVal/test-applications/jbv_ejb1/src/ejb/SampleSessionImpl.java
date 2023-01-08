/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

@Stateless
public class SampleSessionImpl implements SampleSessionRemote, SampleSessionLocal {
    @Override
    public String greeting(String name) {
        return "Hello " + name;
    }

    @Resource
    ValidatorFactory validatorFactory;

    @Resource
    Validator validator;

    @NotNull
    public String name; // Time permitting, add this in the ejbmodule validation constraint descriptor    

    @NotNull
    public String getName() {
        return this.name;
    }

    public Boolean validGreeting(String name) {
        this.name = name;
        Set<ConstraintViolation<SampleSessionImpl>> cvSet = null;
        try {
            cvSet = validator.validate(this);
        } catch (Throwable t) {
            System.out.println("An unexpected exception occurred during bean validation: " + t);
            t.printStackTrace();
            return false;

        }
        if (cvSet != null && cvSet.size() == 0) {
            return true;
        } else {
            System.out.println(formatConstraintViolations(cvSet));
            return false;
        }
    }

    String formatConstraintViolations(Set<ConstraintViolation<SampleSessionImpl>> cvSet) {
        StringBuffer sb = new StringBuffer();
        for (ConstraintViolation<SampleSessionImpl> cv : cvSet)
            sb.append("\n\t" + cv);
        return sb.toString();
    }
}
