package bval.v20.customprovider;

import java.util.Set;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

public class MyCustomValidatorFactory implements ValidatorFactory {

    @Override
    public Validator getValidator() {
        return new MyCustomValidator();
    }

    @Override
    public ValidatorContext usingContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClockProvider getClockProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> paramClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    public static class MyCustomValidator implements Validator {

        @Override
        public ExecutableValidator forExecutables() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BeanDescriptor getConstraintsForClass(Class<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validate(T arg0, Class<?>... arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateProperty(T arg0, String arg1, Class<?>... arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateValue(Class<T> arg0, String arg1, Object arg2, Class<?>... arg3) {
            throw new UnsupportedOperationException();
        }

    }

}
