package joist.web.controls.form;

import joist.web.CurrentContext;
import joist.web.util.HtmlWriter;

import org.exigencecorp.bindgen.Binding;

public class HiddenField extends AbstractField<HiddenField> {

    public HiddenField() {
    }

    public HiddenField(Binding<?> binding) {
        super(binding);
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    public void render(HtmlWriter w) {
        String valueForUrl = CurrentContext.get().getClickConfig().getUrlConverterRegistry().convert(this.getBoundValue(), String.class);
        w.append("<input id={} name={} type={} value={}/>", this.getFullId(), this.getId(), "hidden", valueForUrl);
    }

    public HiddenField getThis() {
        return this;
    }

}
