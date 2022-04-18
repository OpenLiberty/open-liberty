/**
 *
 */
package test.jakarta.concurrency.ejb.error;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This bean has a class that uses the Jakarta Concurrency @Asynchronous annotation.
 * This annotation is only allowed on CDI bean methods, and should be rejected on a class level.
 * This application should not be installed and hence none of these methods should be run.
 */
@Asynchronous
@Stateless
@Local(GenericLocal.class)
public class AsynchClassBean {

    public void getThreadName() {
        System.out.println("Thread name async: " + Thread.currentThread().getName());
    }

    public void getThreadNameNonAsyc() {
        System.out.println("Thread name non-async:" + Thread.currentThread().getName());
    }

    public CompletableFuture<String> getState(String city) {
        if (city == "Rochester")
            return Asynchronous.Result.complete("Minnesota");
        else
            return Asynchronous.Result.complete(null);
    }

    public CompletableFuture<String> getStateFromService(String city) {

        CompletableFuture<String> future = Asynchronous.Result.getFuture();

        assertNotNull(future);

        try {
            if (city == "Rochester")
                future.complete("Minnesota");
            else
                future.complete(null);
        } catch (Exception x) {
            future.completeExceptionally(x);
        }

        return future;
    }

}
