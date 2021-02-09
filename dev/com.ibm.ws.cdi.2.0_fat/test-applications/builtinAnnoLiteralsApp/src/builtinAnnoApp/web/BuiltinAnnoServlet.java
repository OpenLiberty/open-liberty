/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package builtinAnnoApp.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/builtin")
public class BuiltinAnnoServlet extends FATServlet {

    @Inject
    private Instance<Cake> builtinAnnoInstance;

    @Inject
    @Exotic
    private Instance<Cake> exoticInstance;

    @Inject
    private Instance<Lemondrizzle> lemonDrizzleInstance;

    private static final long serialVersionUID = 8549700799591343964L;

    /**
     * Test the javax.enterprise.inject.literal.NamedLiteral introduced in CDI2.0.
     *
     * Use the javax.enterprise.inject.Instance.select() method to choose the appropriately named bean from a set of beans.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testNamedLiteral() throws Exception {
        Cake builtinAnnoBean = null;

        builtinAnnoBean = builtinAnnoInstance.select(NamedLiteral.of("BATTENBERG")).get();

        System.out.println(builtinAnnoBean.greeting());
        Bean<?> theBean = builtinAnnoBean.getCakeBean();

        String beanName = theBean.getBeanClass().getSimpleName();
        assertTrue("Unexpected Battenberg bean retrieved - " + beanName, beanName.equals("Battenberg"));

        builtinAnnoBean = builtinAnnoInstance.select(NamedLiteral.of("CHEESECAKE")).get();

        System.out.println(builtinAnnoBean.greeting());
        theBean = builtinAnnoBean.getCakeBean();
        beanName = theBean.getBeanClass().getSimpleName();
        assertTrue("Unexpected Cheesecake bean retrieved - " + beanName, beanName.equals("Cheesecake"));
    }

    /**
     * Test the javax.enterprise.inject.Any.Literal introduced in CDI2.0.
     *
     * Use the javax.enterprise.inject.Instance.select() method to iterate over the set of beans and check that
     * we found a couple of representative beans.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testAnyLiteral() throws Exception {
        boolean foundVictoriaSponge = false;
        boolean foundBattenberg = false;
        Bean<?> theBean = null;
        String beanName = null;

        Any anyLiteral = new Any.Literal();
        Iterator<Cake> beanIterator = builtinAnnoInstance.select(anyLiteral).iterator();
        while (beanIterator.hasNext()) {
            Cake bean = beanIterator.next();
            theBean = bean.getCakeBean();
            beanName = theBean.getBeanClass().getSimpleName();
            if (beanName.equals("Battenberg"))
                foundBattenberg = true;
            if (beanName.equals("VictoriaSponge"))
                foundVictoriaSponge = true;
            System.out.println("Got bean: " + beanName + " " + bean.greeting());
        }
        assertTrue("Failed to find VictoriaSponge bean", foundVictoriaSponge);
        assertTrue("Failed to find Battenberg bean", foundBattenberg);
    }

    /**
     * Test the javax.enterprise.context.RequestScoped.Literal static nested class introduced in CDI2.0.
     *
     * The testing mechanism involves the use of a CDI Extension in the CakeExtension class which observes the
     * VictoriaSponge class through a ProcessAnnotatedType event. When the event fires, as Weld processes the VictoriaSponge bean,
     * the CakeExtension programmatically adds a @RequestScoped annotation to the VictoriaSponge bean using a
     * RequestScoped.Literal. We check that VictoriaSponge is RequestScoped.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testRequestScopeLiteral() throws Exception {
        Cake builtinAnnoBean = null;
        builtinAnnoBean = builtinAnnoInstance.select(NamedLiteral.of("VICTORIA")).get();
        System.out.println("Retrieving a bean with named qualifier VICTORIA, Retrieved - " + builtinAnnoBean);
        System.out.println(builtinAnnoBean.greeting());
        Bean<?> theBean = builtinAnnoBean.getCakeBean();

        String theScope = theBean.getScope().getSimpleName();
        assertTrue("Unexpected VictoriaSponge bean scope - " + theScope, theScope.equals("RequestScoped"));

        System.out.println("The scope of the VictoriaSponge Bean is - " + theScope);
    }

    /**
     * Test the javax.enterprise.inject.literal.InjectLiteral class introduced in CDI2.0.
     *
     * The testing mechanism involves the use of a CDI Extension in the CakeExtension class which observes the
     * Cake class through a ProcessAnnotatedType event. When the event fires, as Weld processes the Cake bean,
     * the CakeExtension programmatically adds a @Inject annotation to the Cake bean using an InjectLiteral.
     * We check that CakeIngredients has been injected and is non-null.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testInjectLiteral() throws Exception {
        Cake builtinAnnoBean = null;
        builtinAnnoBean = builtinAnnoInstance.select(NamedLiteral.of("VICTORIA")).get();
        System.out.println("Retrieving a bean with named qualifier VICTORIA, Retrieved - " + builtinAnnoBean);
        System.out.println(builtinAnnoBean.greeting());
        CakeIngredients theIngredients = builtinAnnoBean.getIngredients();
        assertNotNull(theIngredients);
    }

    /**
     * Test the javax.enterprise.util.Nonbinding.Literal class introduced in CDI2.0.
     *
     * The testing mechanism involves the use of a CDI Extension in the CakeExtension class which observes the
     * Exotic qualifier interface through a BeforeBeanDiscovery event. When the event fires, as Weld processes the Exotic interface,
     * the CakeExtension programmatically adds a @Nonbinding annotation to the value() method using a Nonbinding Literal.
     *
     * We check that a Rumbaba Cake is found and that a Weld UnsatisfiedResolutionException was not thrown.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testNonbindingLiteral() throws Exception {
        Cake builtinAnnoBean = null;

        try {
            builtinAnnoBean = exoticInstance.select(NamedLiteral.of("RUMBABA")).get(); // was annotation
            System.out.println("Retrieved an exotic cake bean - " + builtinAnnoBean);
        } catch (Exception ure) {
            // If the @NonBinding annotation was not applied then the Rumbaba cake's "Exotic" value will not
            // match the expected default Exotic qualifier value
            fail("Caught unexpected exception: " + ure);
        }
    }

    /**
     * Test the javax.enterprise.inject.Typed.Literal class introduced in CDI2.0.
     *
     * The testing mechanism involves the use of a CDI Extension in the CakeExtension class which observes the
     * Lemondrizzle class through a ProcessAnnotatedType event. When the event fires, as Weld processes the Lemondrizzle bean,
     * the CakeExtension programmatically adds a @Typed(Cake.class) annotation to the Lemondrizzle bean using a
     * Typed.Literal. We check that the Lemondrizzle can (thanks to this annotation) NOT be found by searching for a Lemondrizzle
     * bean but CAN be found by a generic Cake search.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testTypedLiteral() throws Exception {
        boolean foundLemondrizzle = false;

        Bean<?> theBean = null;
        String beanName = null;

        Any anyLiteral = new Any.Literal();
        Iterator<Lemondrizzle> lemonIterator = lemonDrizzleInstance.select(anyLiteral).iterator();
        while (lemonIterator.hasNext()) {
            Cake bean = lemonIterator.next();
            theBean = bean.getCakeBean();
            beanName = theBean.getBeanClass().getSimpleName();
            if (beanName.equals("Lemondrizzle"))
                foundLemondrizzle = true;

            System.out.println("Looking for a specific lemondrizzle cake, got bean: " + beanName + " " + bean.greeting());
            assertFalse("Should not have found a Lemondrizzle bean with a specific search", foundLemondrizzle);
        }

        Iterator<Cake> cakeIterator = builtinAnnoInstance.select(anyLiteral).iterator();
        while (cakeIterator.hasNext()) {
            Cake bean = cakeIterator.next();
            theBean = bean.getCakeBean();
            beanName = theBean.getBeanClass().getSimpleName();
            if (beanName.equals("Lemondrizzle"))
                foundLemondrizzle = true;

            System.out.println("Looking for any old cake, got bean: " + beanName + " " + bean.greeting());
        }

        assertTrue("Failed to find Lemondrizzle bean", foundLemondrizzle);
    }
}
