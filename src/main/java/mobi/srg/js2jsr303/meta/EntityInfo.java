package mobi.srg.js2jsr303.meta;

import java.util.Iterator;
import java.util.Map;

public class EntityInfo extends UniquelyNamed implements Iterable<FieldInfo>{
    private Map<String, FieldInfo> fields;

    public Map<String, FieldInfo> getFields() {
        return fields;
    }

    public void setFields(Map<String, FieldInfo> fields) {
        this.fields = fields;
    }

    public FieldInfo get(String name){
        return fields.get(name);
    }

    @Override
    public Iterator<FieldInfo> iterator() {
        return fields.values().iterator();
    }
}
