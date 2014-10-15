package mobi.srg.js2jsr303.jsr303;

import javax.validation.ElementKind;
import javax.validation.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class PathImpl implements Path{
    private final String path;
    private final List<Node> elements;

    private PathImpl(String path) {
        this.path = path;
        final String pathElements[] = path.split(".");

        elements = new ArrayList<>(pathElements.length);
        for(String pe:pathElements){
            final Node n = createPropertyNode(pe);
            elements.add(n);
        }
    }

    @Override
    public Iterator<Node> iterator() {
        return elements.iterator();
    }

    private static Node createPropertyNode(final String property){
        return  new PropertyNode() {
            @Override
            public String getName() {
                return property;
            }

            @Override
            public boolean isInIterable() {
                return false;
            }

            @Override
            public Integer getIndex() {
                return null;
            }

            @Override
            public Object getKey() {
                return null;
            }

            @Override
            public ElementKind getKind() {
                return ElementKind.PROPERTY;
            }

            @Override
            public <T extends Node> T as(Class<T> nodeType) {
                return null;
            }
        };
    }

    public static Path create(String path){
        return new PathImpl(path);
    }

    @Override
    public String toString() {
        return path;
    }
}
