/**
 * 
 */
package mpGraphQL10.voidQuery;

/**
 *  Expected exception (404 Not Found) when the app fails to start
 */
@SuppressWarnings("serial")
public class Expected404Exception extends Exception {

    public Expected404Exception() {
        super();
    }

    /**
     * @param message
     */
    public Expected404Exception(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public Expected404Exception(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public Expected404Exception(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public Expected404Exception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
