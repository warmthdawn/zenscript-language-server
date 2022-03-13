package raylras.zen.ast.type;

public class TypeLong extends Type {

    public static final TypeLong INSTANCE = new TypeLong();

    private TypeLong() {}

    @Override
    public String toString() {
        return "long";
    }

}