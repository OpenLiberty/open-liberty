/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.beanval;

import javax.faces.validator.BeanValidator;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.annotation.SkipForRepeat;

@SuppressWarnings("serial")
@WebServlet("/BeanValTestServlet")
public class BeanValTestServlet extends FATServlet {

    @SkipForRepeat(SkipForRepeat.EE10_FEATURES)
    @Test
    public void testValidatorInServletContext(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object val = request.getServletContext().getAttribute(BeanValidator.VALIDATOR_FACTORY_KEY);
        Assert.assertNotNull(val);
    }

}
