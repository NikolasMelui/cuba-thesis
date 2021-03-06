/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.data.ValueChangingListener;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.presentations.Presentations;
import org.dom4j.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Root of the GenericUI components hierarchy
 *
 * @author abramov
 */
public interface Component {

    enum Alignment {
        TOP_RIGHT,
        TOP_LEFT,
        TOP_CENTER,
        MIDDLE_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER
    }

    int UNITS_PIXELS = 0;
    int UNITS_PERCENTAGE = 8;

    String AUTO_SIZE = "-1px";

    /** Component ID as defined in <code>id</code> attribute */
    String getId();
    /** Set component ID */
    void setId(String id);

    /**
     * @return Parent of component.
     */
    Component getParent();
    /**
     * This method for internal use only. <br/>
     *
     * {@link Component.Container#add(Component)} is normally used for adding components
     * to a parent and the used method will call this method implicitly.
     *
     * @param parent Parent component
     */
    void setParent(Component parent);

    String getDebugId();
    /** For internal use only. Managed by debug Id system. */
    void setDebugId(String id);

    /** Is component in enabled state? */
    boolean isEnabled();
    /** Set component enabled state */
    void setEnabled(boolean enabled);

    /** Is component visible? */
    boolean isVisible();
    /** Set component visibility */
    void setVisible(boolean visible);

    /** Set focus to this component */
    void requestFocus();

    /** Get component height in {@link #getHeightUnits()} */
    float getHeight();

    /** Height units: {@link #UNITS_PIXELS}, {@link #UNITS_PERCENTAGE} */
    int getHeightUnits();

    /** Set component height in {@link #getHeightUnits()} */
    void setHeight(String height);

    /** Get component width in {@link #getWidthUnits()} */
    float getWidth();

    /** Width units: {@link #UNITS_PIXELS}, {@link #UNITS_PERCENTAGE} */
    int getWidthUnits();

    /** Set component width in {@link #getWidthUnits()} */
    void setWidth(String width);

    Alignment getAlignment();
    void setAlignment(Alignment alignment);

    /** Current style name. Styles implementation is client-type-specific */
    String getStyleName();
    /** Set style name. Styles implementation is client-type-specific */
    void setStyleName(String styleName);

    /**
     * Component which can contain other components
     */
    interface Container extends Component {
        void add(Component childComponent);
        void remove(Component childComponent);

        void removeAll();

        /**
         * Get component directly owned by this container.
         * @return component or null if not found
         */
        @Nullable
        <T extends Component> T getOwnComponent(String id);

        /**
         * Get component belonging to the whole components tree below this container.
         * @return component or null if not found
         */
        @Nullable
        <T extends Component> T getComponent(String id);

        /**
         * Get component belonging to the whole components tree below this container.
         *
         * @return component. Throws exception if not found.
         */
        @Nonnull
        <T extends Component> T getComponentNN(String id);

        /** Get all components directly owned by this container */
        Collection<Component> getOwnComponents();

        /** Get all components belonging to the whole components tree below this container */
        Collection<Component> getComponents();
    }

    interface OrderedContainer extends Container {
        void add(Component childComponent, int index);
        int indexOf(Component component);
    }

    interface HasNamedComponents {
        /**
         * Get subcomponent by name.
         * @return component or null if not found
         */
        @Nullable
        <T extends Component> T getComponent(String id);
    }

    /**
     * Component delegating work to some "wrapped" client-specific implementation
     */
    interface Wrapper extends Component {
        <T> T getComponent();
        Object getComposition();
    }

    /**
     * Component belonging to a frame
     */
    interface BelongToFrame extends Component {
        <A extends IFrame> A getFrame();
        void setFrame(IFrame frame);
    }

    /**
     * Object having a caption
     */
    interface HasCaption {
        String getCaption();
        void setCaption(String caption);

        String getDescription();
        void setDescription(String description);
    }

    /**
     * Object having a border
     */
    interface HasBorder {
        boolean isBorderVisible();
        void setBorderVisible(boolean borderVisible);
    }

    /**
     * Object having a value
     */
    interface HasValue extends Editable, BelongToFrame {
        <T> T getValue();

        void setValue(Object value);

        void addListener(ValueListener listener);
        void removeListener(ValueListener listener);

        /**
         * @deprecated Use normal {@link com.haulmont.cuba.gui.data.ValueListener} with setValue
         */
        @Deprecated
        void setValueChangingListener(ValueChangingListener listener);
        @Deprecated
        void removeValueChangingListener();
    }

    /**
     * Object having a formatter
     */
    interface HasFormatter {
        Formatter getFormatter();
        void setFormatter(Formatter formatter);
    }

    /**
     * Object having an XML descriptor attached
     */
    interface HasXmlDescriptor {
        Element getXmlDescriptor();
        void setXmlDescriptor(Element element);
    }

    /**
     * A component containing {@link Action}s
     */
    interface ActionsHolder extends Component {
        /**
         * Add an action to the component
         */
        void addAction(Action action);

        /**
         * Add an action to the component with index.
         */
        void addAction(Action action, int index);

        /**
         * Remove the action from the component
         */
        void removeAction(@Nullable Action action);

        /**
         * Remove the action by its ID. If there is no action with that ID, nothing happens.
         */
        void removeAction(@Nullable String id);

        /**
         * Remove all actions from the component
         */
        void removeAllActions();

        /**
         * @return unmodifiable collection of actions
         */
        Collection<Action> getActions();

        /**
         * @return an action by its ID, or null if not found
         */
        @Nullable
        Action getAction(String id);

        /**
         * @return an action by its ID
         * @throws java.lang.IllegalArgumentException if not found
         */
        @Nonnull
        Action getActionNN(String id);
    }

    interface SecuredActionsHolder extends ActionsHolder {

        ActionsPermissions getActionsPermissions();
    }

    /**
     * Component supporting "editable" state.
     * Editable means not read-only, so user can view a value but can not edit it. Not editable value can be copied to
     * clipboard.
     */
    interface Editable extends Component {
        boolean isEditable();
        void setEditable(boolean editable);
    }

    /**
     * Object supporting save/restore of user settings.
     * @see com.haulmont.cuba.security.app.UserSettingService
     */
    interface HasSettings {
        void applySettings(Element element);
        boolean saveSettings(Element element);

        boolean isSettingsEnabled();
        void setSettingsEnabled(boolean settingsEnabled);
    }

    /**
     * Is able to collapse (folding)
     */
    interface Collapsable {
        boolean isExpanded();
        void setExpanded(boolean expanded);

        boolean isCollapsable();
        void setCollapsable(boolean collapsable);

        void addListener(ExpandListener listener);
        void removeListener(ExpandListener listener);

        void addListener(CollapseListener listener);
        void removeListener(CollapseListener listener);

        interface ExpandListener {
            void onExpand(Collapsable component);
        }

        interface CollapseListener {
            void onCollapse(Collapsable component);
        }
    }

    interface Disposable {
        void dispose();
        boolean isDisposed();
    }

    /**
     * Component supporting an action
     */
    interface ActionOwner {
        Action getAction();
        void setAction(Action action);
    }

    /**
     * Component having an icon
     */
    interface HasIcon {
        String getIcon();
        void setIcon(String icon);
    }

    interface HasButtonsPanel {
        ButtonsPanel getButtonsPanel();
        void setButtonsPanel(ButtonsPanel panel);
    }

    /**
     * A component which can be validated
     */
    interface Validatable {
        boolean isValid();
        void validate() throws ValidationException;
    }

    interface HasPresentations extends HasSettings {
        void usePresentations(boolean b);
        boolean isUsePresentations();

        void resetPresentation();
        void loadPresentations();

        Presentations getPresentations();

        void applyPresentation(Object id);
        void applyPresentationAsDefault(Object id);

        Object getDefaultPresentationId();
    }

    interface Spacing {
        void setSpacing(boolean enabled);
    }

    interface Margin {
        void setMargin(boolean enable);
        void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable);
    }

    interface HasInputPrompt {
        /**
         * @return current input prompt.
         */
        String getInputPrompt();

        /**
         * Sets the input prompt - a textual prompt that is displayed when the field
         * would otherwise be empty, to prompt the user for input.
         *
         * @param inputPrompt input prompt
         */
        void setInputPrompt(String inputPrompt);
    }

    interface HasDropZone {
        /**
         * @return current drop zone
         */
        DropZone getDropZone();
        /**
         * Set drop zone reference to this upload component. Files can be dropped to component of the drop zone
         * to be uploaded by this upload component.
         *
         * @param dropZone drop zone descriptor
         */
        void setDropZone(DropZone dropZone);

        /**
         * @return current drop zone prompt
         */
        String getDropZonePrompt();
        /**
         * Set drop zone prompt that will be shown on drag over window with file.
         *
         * @param dropZonePrompt drop zone prompt
         */
        void setDropZonePrompt(String dropZonePrompt);

        /**
         * Drop zone descriptor. BoxLayout or Window can be used as drop zone for an upload component.
         */
        class DropZone {
            protected BoxLayout layout;

            protected Window window;

            public DropZone(BoxLayout targetLayout) {
                this.layout = targetLayout;
            }

            public DropZone(Window window) {
                this.window = window;
            }

            public BoxLayout getTargetLayout() {
                return layout;
            }

            public Window getTargetWindow() {
                return window;
            }

            public Component getTarget() {
                if (window != null) {
                    return window;
                } else {
                    return layout;
                }
            }
        }
    }
}