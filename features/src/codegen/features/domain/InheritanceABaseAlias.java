package features.domain;

import java.util.ArrayList;
import java.util.List;
import joist.domain.orm.queries.Alias;
import joist.domain.orm.queries.columns.AliasColumn;
import joist.domain.orm.queries.columns.IdAliasColumn;
import joist.domain.orm.queries.columns.IntAliasColumn;
import joist.domain.orm.queries.columns.StringAliasColumn;

public class InheritanceABaseAlias extends Alias<InheritanceABase> {

    private final List<AliasColumn<InheritanceABase, ?, ?>> columns = new ArrayList<AliasColumn<InheritanceABase, ?, ?>>();
    public final IdAliasColumn<InheritanceABase> id = new IdAliasColumn<InheritanceABase>(this, "id", InheritanceABaseCodegen.Shims.id);
    public final StringAliasColumn<InheritanceABase> name = new StringAliasColumn<InheritanceABase>(this, "name", InheritanceABaseCodegen.Shims.name);
    public final IntAliasColumn<InheritanceABase> version = new IntAliasColumn<InheritanceABase>(this, "version", InheritanceABaseCodegen.Shims.version);

    public InheritanceABaseAlias(String alias) {
        super(InheritanceABase.class, "inheritance_a_base", alias);
        InheritanceABaseAlias inheritanceABase = this;
        InheritanceASubOneAlias inheritanceASubOne = new InheritanceASubOneAlias(inheritanceABase, alias + "_0");
        this.addSubClassAlias(inheritanceASubOne);
        InheritanceASubTwoAlias inheritanceASubTwo = new InheritanceASubTwoAlias(inheritanceABase, alias + "_1");
        this.addSubClassAlias(inheritanceASubTwo);
        this.columns.add(this.id);
        this.columns.add(this.name);
        this.columns.add(this.version);
    }

    public List<AliasColumn<InheritanceABase, ?, ?>> getColumns() {
        return this.columns;
    }

    public IdAliasColumn<InheritanceABase> getIdColumn() {
        return this.id;
    }

    public IntAliasColumn<InheritanceABase> getVersionColumn() {
        return this.version;
    }

    public IdAliasColumn<InheritanceABase> getSubClassIdColumn() {
        return null;
    }

    public int getOrder() {
        return 3;
    }

}
