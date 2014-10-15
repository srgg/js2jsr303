package mobi.srg.js2jsr303.meta;

import java.math.BigDecimal;

public class FieldInfo extends UniquelyNamed{
    public enum FieldType {
        TEXT,
        BOOLEAN,
        INTEGER,
        BIG_DECIMAL,
        LONG,
    }

    private FieldType type;
    private String validator;

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public String getValidator() {
        return validator;
    }

    public void setValidator(String validator) {
        this.validator = validator;
    }

    public Object parseValue(String value ){
        switch (type){
            case BIG_DECIMAL:
                return new BigDecimal(value);
            case BOOLEAN:
                return Boolean.parseBoolean(value);
            case INTEGER:
                return Integer.parseInt(value);
            case LONG:
                return Long.parseLong(value);
            case TEXT:
                return value;
            default:
                throw new IllegalStateException(String.format("Unsupported field type '%s'", type.name()));
        }
    }
}
