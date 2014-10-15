package mobi.srg.js2jsr303.jsr303;

import javax.validation.MessageInterpolator;
import javax.validation.metadata.ConstraintDescriptor;

class MessageInterpolatorContextImpl implements MessageInterpolator.Context {

    private final ConstraintDescriptor constraintDescriptor;
    private final Object validatedValue;

    private MessageInterpolatorContextImpl(ConstraintDescriptor constraintDescriptor, Object validatedValue) {
        this.constraintDescriptor = constraintDescriptor;
        this.validatedValue = validatedValue;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    @Override
    public Object getValidatedValue() {
        return validatedValue;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return (T)this;
    }

    public static MessageInterpolatorContextImpl create(ConstraintDescriptor constraintDescriptor, Object validatedValue){
        return new MessageInterpolatorContextImpl(constraintDescriptor, validatedValue);
    }
}
