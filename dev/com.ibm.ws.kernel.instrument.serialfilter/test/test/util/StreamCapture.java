package test.util;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StreamCapture implements TestRule {
    private final boolean checkStdOut, checkStdErr;
    private ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    PrintStream oldOut;
    PrintStream oldErr;

    public StreamCapture() { this(false, false); }

    public StreamCapture(boolean checkStdOut, boolean checkStdErr) {
        this.checkStdOut = checkStdOut;
        this.checkStdErr = checkStdErr;
    }


    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                oldOut = System.out;
                oldErr = System.err;
                final String out, err;
                try {
                    System.setOut(new PrintStream(outBytes));
                    System.setErr(new PrintStream(errBytes));
                    base.evaluate();
                } finally {
                    System.out.flush();
                    System.out.close();
                    System.setOut(oldOut);
                    out = new String(outBytes.toByteArray());
                    System.err.close();
                    System.setErr(oldErr);
                    err = new String(errBytes.toByteArray());
                }
                if (checkStdOut) Assert.assertEquals("", out);
                if (checkStdErr) Assert.assertEquals("", err);
            }
        };
    }

    public String getOut() {
        System.out.flush();
        System.out.close();
        String result = new String(outBytes.toByteArray());
        oldOut.print(result);
        System.setOut(new PrintStream(outBytes = new ByteArrayOutputStream()));
        return result;
    }

    public String getErr() {
        System.err.flush();
        System.err.close();
        String result = new String(errBytes.toByteArray());
        errBytes.reset();
        oldErr.print(result);
        System.setErr(new PrintStream(errBytes = new ByteArrayOutputStream()));
        return result;
    }
}
