package mobi.srg.js2jsr303;

import mobi.srg.js2jsr303.meta.EntityInfo;
import org.junit.Test;

import javax.script.ScriptException;
import javax.validation.*;

import java.lang.annotation.*;
import java.util.*;

import static org.junit.Assert.assertNull;

/**
 * The following scenario was identified:
 *    - validation by builtin jsr303 constraints
 *    - validation by Hibernate specific constraints
 *    - validation by user defined jsr303 constraints
 *    - validation by user defined JavaScript validation predicate
 */
public class ProofOfUsageFieldValueValidationScenariosTest extends AbstractValidationTest{

    @Target( { ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ExactStringValidator.class)
    @Documented
    public @interface ExactString{
        String message() default "{mobi.srg.js_driven_jsr303.ExactString.message}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
        String value();
    }

    private final String jsValidationPredicate = "function exact5(message){" +
            "   if (currentFieldValue != '5') {" +
            "        if(typeof message === 'undefined'){" +
            "           message = \"Values must be exactly '5'\"" +
            "        }" +
            "      __validator.addViolation('t', message, null);" +
            "      return false" +
            "   }" +
            "   return true;" +
            "}";

    @Override
    protected AbstractValidator createValidator() {
        return new HibernateValidator(){
            @Override
            protected void onInit() throws Exception {
                super.onInit();
                registerValidator(ExactString.class);
                    registerScript(jsValidationPredicate);
            }
        };
    }

    @Test
    public void singleValidationConstraintWithCustomMessage() throws ScriptException {
        final EntityInfo ei = parseAsSingleFieldEntity("TestEntity",
                "{\n" +
                    "name: 'integerField'," +
                    "type: 'INTEGER'," +
                    "validator: \"decimalMin({value: '0', message: 'something wrong'})\"" +
                "}");

        // positive
        Set<ConstraintViolation> violations = validate(ei, "integerField: 12");
        assertNull(violations);

        // negative
        violations = validate(ei, "integerField: -1");
        assertEqualsToSingleViolation("message: 'something wrong', propertyPath: 'integerField'", violations);
    }

    @Test
    public void severalValidationConstraintsAsABooleanExpression() throws ScriptException {
        final EntityInfo ei = parseAsSingleFieldEntity("Test Entity",
                "{" +
                    "name: 'integerField'," +
                    "type: 'INTEGER'," +
                    "validator: \"decimalMin({value: '0'}) && decimalMax({value: '10'})\"" +
                 "}"
            );

        // positive
        Set<ConstraintViolation> violations = validate(ei, "integerField: 5");
        assertNull(violations);

        // negative, min
        violations = validate(ei, "integerField: -15");
        assertEqualsToSingleViolation("message: 'must be greater than or equal to 0', propertyPath: 'integerField'", violations);

        // negative, max
        violations = validate(ei, "integerField: 15");
        assertEqualsToSingleViolation("message: 'must be less than or equal to 10', propertyPath: 'integerField'", violations);
    }

    @Test
    public void validationByUserDefinedValidator(){
        final EntityInfo ei = parseAsSingleFieldEntity("TestEntity",
                "{\n" +
                    "name: 'stringField'," +
                    "type: 'TEXT'," +
                    "validator: \"exactString({value: 'Bingo'})\"" +
                "}");

        // positive
        Set<ConstraintViolation> violations = validate(ei, "stringField: 'Bingo'");
        assertNull(violations);

        // negative, standard message
        violations = validate(ei, "stringField: 'Hey, what is going on?'");
        assertEqualsToSingleViolation("message: 'Must be exactly the same as \\'Bingo\\'', propertyPath: 'stringField'", violations);

        // negative, custom message
        violations = validate(ei, "stringField: \"exactString({value: 'Bingo', message: 'Ops, {value} was expected'})\"", "stringField: 'Hey, what is going on?'");
        assertEqualsToSingleViolation("message: 'Ops, Bingo was expected', propertyPath: 'stringField'", violations);
    }

    @Test
    public void validationByUserDefinedJavaScriptPredicate(){
        final EntityInfo ei = parseAsSingleFieldEntity("Test Entity",
                "{" +
                    "name: 'integerField'," +
                    "type: 'INTEGER'," +
                    "validator: \"decimalMin({value: '0'}) && decimalMax({value: '10'}) && exact5()\"" +
                "}"
        );

        // positive
        Set<ConstraintViolation> violations = validate(ei, "integerField: 5");
        assertNull(violations);


        // negative, exact5
        violations = validate(ei, "integerField: 6");
        assertEqualsToSingleViolation("message: 'Values must be exactly \\'5\\'', propertyPath: 'integerField'", violations);
    }

    public static class ExactStringValidator implements ConstraintValidator<ExactString, String>{
        private String expected;

        @Override
        public void initialize(ExactString constraintAnnotation) {
            expected = constraintAnnotation.value();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return expected.equals(value);
        }
    }
}
