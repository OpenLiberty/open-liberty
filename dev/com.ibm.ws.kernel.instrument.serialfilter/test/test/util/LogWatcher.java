package test.util;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Watch the enabled logs for java.util.logging. Allow a test to expect particular logs,
 * and ensure that tests fail when unexpected WARNING or SEVERE messages are logged.
 */
public class LogWatcher implements TestRule{
    private static final String NAME = LogWatcher.class.getSimpleName();
    private static final Formatter logFormatter = new SimpleFormatter();
    private final List<LogRecord> logs = new ArrayList<LogRecord>();
    private final Set<LogRecord> expected = new HashSet<LogRecord>();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                logs.clear();
                final Handler handler = new Handler(){
                    public void publish(LogRecord record) {logs.add(record);}
                    public void flush() {}
                    public void close() throws SecurityException {}
                };
                Logger rootLogger = Logger.getLogger("");
                rootLogger.addHandler(handler);
                try {
                    System.out.println();
                    System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
                    System.out.println(NAME + " watching logs for test: " + description.getMethodName());
                    base.evaluate();
                } finally {
                    rootLogger.removeHandler(handler);
                    StringBuilder failures = new StringBuilder();
                    for (LogRecord rec : logs) {
                        if (expected.contains(rec)) continue;
                        if (rec.getLevel() == Level.SEVERE || rec.getLevel() == Level.WARNING) {
                            failures.append(logFormatter.format(rec).replaceAll("(?m:^)", "\t"));
                        }
                    }
                    if (failures.length() > 0) {
                        System.out.println(NAME + " found unexpected logs");
                        System.out.println(failures);
                    }
                    assertThat(failures.toString(), is(""));
                    System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                    System.out.println();
                }
            }
        };
    }

    public LogRecord expectLog(Level level, String pattern) {
        for (LogRecord rec : logs) {
            if (rec.getLevel() != level) continue;
            if (!!!rec.getMessage().contains(pattern)) continue;
            expected.add(rec);
            System.out.println(NAME + " found expected log:");
            System.out.println(logFormatter.format(rec).replaceAll("(?m:^)", "\t"));
            return rec;
        }
        Assert.fail("No log found matching level : " + level + ", and containing string: " +pattern);
        throw new Error(); // never happens
    }
}
