package org.exigencecorp.domainobjects.codegen.passes;

import java.util.ArrayList;
import java.util.List;

import org.exigencecorp.domainobjects.AbstractChanged;
import org.exigencecorp.domainobjects.Changed;
import org.exigencecorp.domainobjects.Shim;
import org.exigencecorp.domainobjects.codegen.Codegen;
import org.exigencecorp.domainobjects.codegen.dtos.Entity;
import org.exigencecorp.domainobjects.codegen.dtos.ManyToManyProperty;
import org.exigencecorp.domainobjects.codegen.dtos.ManyToOneProperty;
import org.exigencecorp.domainobjects.codegen.dtos.OneToManyProperty;
import org.exigencecorp.domainobjects.codegen.dtos.PrimitiveProperty;
import org.exigencecorp.domainobjects.orm.AliasRegistry;
import org.exigencecorp.domainobjects.orm.ForeignKeyCodeHolder;
import org.exigencecorp.domainobjects.orm.ForeignKeyHolder;
import org.exigencecorp.domainobjects.orm.ForeignKeyListHolder;
import org.exigencecorp.domainobjects.uow.UoW;
import org.exigencecorp.domainobjects.validation.rules.MaxLength;
import org.exigencecorp.domainobjects.validation.rules.NotNull;
import org.exigencecorp.gen.GClass;
import org.exigencecorp.gen.GField;
import org.exigencecorp.gen.GMethod;
import org.exigencecorp.util.Copy;

public class GenerateDomainCodegenPass implements Pass {

    public void pass(Codegen codegen) {
        for (Entity entity : codegen.getEntities().values()) {
            if (entity.isCodeEntity()) {
                continue;
            }

            GClass domainCodegen = codegen.getOutputCodegenDirectory().getClass(entity.getFullCodegenClassName());
            domainCodegen.setAbstract();
            domainCodegen.baseClassName(entity.getParentClassName());

            domainCodegen.getConstructor().setProtected().body.line("this.addExtraRules();");
            domainCodegen.getMethod("addExtraRules").setPrivate();

            this.addAlias(domainCodegen, entity);
            this.primitiveProperties(domainCodegen, entity);
            this.manyToOneProperties(domainCodegen, entity);
            this.oneToManyProperties(domainCodegen, entity);
            this.manyToManyProperties(domainCodegen, entity);
            this.changed(domainCodegen, entity);
        }
    }

    private void addAlias(GClass domainCodegen, Entity entity) {
        if (!entity.isCodeEntity()) {
            GField alias = domainCodegen.getField("alias") //
                .setStatic()
                .setProtected()
                .type("{}Alias", entity.getClassName());
            if (entity.isSubclass()) {
                alias.addAnnotation("@SuppressWarnings(\"hiding\")");
            }

            GField query = domainCodegen.getField("queries").setPublic().setStatic().setFinal();
            query.type("{}Queries", entity.getClassName());
            if (entity.isSubclass()) {
                query.addAnnotation("@SuppressWarnings(\"hiding\")");
            }
            domainCodegen.addImports(entity.getFullQueriesClassName());

            domainCodegen.staticInitializer.line("alias = new {}Alias(\"a\");", entity.getClassName());
            domainCodegen.staticInitializer.line("AliasRegistry.register({}.class, alias);", entity.getClassName());
            domainCodegen.staticInitializer.line("queries = new {}Queries();", entity.getClassName());
            domainCodegen.addImports(AliasRegistry.class);
            domainCodegen.addImports(entity.getFullAliasClassName());
        }
    }

    private void primitiveProperties(GClass domainCodegen, Entity entity) {
        for (PrimitiveProperty p : entity.getPrimitiveProperties()) {
            GField field = domainCodegen.getField(p.getVariableName());
            field.type(p.getJavaType());
            field.initialValue(p.getDefaultJavaString());
            field.makeGetter();

            if (!"version".equals(p.getColumnName())) {
                GMethod setter = domainCodegen.getMethod("set" + p.getCapitalVariableName());
                setter.argument(p.getJavaType(), p.getVariableName());
                setter.body.line("this.getChanged().record(\"{}\", this.{}, {});", p.getVariableName(), p.getVariableName(), p.getVariableName());
                setter.body.line("this.{} = {};", p.getVariableName(), p.getVariableName());
                if ("id".equals(p.getColumnName())) {
                    setter.body.line("if (UoW.isOpen()) {");
                    setter.body.line("    UoW.getIdentityMap().store(this);");
                    setter.body.line("}");
                    domainCodegen.addImports(UoW.class);
                }
            }

            GClass shims = domainCodegen.getInnerClass("Shims");
            GField shimField = shims.getField(p.getVariableName()).setPublic().setStatic().setFinal();
            shimField.type("Shim<" + entity.getClassName() + ", " + p.getJavaType() + ">");
            GClass shimClass = shimField.initialAnonymousClass();

            GMethod shimSetter = shimClass.getMethod("set");
            shimSetter.argument(entity.getClassName(), "instance").argument(p.getJavaType(), p.getVariableName());
            shimSetter.body.line("(({}) instance).{} = {};", entity.getCodegenClassName(), p.getVariableName(), p.getVariableName());

            GMethod shimGetter = shimClass.getMethod("get");
            shimGetter.argument(entity.getClassName(), "instance");
            shimGetter.returnType(p.getJavaType());
            shimGetter.body.line("return (({}) instance).{};", entity.getCodegenClassName(), p.getVariableName());

            if (p.shouldHaveNotNullRule()) {
                domainCodegen.getMethod("addExtraRules").body.line("this.addRule(new NotNull<{}>(\"{}\", Shims.{}));",//
                    entity.getClassName(),
                    p.getVariableName(),
                    p.getVariableName());
                domainCodegen.addImports(NotNull.class);
            }

            if (p.getMaxCharacterLength() != 0) {
                domainCodegen.getMethod("addExtraRules").body.line("this.addRule(new MaxLength<{}>(\"{}\", {}, Shims.{}));",//
                    entity.getClassName(),
                    p.getVariableName(),
                    p.getMaxCharacterLength(),
                    p.getVariableName());
                domainCodegen.addImports(MaxLength.class);
            }

            domainCodegen.addImports(Shim.class);
        }
    }

    private void manyToOneProperties(GClass domainCodegen, Entity entity) {
        for (ManyToOneProperty mtop : entity.getManyToOneProperties()) {
            GField field = domainCodegen.getField(mtop.getVariableName());
            if (mtop.getManySide().isCodeEntity()) {
                field.type("ForeignKeyCodeHolder<" + mtop.getJavaType() + ">");
                field.initialValue("new ForeignKeyCodeHolder<" + mtop.getJavaType() + ">(" + mtop.getJavaType() + ".class)");
                domainCodegen.addImports(ForeignKeyCodeHolder.class);
            } else {
                field.type("ForeignKeyHolder<" + mtop.getJavaType() + ">");
                field.initialValue("new ForeignKeyHolder<" + mtop.getJavaType() + ">(" + mtop.getJavaType() + ".class)");
                domainCodegen.addImports(ForeignKeyHolder.class);
            }

            GMethod getter = domainCodegen.getMethod("get" + mtop.getCapitalVariableName());
            getter.returnType(mtop.getJavaType());
            getter.body.line("return this.{}.get();", mtop.getVariableName());

            GMethod setter = domainCodegen.getMethod("set{}", mtop.getCapitalVariableName());
            setter.argument(mtop.getJavaType(), mtop.getVariableName());
            if (!mtop.getManySide().isCodeEntity()) {
                setter.body.line("if (this.{}.get() != null) {", mtop.getVariableName());
                setter.body.line("   this.{}.get().remove{}WithoutPercolation(({}) this);",//
                    mtop.getVariableName(),
                    mtop.getOneToManyProperty().getCapitalVariableNameSingular(),
                    entity.getClassName());
                setter.body.line("}");
                if (mtop.getOneToManyProperty().isOneToOne()) {
                    setter.body.line("{}.set{}(null);", mtop.getVariableName(), mtop.getOneToManyProperty().getCapitalVariableNameSingular());
                }
            }
            setter.body.line("this.set{}WithoutPercolation({});", mtop.getCapitalVariableName(), mtop.getVariableName());
            if (!mtop.getManySide().isCodeEntity()) {
                setter.body.line("if (this.{}.get() != null) {", mtop.getVariableName());
                setter.body.line("   this.{}.get().add{}WithoutPercolation(({}) this);",//
                    mtop.getVariableName(),
                    mtop.getOneToManyProperty().getCapitalVariableNameSingular(),
                    entity.getClassName());
                setter.body.line("}");
            }

            GMethod setter2 = domainCodegen.getMethod("set{}WithoutPercolation", mtop.getCapitalVariableName()).setProtected();
            setter2.argument(mtop.getJavaType(), mtop.getVariableName());
            setter2.body.line("this.getChanged().record(\"{}\", this.{}, {});", mtop.getVariableName(), mtop.getVariableName(), mtop
                .getVariableName());
            setter2.body.line("this.{}.set({});", mtop.getVariableName(), mtop.getVariableName());

            GClass shims = domainCodegen.getInnerClass("Shims");
            GField shimField = shims.getField(mtop.getVariableName() + "Id").setPublic().setStatic().setFinal();
            shimField.type("Shim<" + entity.getClassName() + ", Integer>");
            GClass shimClass = shimField.initialAnonymousClass();

            GMethod shimSetter = shimClass.getMethod("set");
            shimSetter.argument(entity.getClassName(), "instance").argument("Integer", mtop.getVariableName() + "Id");
            shimSetter.body.line("(({}) instance).{}.setId({}Id);", entity.getCodegenClassName(), mtop.getVariableName(), mtop.getVariableName());

            GMethod shimGetter = shimClass.getMethod("get");
            shimGetter.argument(entity.getClassName(), "instance");
            shimGetter.returnType("Integer");
            shimGetter.body.line(0, "return (({}) instance).{}.getId();", entity.getCodegenClassName(), mtop.getVariableName());

            domainCodegen.addImports(Shim.class);
        }
    }

    private void oneToManyProperties(GClass domainCodegen, Entity entity) {
        for (OneToManyProperty otmp : entity.getOneToManyProperties()) {
            GField collection = domainCodegen.getField(otmp.getVariableName());
            collection.type("ForeignKeyListHolder<{}, {}>", entity.getClassName(), otmp.getTargetJavaType());
            collection.initialValue("new ForeignKeyListHolder<{}, {}>(({}) this, {}.alias, {}.alias.{})",//
                entity.getClassName(),
                otmp.getTargetJavaType(),
                entity.getClassName(),
                otmp.getTargetJavaType() + "Codegen",
                otmp.getTargetJavaType() + "Codegen",
                otmp.getKeyFieldName());
            domainCodegen.addImports(ForeignKeyListHolder.class);

            if (!otmp.isOneToOne()) {
                GMethod getter = domainCodegen.getMethod("get" + otmp.getCapitalVariableName()).returnType(otmp.getJavaType());
                getter.body.line("return this.{}.get();", otmp.getVariableName());

                GMethod adder = domainCodegen.getMethod("add{}", otmp.getCapitalVariableNameSingular());
                adder.argument(otmp.getTargetJavaType(), "o");
                adder.body.line("o.set{}WithoutPercolation(({}) this);", otmp.getForeignKeyColumn().getCapitalVariableName(), entity.getClassName());
                adder.body.line("this.add{}WithoutPercolation(o);", otmp.getCapitalVariableNameSingular());

                GMethod remover = domainCodegen.getMethod("remove{}", otmp.getCapitalVariableNameSingular());
                remover.argument(otmp.getTargetJavaType(), "o");
                remover.body.line("o.set{}WithoutPercolation(null);", otmp.getForeignKeyColumn().getCapitalVariableName(), entity.getClassName());
                remover.body.line("this.remove{}WithoutPercolation(o);", otmp.getCapitalVariableNameSingular());
                domainCodegen.addImports(List.class);
            } else {
                GMethod getter = domainCodegen.getMethod("get" + otmp.getCapitalVariableNameSingular()).returnType(otmp.getTargetJavaType());
                getter.body.line("return (this.{}.get().size() == 0) ? null : this.{}.get().get(0);", otmp.getVariableName(), otmp.getVariableName());

                GMethod setter = domainCodegen.getMethod("set" + otmp.getCapitalVariableNameSingular());
                setter.argument(otmp.getTargetJavaType(), "n");
                setter.body.line("{} o = this.get{}();", otmp.getTargetJavaType(), otmp.getCapitalVariableNameSingular());
                setter.body.line("if (o != null) {", otmp.getVariableName());
                setter.body.line("    o.set{}WithoutPercolation(null);", otmp.getForeignKeyColumn().getCapitalVariableName(), entity.getClassName());
                setter.body.line("    this.remove{}WithoutPercolation(o);", otmp.getCapitalVariableNameSingular());
                setter.body.line("}");
                setter.body.line("if (n != null) {");
                setter.body.line("    n.set{}WithoutPercolation(({}) this);",//
                    otmp.getForeignKeyColumn().getCapitalVariableName(),
                    entity.getClassName());
                setter.body.line("    this.add{}WithoutPercolation(n);", otmp.getCapitalVariableNameSingular());
                setter.body.line("}");
            }

            GMethod adder2 = domainCodegen.getMethod("add{}WithoutPercolation", otmp.getCapitalVariableNameSingular());
            adder2.argument(otmp.getTargetJavaType(), "o").setProtected();
            adder2.body.line("this.getChanged().record(\"{}\");", otmp.getVariableName());
            adder2.body.line("this.{}.add(o);", otmp.getVariableName());

            GMethod remover2 = domainCodegen.getMethod("remove{}WithoutPercolation", otmp.getCapitalVariableNameSingular());
            remover2.argument(otmp.getTargetJavaType(), "o").setProtected();
            remover2.body.line("this.getChanged().record(\"{}\");", otmp.getVariableName());
            remover2.body.line("this.{}.remove(o);", otmp.getVariableName());

            domainCodegen.addImports(otmp.getOneSide().getFullAliasClassName());
        }
    }

    private void manyToManyProperties(GClass domainCodegen, Entity entity) {
        for (ManyToManyProperty mtmp : entity.getManyToManyProperties()) {
            GMethod getter = domainCodegen.getMethod("get" + mtmp.getCapitalVariableName()).returnType(mtmp.getJavaType());
            getter.body.line("{} l = {};", mtmp.getJavaType(), mtmp.getDefaultJavaString());
            getter.body.line("for ({} o : this.get{}()) {",//
                mtmp.getJoinTable().getClassName(),
                mtmp.getMySideManyToOne().getOneToManyProperty().getCapitalVariableName());
            getter.body.line("    l.add(o.get{}());", mtmp.getCapitalVariableNameSingular());
            getter.body.line("}");
            getter.body.line("return l;");

            GMethod adder = domainCodegen.getMethod("add{}", mtmp.getCapitalVariableNameSingular());
            adder.argument(mtmp.getTargetTable().getClassName(), "o");
            adder.body.line("{} a = new {}();", mtmp.getJoinTable().getClassName(), mtmp.getJoinTable().getClassName());
            adder.body.line("a.set{}(({}) this);", mtmp.getMySideManyToOne().getCapitalVariableName(), entity.getClassName());
            adder.body.line("a.set{}(o);", mtmp.getOther().getMySideManyToOne().getCapitalVariableName(), mtmp.getTargetTable().getClassName());

            GMethod remover = domainCodegen.getMethod("remove{}", mtmp.getCapitalVariableNameSingular());
            remover.argument(mtmp.getTargetTable().getClassName(), "o");
            remover.body.line("for ({} a : Copy.shallow(this.get{}())) {",//
                mtmp.getJoinTable().getClassName(),
                mtmp.getMySideManyToOne().getOneToManyProperty().getCapitalVariableName());
            remover.body.line("    if (a.get{}().equals(o)) {", mtmp.getCapitalVariableNameSingular());
            remover.body.line("        a.set{}(null);", mtmp.getCapitalVariableNameSingular());
            remover.body.line("        a.set{}(null);", mtmp.getOther().getCapitalVariableNameSingular());
            remover.body.line("        if (UoW.isOpen()) {");
            remover.body.line("            UoW.delete(a);");
            remover.body.line("        }");
            remover.body.line("    }");
            remover.body.line("}");

            domainCodegen.addImports(Copy.class, ArrayList.class, UoW.class);
        }
    }

    private void changed(GClass domainCodegen, Entity entity) {
        if (entity.isRoot()) {
            domainCodegen.getField("changed").type(Changed.class).setProtected();
        }

        GMethod getter = domainCodegen.getMethod("getChanged").returnType("{}Changed", entity.getClassName());
        getter.body.line("if (this.changed == null) {");
        getter.body.line("    this.changed = new {}Changed(({}) this);", entity.getClassName(), entity.getClassName());
        getter.body.line("}");
        getter.body.line("return ({}Changed) this.changed;", entity.getClassName());

        GClass changedClass = domainCodegen.getInnerClass("{}Changed", entity.getClassName());
        if (entity.isRoot()) {
            changedClass.baseClass(AbstractChanged.class);
        } else {
            changedClass.baseClassName("{}Changed", entity.getBaseEntity().getClassName());
        }
        changedClass.getConstructor(entity.getClassName() + " instance").body.line("super(instance);", entity.getClassName());

        for (PrimitiveProperty p : entity.getPrimitiveProperties()) {
            GMethod has = changedClass.getMethod("has{}", p.getCapitalVariableName()).returnType(boolean.class);
            has.body.line("return this.contains(\"{}\");", p.getVariableName());

            GMethod original = changedClass.getMethod("getOriginal{}", p.getCapitalVariableName()).returnType(p.getJavaType());
            original.body.line("return ({}) this.getOriginal(\"{}\");", p.getJavaType(), p.getVariableName());
        }

        for (ManyToOneProperty mtop : entity.getManyToOneProperties()) {
            GMethod has = changedClass.getMethod("has{}", mtop.getCapitalVariableName()).returnType(boolean.class);
            has.body.line("return this.contains(\"{}\");", mtop.getVariableName());

            GMethod original = changedClass.getMethod("getOriginal{}", mtop.getCapitalVariableName()).returnType(mtop.getJavaType());
            original.body.line("return ({}) this.getOriginal(\"{}\");", mtop.getJavaType(), mtop.getVariableName());
        }

        for (OneToManyProperty otmp : entity.getOneToManyProperties()) {
            GMethod has = changedClass.getMethod("has{}", otmp.getCapitalVariableName()).returnType(boolean.class);
            has.body.line("return this.contains(\"{}\");", otmp.getVariableName());
        }
    }
}
