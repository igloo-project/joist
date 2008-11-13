package features.domain;

import features.domain.InheritanceASubOneAlias;
import features.domain.queries.InheritanceASubOneQueries;
import org.exigencecorp.domainobjects.Shim;
import org.exigencecorp.domainobjects.orm.AliasRegistry;
import org.exigencecorp.domainobjects.validation.rules.MaxLength;
import org.exigencecorp.domainobjects.validation.rules.NotNull;

abstract class InheritanceASubOneCodegen extends InheritanceABase {

    static {
        AliasRegistry.register(InheritanceASubOne.class, new InheritanceASubOneAlias("a"));
    }

    public static final @SuppressWarnings("hiding") InheritanceASubOneQueries queries = new InheritanceASubOneQueries();
    private String one = null;

    protected InheritanceASubOneCodegen() {
        this.addExtraRules();
    }

    private void addExtraRules() {
        this.addRule(new NotNull<InheritanceASubOne>("one", Shims.one));
        this.addRule(new MaxLength<InheritanceASubOne>("one", 100, Shims.one));
    }

    public String getOne() {
        return this.one;
    }

    public void setOne(java.lang.String one) {
        this.recordIfChanged("one", this.one, one);
        this.one = one;
    }

    public static class Shims {
        public static final Shim<InheritanceASubOne, java.lang.String> one = new Shim<InheritanceASubOne, java.lang.String>() {
            public void set(InheritanceASubOne instance, java.lang.String one) {
                ((InheritanceASubOneCodegen) instance).one = one;
            }
            public String get(InheritanceASubOne instance) {
                return ((InheritanceASubOneCodegen) instance).one;
            }
        };
    }

}
