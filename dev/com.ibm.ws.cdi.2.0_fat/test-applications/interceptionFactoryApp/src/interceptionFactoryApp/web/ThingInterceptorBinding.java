/**
 *
 */
package interceptionFactoryApp.web;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

/**
 *
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Documented
@javax.interceptor.InterceptorBinding
public @interface ThingInterceptorBinding {

    public class BindingLiteral extends AnnotationLiteral<ThingInterceptorBinding> implements ThingInterceptorBinding {

        /**  */
        private static final long serialVersionUID = -6780837274999114942L;

        public static BindingLiteral INSTANCE = new BindingLiteral();
    }
}