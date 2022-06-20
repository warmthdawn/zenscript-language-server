package raylras.zen.ast.decl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import raylras.zen.ast.*;
import raylras.zen.ast.expr.Expression;
import raylras.zen.ast.visit.NodeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ParameterDeclaration extends BaseNode implements Declaration, Variable, LocatableID {

    @NotNull
    private final String name;
    @Nullable
    private final TypeDeclaration typeDecl;
    @Nullable
    private final Expression defaultValue;

    private Range idRange;

    public ParameterDeclaration(@NotNull String name, @Nullable TypeDeclaration typeDecl, @Nullable Expression defaultValue) {
        this.name = name;
        this.typeDecl = typeDecl;
        this.defaultValue = defaultValue;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public Optional<TypeDeclaration> getTypeDecl() {
        return Optional.ofNullable(typeDecl);
    }

    public Optional<Expression> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    @Override
    public <T> T accept(NodeVisitor<? extends T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<Node> getChildren() {
        if (typeDecl == null && defaultValue == null) {
            return Collections.emptyList();
        }
        List<Node> children = new ArrayList<>(2);
        if (typeDecl != null) children.add(typeDecl);
        if (defaultValue != null) children.add(defaultValue);
        return Collections.unmodifiableList(children);
    }

    @Override
    public Range getIdRange() {
        return idRange;
    }

    public void setIDRange(Range idRange) {
        this.idRange = idRange;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (getType() != null) {
            builder.append(" as ").append(getType());
        }
        if (defaultValue != null) {
            builder.append(" = ").append(defaultValue);
        }
        return builder.toString();
    }

}