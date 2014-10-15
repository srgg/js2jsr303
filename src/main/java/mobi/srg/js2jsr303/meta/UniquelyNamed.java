package mobi.srg.js2jsr303.meta;

class UniquelyNamed {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UniquelyNamed that = (UniquelyNamed) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"@" + Integer.toHexString(hashCode()) +
                "{" +
                "name='" + name + '\'' +
                '}';
    }
}
