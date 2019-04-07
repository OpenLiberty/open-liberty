package test.util;

public enum Exceptions {;
    public static RuntimeException rethrow(Throwable t) {throw throwAs(RuntimeException.class, t);}
    /** this method fools the compiler into throwing any throwable as if it were a T */
    private static <T extends Throwable> T throwAs(Class<T> clazz, Throwable t) throws T { throw (T) t;}
}
