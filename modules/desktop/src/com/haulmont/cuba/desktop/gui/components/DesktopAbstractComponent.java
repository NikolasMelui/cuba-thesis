/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Range;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.gui.data.ComponentSize;
import com.haulmont.cuba.desktop.theme.DesktopTheme;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.gui.components.IFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import javax.swing.*;
import java.util.Locale;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class DesktopAbstractComponent<C extends JComponent>
        implements
        DesktopComponent, Component.Wrapper, Component.HasXmlDescriptor, Component.BelongToFrame {

    protected C impl;

    protected DesktopContainer container;

    protected String id;
    protected IFrame frame;
    protected Element xmlDescriptor;

    protected ComponentSize widthSize, heightSize;
    protected Alignment alignment;

    protected Log log = LogFactory.getLog(getClass());

    protected final String swingPropertyId = "cubaId";
    public static final String swingPropertyClass = "cubaClass";

    protected C getImpl() {
        return impl;
    }

    protected String getSwingPropertyId() {
        return swingPropertyId;
    }

    @Override
    public <A extends IFrame> A getFrame() {
        return (A) frame;
    }

    @Override
    public void setFrame(IFrame frame) {
        this.frame = frame;
        frame.registerComponent(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
        if (this.getImpl() != null) {
            impl.putClientProperty(getSwingPropertyId(), id);
        }
    }

    protected void assignClassDebugProperty(JComponent c) {
        c.putClientProperty(swingPropertyClass, getClass().getSimpleName());
    }

    @Override
    public String getDebugId() {
        return null;
    }

    @Override
    public void setDebugId(String id) {
    }

    @Override
    public boolean isEnabled() {
        return impl.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getComposition().setEnabled(enabled);

        requestContainerUpdate();
    }

    @Override
    public boolean isVisible() {
        boolean componentVisible = getComposition() != null && getComposition().isVisible();

        if (container != null)
            return componentVisible && container.isVisible();
        else
            return componentVisible;
    }

    @Override
    public void setVisible(boolean visible) {
        getComposition().setVisible(visible);
        requestContainerUpdate();
    }

    @Override
    public void requestFocus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                impl.requestFocus();
            }
        });
    }

    @Override
    public float getHeight() {
        return heightSize != null ? heightSize.value : 0.0f;
    }

    @Override
    public int getHeightUnits() {
        return heightSize != null ? heightSize.unit : 0;
    }

    @Override
    public void setHeight(String height) {
        heightSize = ComponentSize.parse(height);
        requestContainerUpdate();
    }

    @Override
    public float getWidth() {
        return widthSize != null ? widthSize.value : 0.0f;
    }

    @Override
    public int getWidthUnits() {
        return widthSize != null ? widthSize.unit : 0;
    }

    @Override
    public void setWidth(String width) {
        widthSize = ComponentSize.parse(width);
        requestContainerUpdate();
    }

    @Override
    public Alignment getAlignment() {
        return alignment;
    }

    @Override
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        requestContainerUpdate();
    }

    @Override
    public String getStyleName() {
        return null;
    }

    @Override
    public void setStyleName(String name) {
        DesktopTheme theme = App.getInstance().getTheme();
        if (theme != null) {
            theme.applyStyle(this, name);
        }
    }

    @Override
    public Element getXmlDescriptor() {
        return xmlDescriptor;
    }

    @Override
    public void setXmlDescriptor(Element element) {
        xmlDescriptor = element;
    }

    @Override
    public <T> T getComponent() {
        return (T) impl;
    }

    @Override
    public JComponent getComposition() {
        return impl;
    }

    @Override
    public void setContainer(DesktopContainer container) {
        this.container = container;
    }

    protected void requestContainerUpdate() {
        if (container != null) {
            container.updateComponent(this);
        }
    }

    @Override
    public void setExpanded(boolean expanded) {
    }

    /**
     * Default formatter for {@link DesktopLabel} and {@link DesktopTextField}
     */
    protected static class DefaultValueFormatter {

        private MetaProperty metaProperty;
        private Formatter formatter;
        private Datatype datatype;
        private Locale locale;

        public DefaultValueFormatter(Locale locale) {
            this.locale = locale;
        }

        public Formatter getFormatter() {
            return formatter;
        }

        public void setFormatter(Formatter formatter) {
            this.formatter = formatter;
        }

        public Datatype getDatatype() {
            return datatype;
        }

        public void setDatatype(Datatype datatype) {
            this.datatype = datatype;
        }

        public MetaProperty getMetaProperty() {
            return metaProperty;
        }

        public void setMetaProperty(MetaProperty metaProperty) {
            this.metaProperty = metaProperty;
        }

        /**
         * Format value for text field or label
         *
         * @param value Object value
         * @return Formatted string
         */
        public String formatValue(Object value) {
            String text = "";
            if (formatter == null) {
                if (value == null) {
                    text = "";
                } else {
                    if (metaProperty != null) {
                        Range range = metaProperty.getRange();
                        if (range.isDatatype()) {
                            text = range.asDatatype().format(value, locale);
                        } else if (range.isEnum()) {
                            text = MessageProvider.getMessage((Enum) value);
                        } else if (range.isClass()) {
                            text = InstanceUtils.getInstanceName((Instance) value);
                        } else
                            text = String.valueOf(value);
                    } else if (datatype != null) {
                        text = datatype.format(value, locale);
                    } else
                        text = String.valueOf(value);
                }
            } else {
                text = formatter.format(value);
            }
            return text;
        }
    }
}
