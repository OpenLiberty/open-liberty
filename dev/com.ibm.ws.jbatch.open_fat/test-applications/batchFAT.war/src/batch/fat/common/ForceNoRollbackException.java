package batch.fat.common;

public class ForceNoRollbackException extends ForceRetryableException {

    /**  */
    private static final long serialVersionUID = 1L;

    public ForceNoRollbackException() {
        super();
    }

    public ForceNoRollbackException(String message) {
        super(message);
    }
}