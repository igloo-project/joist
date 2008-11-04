package features.domain;

import features.domain.mappers.ParentBChildBarAlias;
import features.domain.mappers.ParentBChildFooAlias;
import features.domain.mappers.ParentBParentAlias;
import java.util.ArrayList;
import java.util.List;
import org.exigencecorp.domainobjects.AbstractDomainObject;
import org.exigencecorp.domainobjects.Id;
import org.exigencecorp.domainobjects.Shim;
import org.exigencecorp.domainobjects.orm.AliasRegistry;
import org.exigencecorp.domainobjects.queries.Alias;
import org.exigencecorp.domainobjects.queries.Select;
import org.exigencecorp.domainobjects.uow.UoW;

public abstract class ParentBParentCodegen extends AbstractDomainObject {

    static {
        AliasRegistry.register(ParentBParent.class, new ParentBParentAlias("a"));
    }

    private Id<ParentBParent> id = null;
    private String name = null;
    private Integer version = null;
    private List<ParentBChildFoo> parentBChildFoos;
    private List<ParentBChildBar> parentBChildBars;

    public Alias<? extends ParentBParent> newAlias(String alias) {
        return new ParentBParentAlias(alias);
    }

    public Id<ParentBParent> getId() {
        return this.id;
    }

    public void setId(Id<ParentBParent> id) {
        this.recordIfChanged("id", this.id, id);
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.recordIfChanged("name", this.name, name);
        this.name = name;
    }

    public Integer getVersion() {
        return this.version;
    }

    public List<ParentBChildFoo> getParentBChildFoos() {
        if (this.parentBChildFoos == null) {
            if (UoW.isOpen() && this.getId() != null) {
                ParentBChildFooAlias a = new ParentBChildFooAlias("a");
                this.parentBChildFoos = Select.from(a).where(a.parentBParent.equals(this.getId())).orderBy(a.id.asc()).list();
            } else {
                this.parentBChildFoos = new ArrayList<ParentBChildFoo>();
            }
        }
        return this.parentBChildFoos;
    }

    public void addParentBChildFoo(ParentBChildFoo o) {
        o.setParentBParentWithoutPercolation((ParentBParent) this);
        this.addParentBChildFooWithoutPercolation(o);
    }

    public void addParentBChildFooWithoutPercolation(ParentBChildFoo o) {
        this.getParentBChildFoos(); // hack
        this.recordIfChanged("parentBChildFoos");
        this.parentBChildFoos.add(o);
    }

    public void removeParentBChildFoo(ParentBChildFoo o) {
        o.setParentBParentWithoutPercolation(null);
        this.removeParentBChildFooWithoutPercolation(o);
    }

    public void removeParentBChildFooWithoutPercolation(ParentBChildFoo o) {
        this.getParentBChildFoos(); // hack
        this.recordIfChanged("parentBChildFoos");
        this.parentBChildFoos.remove(o);
    }

    public List<ParentBChildBar> getParentBChildBars() {
        if (this.parentBChildBars == null) {
            if (UoW.isOpen() && this.getId() != null) {
                ParentBChildBarAlias a = new ParentBChildBarAlias("a");
                this.parentBChildBars = Select.from(a).where(a.parentBParent.equals(this.getId())).orderBy(a.id.asc()).list();
            } else {
                this.parentBChildBars = new ArrayList<ParentBChildBar>();
            }
        }
        return this.parentBChildBars;
    }

    public void addParentBChildBar(ParentBChildBar o) {
        o.setParentBParentWithoutPercolation((ParentBParent) this);
        this.addParentBChildBarWithoutPercolation(o);
    }

    public void addParentBChildBarWithoutPercolation(ParentBChildBar o) {
        this.getParentBChildBars(); // hack
        this.recordIfChanged("parentBChildBars");
        this.parentBChildBars.add(o);
    }

    public void removeParentBChildBar(ParentBChildBar o) {
        o.setParentBParentWithoutPercolation(null);
        this.removeParentBChildBarWithoutPercolation(o);
    }

    public void removeParentBChildBarWithoutPercolation(ParentBChildBar o) {
        this.getParentBChildBars(); // hack
        this.recordIfChanged("parentBChildBars");
        this.parentBChildBars.remove(o);
    }

    public static class Shims {
        public static final Shim<ParentBParent, Id<ParentBParent>> id = new Shim<ParentBParent, Id<ParentBParent>>() {
            public void set(ParentBParent instance, Id<ParentBParent> id) {
                ((ParentBParentCodegen) instance).id = id;
            }
            public Id<ParentBParent> get(ParentBParent instance) {
                return ((ParentBParentCodegen) instance).id;
            }
        };
        public static final Shim<ParentBParent, String> name = new Shim<ParentBParent, String>() {
            public void set(ParentBParent instance, String name) {
                ((ParentBParentCodegen) instance).name = name;
            }
            public String get(ParentBParent instance) {
                return ((ParentBParentCodegen) instance).name;
            }
        };
        public static final Shim<ParentBParent, Integer> version = new Shim<ParentBParent, Integer>() {
            public void set(ParentBParent instance, Integer version) {
                ((ParentBParentCodegen) instance).version = version;
            }
            public Integer get(ParentBParent instance) {
                return ((ParentBParentCodegen) instance).version;
            }
        };
    }

}
