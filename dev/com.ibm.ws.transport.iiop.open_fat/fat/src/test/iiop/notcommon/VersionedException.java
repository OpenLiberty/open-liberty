/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.notcommon;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * client-side version
 */
public class VersionedException extends RuntimeException {
    private static final long serialVersionUID = 796770993296843510L;
    private final Exception causeException;
    @SuppressWarnings("unused")
    private final String clientField;

    public VersionedException() {
        this(null);
    }

    public VersionedException(Exception causeException) {
        super("created in the client");
        this.causeException = causeException;
        this.clientField = "client field";
    }

    public Exception getCausedByException() {
        return causeException;
    }

    @Override
    public Throwable getCause() {
        return super.getCause() != null ? super.getCause() : getCausedByException();
    }

    @Override
    public String getMessage() {
        if (causeException == null) {
            return super.getMessage();
        }

        StringBuilder sb = new StringBuilder();

        if (super.getMessage() != null) {
            sb.append(super.getMessage());
            sb.append("; ");
        }

        sb.append("nested exception is: ");
        sb.append(causeException.toString());

        return sb.toString();
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        if (causeException == null || super.getCause() == null || causeException == super.getCause()) {
            super.printStackTrace(ps);
        } else {
            synchronized (ps) {
                ps.println(this);
                causeException.printStackTrace(ps);
                super.printStackTrace(ps);
            }
        }
    }

    @Override
    public void printStackTrace(PrintWriter pw) {
        if (causeException == null || super.getCause() == null || causeException == super.getCause()) {
            super.printStackTrace(pw);
        } else {
            synchronized (pw) {
                pw.println(this);
                causeException.printStackTrace(pw);
                super.printStackTrace(pw);
            }
        }
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }
}
