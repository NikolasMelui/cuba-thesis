package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.app.UIComponentsConfig;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.export.ExportDataProvider;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.FileResource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ApplicationResource;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Nikolay Gorodnov
 * Date: 22.06.2009
 */
public class Embedded
        extends AbstractComponent<com.vaadin.ui.Embedded>
        implements com.haulmont.cuba.gui.components.Embedded, Component.Disposable
{
    private Map<String, String> parameters = null;
    private Type type = Type.OBJECT;
    private ApplicationResource resource;
    private boolean disposed;

    public Embedded() {
        component = new com.vaadin.ui.Embedded();
        provideType();
    }

    public void setSource(URL src) {
        component.setSource(new ExternalResource(src));
    }

    public void setSource(String src) {
        File file = new File(src);
        if (!file.isAbsolute()) {
            UIComponentsConfig config = ConfigProvider.getConfig(UIComponentsConfig.class);
            String root = config.getResourcesRoot();
            if (root != null) {
                if (!root.endsWith(File.separator)) {
                    root += File.separator;
                }
                file = new File(root + file.getName());
            }
        }

        resource = new FileResource(file, App.getInstance());
        component.setSource(resource);
    }

    public void setSource(String fileName, final InputStream src) {

        final StreamResource.StreamSource source = new StreamResource.StreamSource() {
            public InputStream getStream() {
                return src;
            }
        };

        try {
            resource = new StreamResource(
                    source,
                    URLEncoder.encode(fileName, "UTF-8"),
                    App.getInstance());
            component.setSource(resource);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSource(String fileName, ExportDataProvider dataProvider) {
        try {
            resource = new EmbeddedApplicationResource(
                    dataProvider,
                    URLEncoder.encode(fileName, "UTF-8"),
                    App.getInstance());
            component.setSource(resource);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMIMEType(String mt) {
        component.setMimeType(mt);
    }

    public void addParameter(String name, String value) {
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        }
        component.setParameter(name, value);
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        component.removeParameter(name);
        if (parameters != null) {
            parameters.remove(name);
        }
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void setType(Type t) {
        type = t;
        provideType();
    }

    public Type getType() {
        return type;
    }

    private void provideType() {
        switch (type) {
            case OBJECT:
                component.setType(com.vaadin.ui.Embedded.TYPE_OBJECT);
                break;
            case IMAGE:
                component.setType(com.vaadin.ui.Embedded.TYPE_IMAGE);
                break;
            case BROWSER:
                component.setType(com.vaadin.ui.Embedded.TYPE_BROWSER);
                break;
        }
    }

    public void dispose() {
        if (resource != null) {
            App.getInstance().removeResource(resource);
        }
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
