/**
 *
 */
package interceptionFactoryApp.web;

/**
 *
 */
public class Intercepted {

    static boolean intercepted = false;

    static void set() {
        intercepted = true;
    }

    static boolean get() {
        return intercepted;
    }
}
