/**
 *
 */
package com.ibm.ws.jpa.tests.spec10.callback.tests;

import java.util.regex.Pattern;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 */
public class SkipDatabaseRule implements TestRule {

    private String database;

    public void setDatabase(String databaseName) {
        this.database = databaseName;
    }

    public boolean isSkipping() {
        return (database != null
                && Pattern.compile("oracle", Pattern.CASE_INSENSITIVE).matcher(database).find());
    }

    @Override
    public Statement apply(Statement arg0, Description arg1) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                System.out.println("Checking DB to skip tests");
                if (isSkipping()) {
                    throw new AssumptionViolatedException("Database is Oracle. Skipping test!");
                } else {
                    System.out.println("Not Skipping");
                    arg0.evaluate();
                }
            }
        };
    }

}
