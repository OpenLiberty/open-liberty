/**
 *
 */
package builtinAnnoApp.web;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.InjectLiteral;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 *
 */
public class CakeExtension implements Extension {

    void observesCake(@Observes ProcessAnnotatedType<VictoriaSponge> event) {
        System.out.println("CAKEOBSERVER: event - " + event);

        AnnotatedTypeConfigurator<VictoriaSponge> annotatedTypeConfigurator = event.configureAnnotatedType();

        // add @RequestScoped to Cake and add @Inject to the CakeIngredients field
        annotatedTypeConfigurator.add(RequestScoped.Literal.INSTANCE)
                        .filterFields(af -> (af.getJavaMember()
                                        .getName()
                                        .equals("ingredients")))
                        .findFirst()
                        .get()
                        .add(InjectLiteral.INSTANCE);

    }

    void observesExotic(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        System.out.println("EXOTICOBSERVER: bbd - " + beforeBeanDiscovery);

        AnnotatedTypeConfigurator<Exotic> annotatedTypeConfigurator = beforeBeanDiscovery.configureQualifier(Exotic.class);

        // add @Nonbinding to the value() method
        annotatedTypeConfigurator.filterMethods(af -> (af.getJavaMember()
                        .getName()
                        .equals("value")))
                        .findFirst()
                        .get()
                        .add(javax.enterprise.util.Nonbinding.Literal.INSTANCE);
    }

    void observesLemon(@Observes ProcessAnnotatedType<Lemondrizzle> event) {
        System.out.println("LEMONOBSERVER: event - " + event);

        AnnotatedTypeConfigurator<Lemondrizzle> annotatedTypeConfigurator = event.configureAnnotatedType();

        // Add @Typed(Cake.class) to Lemondrizzle Cake
        Class<?>[] theCakeType = { Cake.class };
        annotatedTypeConfigurator.add(javax.enterprise.inject.Typed.Literal.of(theCakeType));

    }
}
