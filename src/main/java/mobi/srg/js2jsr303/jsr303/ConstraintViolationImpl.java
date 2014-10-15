package mobi.srg.js2jsr303.jsr303;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

class ConstraintViolationImpl implements ConstraintViolation {

    private final String message;
    private final ConstraintDescriptor constraintDescriptor;
    private final String path;
    private final Object invalidValue;
    private final Path propertyPath;

    private ConstraintViolationImpl(String message, ConstraintDescriptor constraintDescriptor, String propertyPath, Object invalidValue) {
        this.message = message;
        this.constraintDescriptor = constraintDescriptor;
        this.path = propertyPath;
        this.invalidValue = invalidValue;
        this.propertyPath = PathImpl.create(path);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageTemplate() {
        return constraintDescriptor.getMessageTemplate();
    }

    @Override
    public Object getRootBean() {
        return null;
    }

    @Override
    public Class getRootBeanClass() {
        return null;
    }

    @Override
    public Object getLeafBean() {
        return null;
    }

    @Override
    public Object[] getExecutableParameters() {
        return null;
    }

    @Override
    public Object getExecutableReturnValue() {
        return null;
    }

    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }

    @Override
    public Object getInvalidValue() {
        return invalidValue;
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    @Override
    public Object unwrap(Class type) {
        return type.cast( this );
    }

    public static ConstraintViolation create(String message, ConstraintDescriptor constraintDescriptor, String propertyPath, Object invalidValue){
        return new ConstraintViolationImpl(message, constraintDescriptor, propertyPath, invalidValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstraintViolationImpl that = (ConstraintViolationImpl) o;

        if (invalidValue != null ? !invalidValue.equals(that.invalidValue) : that.invalidValue != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (propertyPath != null ? !propertyPath.equals(that.propertyPath) : that.propertyPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (propertyPath != null ? propertyPath.hashCode() : 0);
        result = 31 * result + (invalidValue != null ? invalidValue.hashCode() : 0);
        return result;
    }
}
