/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.textfield;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VTextField;
import com.vaadin.shared.EventId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaMaskedFieldWidget extends VTextField {

    public static final String CLASSNAME = "cuba-maskedfield";

    protected static final String EMPTY_FIELD_CLASS = "cuba-maskedfield-empty";

    protected static final char PLACE_HOLDER = '_';

    protected StringBuilder valueBuilder;

    protected String nullRepresentation;

    protected String mask;

    protected List<Mask> maskTest;

    protected Map<Character, Mask> maskMap = new HashMap<Character, Mask>();

    protected boolean maskedMode = false;

    protected MaskedKeyHandler keyHandler;

    protected int tabIndex = 0;

    protected boolean focused = false;

    public CubaMaskedFieldWidget() {
        setStylePrimaryName(CLASSNAME);
        setStyleName(CLASSNAME);
        valueBeforeEdit = "";

        initMaskMap();

        keyHandler = new MaskedKeyHandler();
        addKeyPressHandler(keyHandler);
        addKeyDownHandler(keyHandler);
        addKeyUpHandler(keyHandler);

        addInputHandler(getElement());
    }

    @Override
    public void onFocus(FocusEvent event) {
        super.onFocus(event);

        if (!this.focused) {
            this.focused = true;

            if (!isReadOnly() && isEnabled()) {
                if (mask != null && nullRepresentation != null && nullRepresentation.equals(super.getText())) {
                    addStyleName("cuba-focus-move");

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            if (!isReadOnly() && isEnabled() && focused) {
                                setSelectionRange(getPreviousPos(0), 0);
                            }

                            removeStyleName("cuba-focus-move");
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onBlur(BlurEvent event) {
        super.onBlur(event);

        this.focused = false;
    }

    public boolean isMaskedMode() {
        return maskedMode;
    }

    public void setMaskedMode(boolean maskedMode) {
        this.maskedMode = maskedMode;
    }

    protected void initMaskMap() {
        maskMap.put('#', new NumericMask());
        maskMap.put('U', new UpperCaseMask());
        maskMap.put('L', new LowerCaseMask());
        maskMap.put('?', new LetterMask());
        maskMap.put('A', new AlphanumericMask());
        maskMap.put('*', new WildcardMask());
        maskMap.put('H', new UpperCaseHexMask());
        maskMap.put('h', new LowerCaseHexMask());
        maskMap.put('~', new SignMask());
    }

    protected void updateCursor(int pos) {
        setCursorPos(getNextPos(pos));
    }

    protected int getNextPos(int pos) {
        while (++pos < maskTest.size() && maskTest.get(pos) == null) {
        }
        if (pos >= maskTest.size())
            return getPreviousPos(maskTest.size()) + 1;
        return pos;
    }

    int getPreviousPos(int pos) {
        while (--pos >= 0 && maskTest.get(pos) == null) {
        }
        if (pos < 0)
            return getNextPos(pos);
        return pos;
    }

    @Override
    public void setTabIndex(int index) {
        if (!isReadOnly()) {
            super.setTabIndex(index);
        }

        this.tabIndex = index;
    }

    @Override
    public int getTabIndex() {
        return tabIndex;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);

        if (readOnly) {
            super.setTabIndex(-1);
        } else {
            super.setTabIndex(tabIndex);
        }
    }

    @Override
    public void setText(String value) {
        valueBuilder = maskValue(value);
        if (valueBuilder.toString().equals(nullRepresentation) || valueBuilder.length() == 0) {
            getElement().addClassName(EMPTY_FIELD_CLASS);
        } else {
            getElement().removeClassName(EMPTY_FIELD_CLASS);
        }
        super.setText(valueBuilder.toString());
    }

    public String getRawText() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maskTest.size(); i++) {
            Mask mask = maskTest.get(i);
            if (mask != null) {
                if (valueBuilder.charAt(i) != PLACE_HOLDER) {
                    result.append(valueBuilder.charAt(i));
                } else result.append("");
            }
        }
        return result.toString();
    }

    protected StringBuilder maskValue(String value) {
        if (maskTest == null) {
            return new StringBuilder(value);
        } else {
            return maskValue(value, 0, maskTest.size());
        }
    }

    protected StringBuilder maskValue(String value, int start, int end) {
        StringBuilder result = new StringBuilder();
        if (maskTest == null) {
            return result.append(value);
        }
        if (value.equals(nullRepresentation.substring(start, end))) {
            result.append(value);
            return result;
        }
        int valuePos = 0;
        for (int i = start; i < maskTest.size() && valuePos < end - start; i++) {
            if (valuePos >= value.length()) {
                result.append(nullRepresentation.charAt(i));
                valuePos++;
            } else {
                Mask mask = maskTest.get(i);
                if (mask == null) {
                    if (nullRepresentation.charAt(i) == value.charAt(valuePos)) {
                        result.append(value.charAt(valuePos));
                        valuePos++;
                    } else {
                        result.append(nullRepresentation.charAt(i));
                    }
                } else {
                    if (mask.isValid(value.charAt(valuePos))) {
                        result.append(mask.getChar(value.charAt(valuePos)));
                        valuePos++;
                    } else {
                        result.append(nullRepresentation.charAt(i));
                        valuePos++;
                    }
                }
            }
        }
        return result;
    }

    public void setMask(String mask) {
        if (mask.equals(this.mask)) {
            return;
        }

        this.mask = mask;
        valueBuilder = new StringBuilder();
        maskTest = new ArrayList<Mask>();

        for (int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            if (c == '\'') {
                maskTest.add(null);
                valueBuilder.append(mask.charAt(++i));
            } else if (maskMap.get(c) != null) {
                maskTest.add(maskMap.get(c));
                valueBuilder.append(PLACE_HOLDER);
            } else {
                maskTest.add(null);
                valueBuilder.append(c);
            }
        }
        nullRepresentation = valueBuilder.toString();
        setText(valueBuilder.toString());
//		updateCursor(0); // KK: commented out because leads to grab focus
    }

    protected boolean validateText(String text) {
        if (text.equals(nullRepresentation)) {
            return true;
        }
        for (int i = 0; i < maskTest.size(); i++) {
            Mask mask = maskTest.get(i);
            if (mask != null) {
                if (!mask.isValid(text.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void valueChange(boolean blurred) {
        if (client != null && paintableId != null) {

            boolean sendBlurEvent = false;
            boolean sendValueChange = false;

            if (blurred && client.hasEventListeners(this, EventId.BLUR)) {
                sendBlurEvent = true;
                client.updateVariable(paintableId, EventId.BLUR, "", false);
            }

            String newText = getText();

            if (!prompting && newText != null
                    && !newText.equals(valueBeforeEdit)) {
                if (validateText(newText)) {
                    sendValueChange = immediate;
                    client.updateVariable(paintableId, "text", maskedMode ? getText() : getRawText(), false);
                    valueBeforeEdit = newText;
                } else {
                    setText(valueBeforeEdit);
                }
            }

            if (sendBlurEvent || sendValueChange) {
                client.sendPendingVariableChanges();
            }
        }
    }

    public void updateTextState() {
        if (valueBeforeEdit == null || !getText().equals(valueBeforeEdit)) {
            valueBeforeEdit = getText();
        }
    }

    protected native void addInputHandler(Element elementID)/*-{
        var temp = this;  // hack to hold on to 'this' reference

        var listener = function (e) {
            temp.@com.haulmont.cuba.web.toolkit.ui.client.textfield.CubaMaskedFieldWidget::handleInput()();
        };

        if (elementID.addEventListener) {
            elementID.addEventListener("input", listener, false);
        } else {
            elementID.attachEvent("input", listener);
        }
    }-*/;

    public void handleInput() {
        String newText = getText();
        if (newText.length() < valueBuilder.length()) {
            super.setText(valueBuilder.toString());
            setCursorPos(getNextPos(valueBuilder.length()));
        } else {
            int pasteLength = newText.length() - valueBuilder.length();
            int pasteStart;
            if (BrowserInfo.get().isGecko() || BrowserInfo.get().isIE()) {
                pasteStart = getCursorPos() - pasteLength;
            } else {
                pasteStart = getCursorPos();
            }

            StringBuilder maskedPart = maskValue(newText.substring(pasteStart, pasteStart + pasteLength), pasteStart, pasteStart + pasteLength);
            valueBuilder.replace(pasteStart, pasteStart + maskedPart.length(), maskedPart.toString());
            super.setText(valueBuilder.toString());
            setCursorPos(getNextPos(pasteStart + maskedPart.length()));
        }
    }

    protected void setRawCursorPosition(int pos) {
        if (pos >= 0 && pos <= maskTest.size())
            setCursorPos(pos);
    }

    public interface Mask {
        boolean isValid(char c);

        char getChar(char c);
    }

    public static abstract class AbstractMask implements Mask {
        @Override
        public char getChar(char c) {
            return c;
        }
    }

    public static class NumericMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            return Character.isDigit(c);
        }
    }

    public static class LetterMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            //it's not  proper way to recognize letters
            return (Character.toLowerCase(c) != Character.toUpperCase(c));
        }
    }

    /**
     * Represents a hex character, 0-9a-fA-F. a-f is mapped to A-F
     */
    public static class HexMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            return ((c == '0' || c == '1' ||
                    c == '2' || c == '3' ||
                    c == '4' || c == '5' ||
                    c == '6' || c == '7' ||
                    c == '8' || c == '9' ||
                    c == 'a' || c == 'A' ||
                    c == 'b' || c == 'B' ||
                    c == 'c' || c == 'C' ||
                    c == 'd' || c == 'D' ||
                    c == 'e' || c == 'E' ||
                    c == 'f' || c == 'F'));
        }
    }

    public static class LowerCaseHexMask extends HexMask {
        @Override
        public char getChar(char c) {
            if (Character.isDigit(c)) {
                return c;
            }
            return Character.toLowerCase(c);
        }
    }

    public static class UpperCaseHexMask extends HexMask {
        @Override
        public char getChar(char c) {
            if (Character.isDigit(c)) {
                return c;
            }
            return Character.toUpperCase(c);
        }
    }

    public static class LowerCaseMask extends LetterMask {
        @Override
        public boolean isValid(char c) {
            return Character.isLetter(getChar(c));
        }

        @Override
        public char getChar(char c) {
            return Character.toLowerCase(c);
        }
    }

    public static class UpperCaseMask extends LetterMask {
        @Override
        public boolean isValid(char c) {
            return Character.isLetter(getChar(c));
        }

        @Override
        public char getChar(char c) {
            return Character.toUpperCase(c);
        }
    }

    public static class AlphanumericMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            return (Character.isDigit(c) || (Character.toLowerCase(c) != Character.toUpperCase(c)));
        }
    }

    public static class WildcardMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            return true;
        }
    }

    public static class SignMask extends AbstractMask {
        @Override
        public boolean isValid(char c) {
            return c == '-' || c == '+';
        }
    }

    public class MaskedKeyHandler implements KeyDownHandler, KeyUpHandler, KeyPressHandler {

        protected boolean shiftPressed = false;
        protected int shiftPressPos = -1;

        protected int getCursorPosSelection() {
            return getCursorPos() + getSelectionLength();
        }

        @Override
        public void onKeyDown(KeyDownEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_SHIFT && !shiftPressed) {
                shiftPressed = true;
                shiftPressPos = getCursorPos();
            }
            if (isReadOnly())
                return;
            if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE) {
                int pos = getPreviousPos(getCursorPos());

                if (pos < maskTest.size()) {
                    Mask m = maskTest.get(pos);
                    if (m != null) {
                        valueBuilder.setCharAt(pos, PLACE_HOLDER);
                        setValue(valueBuilder.toString());
                    }
                    setCursorPos(pos);
                }
                event.preventDefault();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_DELETE) {
                int pos = getCursorPos();

                if (pos < maskTest.size()) {
                    Mask m = maskTest.get(pos);
                    if (m != null) {
                        valueBuilder.setCharAt(pos, PLACE_HOLDER);
                        setValue(valueBuilder.toString());
                    }
                    updateCursor(pos);
                }
                event.preventDefault();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_RIGHT) {
                if (getCursorPosSelection() <= shiftPressPos) {
                    setCursorPos(getNextPos(getCursorPos()));
                } else {
                    setCursorPos(getNextPos(getCursorPosSelection()));
                }
                updateSelectionRange();
                event.preventDefault();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_LEFT) {
                if (getCursorPosSelection() <= shiftPressPos) {
                    setCursorPos(getPreviousPos(getCursorPos()));
                } else {
                    setCursorPos(getPreviousPos(getCursorPosSelection()));
                }
                updateSelectionRange();
                event.preventDefault();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_HOME || event.getNativeKeyCode() == KeyCodes.KEY_UP) {
                setCursorPos(getPreviousPos(0));
                updateSelectionRange();
                event.preventDefault();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_END || event.getNativeKeyCode() == KeyCodes.KEY_DOWN) {
                setCursorPos(getPreviousPos(getValue().length()) + 1);
                updateSelectionRange();
                event.preventDefault();
            }
        }

        protected void updateSelectionRange() {
            if (shiftPressed) {
                if (getCursorPosSelection() > shiftPressPos) {
                    setSelectionRange(shiftPressPos, getCursorPosSelection() - shiftPressPos);
                } else {
                    setSelectionRange(getCursorPosSelection(), shiftPressPos - getCursorPosSelection());
                }
            }
        }

        @Override
        public void onKeyUp(KeyUpEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_SHIFT) {
                shiftPressed = false;
                shiftPressPos = -1;
            }
        }

        @Override
        public void onKeyPress(KeyPressEvent e) {
            if (isReadOnly())
                return;

            if (e.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
                    && !e.getNativeEvent().getAltKey()
                    && !e.getNativeEvent().getCtrlKey()
                    && !e.getNativeEvent().getShiftKey()) {
                valueChange(false);
            }

            if (e.getNativeEvent().getKeyCode() == KeyCodes.KEY_BACKSPACE
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_DELETE
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_END
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_HOME
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_LEFT
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_PAGEDOWN
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_PAGEUP
                    || e.getNativeEvent().getKeyCode() == KeyCodes.KEY_RIGHT
                    || e.getNativeEvent().getAltKey()
                    //|| e.getNativeEvent().getCtrlKey()
                    || e.getNativeEvent().getMetaKey()) {

                e.preventDefault(); // KK: otherwise incorrectly handles combinations like Shift+'='
                return;
            } else if (BrowserInfo.get().isGecko() && e.getCharCode() == '\u0000' || e.getNativeEvent().getCtrlKey()) {
                //pressed tab in firefox or ctrl. because FF fires keyPressEvents on CTRL+C
                return;
            }
            if (getCursorPos() < maskTest.size()) {
                Mask m = maskTest.get(getCursorPos());
                if (m != null) {
                    if (m.isValid(e.getCharCode())) {
                        int pos = getCursorPos();
                        valueBuilder.setCharAt(pos, m.getChar(e.getCharCode()));
                        setValue(valueBuilder.toString());
                        updateCursor(pos);
                    }
                } else {
                    updateCursor(getCursorPos());
                }
            }
            e.preventDefault();
        }
    }
}