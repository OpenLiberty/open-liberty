package batch.fat.common;

public class ForceRollbackException extends ForceRetryableException {

    /**  */
    private static final long serialVersionUID = 1L;

    public ForceRollbackException() {
        super();
    }

    public ForceRollbackException(String message) {
        super(message);
    }
}