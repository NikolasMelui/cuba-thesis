/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.dev;

import com.haulmont.cuba.gui.ComponentVisitor;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.haulmont.cuba.gui.dev.LayoutTip.error;
import static com.haulmont.cuba.gui.dev.LayoutTip.warn;

/**
 * @author artamonov
 * @version $Id$
 * @since 5.2.0
 */
public class LayoutAnalyzer {

    protected List<Inspection> inspections = new ArrayList<>();
    {
        inspections.add(new ComponentUndefinedSize());
        inspections.add(new ScrollBoxInnerComponentRelativeSize());
        inspections.add(new ComponentRelativeSizeInsideUndefinedSizedContainer());
        inspections.add(new AlignInsideUndefinedSizedContainer());
    }

    public List<LayoutTip> analyze(Window window) {
        final List<LayoutTip> errors = new ArrayList<>();

        for (Inspection inspection : inspections) {
            errors.addAll(inspection.analyze(window, "window"));
        }

        ComponentsHelper.walkComponents(window, new ComponentVisitor() {
            @Override
            public void visit(Component component, String name) {
                for (Inspection inspection : inspections) {
                    errors.addAll(inspection.analyze(component, name));
                }
            }
        });

        return errors;
    }

    public interface Inspection {
        List<LayoutTip> analyze(Component component, String path);
    }

    public static class ComponentUndefinedSize implements Inspection {

        @Override
        public List<LayoutTip> analyze(Component c, String path) {

            if (c instanceof Table
                    || c instanceof Tree
                    || c instanceof ScrollBoxLayout) {
                String componentClass = c.getClass().getSimpleName();

                if (c.getWidth() < 0 && c.getHeight() < 0) {
                    return Collections.singletonList(warn("Component '" + path + "'", componentClass + " should not have undefined size"));
                } else if (c.getWidth() < 0) {
                    return Collections.singletonList(warn("Component '" + path + "'", componentClass + " should not have undefined width"));
                } else if (c.getHeight() < 0) {
                    return Collections.singletonList(warn("Component '" + path + "'", componentClass + " should not have undefined height"));
                }
            }

            return Collections.emptyList();
        }
    }

    public static class ScrollBoxInnerComponentRelativeSize implements Inspection {

        @Override
        public List<LayoutTip> analyze(Component c, String path) {
            if (c instanceof ScrollBoxLayout) {
                List<LayoutTip> tips = null;

                ScrollBoxLayout scrollBox = (ScrollBoxLayout) c;

                if (scrollBox.getOrientation() == ScrollBoxLayout.Orientation.HORIZONTAL) {
                    for (Component component : scrollBox.getOwnComponents()) {
                        if (component.getWidth() > 0 && component.getWidthUnits() == Component.UNITS_PERCENTAGE) {
                            if (tips == null) {
                                tips = new ArrayList<>();
                            }
                            String id = component.getId() != null ? component.getId() : component.getClass().getSimpleName();
                            tips.add(error("Scrollbox '" + path + "', nested component '" + id + "'",
                                    "ScrollBox has HORIZONTAL orientation, nested component should not have relative width %s%%",
                                    component.getWidth()));
                        }
                    }
                } else {
                    for (Component component : scrollBox.getOwnComponents()) {
                        if (component.getHeight() > 0 && component.getHeightUnits() == Component.UNITS_PERCENTAGE) {
                            if (tips == null) {
                                tips = new ArrayList<>();
                            }
                            String id = component.getId() != null ? component.getId() : component.getClass().getSimpleName();
                            tips.add(error("Scrollbox '" + path + "', nested component '" + id + "'",
                                    "ScrollBox has VERTICAL orientation, nested component should not have relative height %s%%",
                                    component.getHeight()));
                        }
                    }
                }

                return tips != null ? tips : Collections.<LayoutTip>emptyList();
            }

            return Collections.emptyList();
        }
    }

    public static class ComponentRelativeSizeInsideUndefinedSizedContainer implements Inspection {

        @Override
        public List<LayoutTip> analyze(Component c, String path) {
            if (c instanceof Component.Container) {
                List<LayoutTip> tips = null;

                Component.Container container = (Component.Container) c;
                if (c.getWidth() < 0) {
                    for (Component component : container.getOwnComponents()) {
                        if (component.getWidthUnits() == Component.UNITS_PERCENTAGE && component.getWidth() > 0) {
                            if (tips == null) {
                                tips = new ArrayList<>();
                            }
                            String id = component.getId() != null ? component.getId() : component.getClass().getSimpleName();
                            tips.add(error("Container '" + path + "', nested component '" + id + "'",
                                    "Nested component has relative width %s%% inside container with undefined width",
                                    component.getWidth()));
                        }
                    }
                }

                if (c.getHeight() < 0) {
                    for (Component component : container.getOwnComponents()) {
                        if (component.getHeightUnits() == Component.UNITS_PERCENTAGE && component.getHeight() > 0) {
                            if (tips == null) {
                                tips = new ArrayList<>();
                            }
                            String id = component.getId() != null ? component.getId() : component.getClass().getSimpleName();
                            tips.add(error("Container '" + path + "', nested component '" + id + "'",
                                    "Nested component has relative height %s%% inside container with undefined height",
                                    component.getHeight()));
                        }
                    }
                }

                return tips != null ? tips : Collections.<LayoutTip>emptyList();
            }
            return Collections.emptyList();
        }
    }

    public static class AlignInsideUndefinedSizedContainer implements Inspection {

        @Override
        public List<LayoutTip> analyze(Component c, String path) {
            if (c instanceof Component.Container) {
                if (c.getWidth() < 0 && c.getHeight() < 0) {
                    List<LayoutTip> tips = null;

                    Component.Container container = (Component.Container) c;
                    for (Component component : container.getOwnComponents()) {
                        if (tips == null) {
                            tips = new ArrayList<>();
                        }
                        if (component.getAlignment() != Component.Alignment.TOP_LEFT) {
                            String id = component.getId() != null ? component.getId() : component.getClass().getSimpleName();
                            tips.add(warn("Container '" + path + "', nested component '" + id + "'",
                                    "Nested component has align %s inside container with undefined size",
                                    component.getAlignment()));
                        }
                    }

                    return tips != null ? tips : Collections.<LayoutTip>emptyList();
                }
            }
            return Collections.emptyList();
        }
    }
}