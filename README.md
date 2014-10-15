jsBridge2jsr303
================

Thoughts(PoC) kind of DSL (JavaScript) for data validation on POJO'less Java backend.

Here is an example of field metadata:

```javascript
{
    "name: "integerField",
    "type: "INTEGER",
    "validator": "decimalMin({value: '0'}) && decimalMax({value: '10'}) && exact5()"
}
```

Field "validator" contains a mixture of JSR-303 validation constraints and user validator 'exact5'. The 'exact5' validator
was implemented as following javascript function:

```javascript
function exact5(message){
    if (currentFieldValue != '5') {
        if(typeof message === 'undefined'){
            message = "Values must be exactly '5';
        }
        __validator.addViolation('t', message, null);
        return false;
    }
    return true;
}
```

This JS function uses two of three implicit objects which are currently available in JS context as global vars:
    - \__validator - corresponding instance of Validator which performs validation;
    - current Field - metadata for a validating field;
    - currentFieldValue - value which is currently validated.

Entity validation might be performed as follows:
```java

final EntityInfo ei;
final String exact5str // <-- place JS

AbstractValidator validator = new HibernateValidator(){
        @Override
        protected void onInit() throws Exception {
            super.onInit();
            registerScript(exact5str);
        }
    };

final Set<ConstraintViolation> violations = validator.validate(ei, (Map<String, Object>) values);
if(violations != null){
    // Something invalid
}
```

You can find more examples in ProofOfUsageFieldValueValidationScenariosTest.java