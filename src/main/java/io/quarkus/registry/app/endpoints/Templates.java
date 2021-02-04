package io.quarkus.registry.app.endpoints;

import java.util.List;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.quarkus.registry.app.model.Extension;

@CheckedTemplate
public class Templates {
    /**
     * The index page
     */
    public static native TemplateInstance index(List<Extension> extensions);

    /**
     * The detail of an extension
     */
    public static native TemplateInstance extensionDetail(Extension extension);
}
