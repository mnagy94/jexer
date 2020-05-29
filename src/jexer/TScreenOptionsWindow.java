/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import jexer.backend.ECMA48Terminal;
import jexer.backend.SwingTerminal;
import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import static jexer.TKeypress.*;

/**
 * TScreenOptionsWindow provides an easy UI for users to alter the running
 * screen options such as fonts and images.
 */
public class TScreenOptionsWindow extends TWindow {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(TScreenOptionsWindow.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The Swing screen.
     */
    private SwingTerminal terminal = null;

    /**
     * The ECMA48 screen.
     */
    private ECMA48Terminal ecmaTerminal = null;

    /**
     * The font name.
     */
    private TComboBox fontName;

    /**
     * The font size.
     */
    private TField fontSize;

    /**
     * The X text adjustment.
     */
    private TField textAdjustX;

    /**
     * The Y text adjustment.
     */
    private TField textAdjustY;

    /**
     * The height text adjustment.
     */
    private TField textAdjustHeight;

    /**
     * The width text adjustment.
     */
    private TField textAdjustWidth;

    /**
     * The sixel palette size.
     */
    private TComboBox sixelPaletteSize;

    /**
     * The wideCharImages option.
     */
    private TCheckBox wideCharImages;

    /**
     * Triple-buffer support.
     */
    private TCheckBox tripleBuffer;

    /**
     * Cursor style.
     */
    private TComboBox cursorStyle;

    /**
     * Mouse style.
     */
    private TComboBox mouseStyle;

    /**
     * Sixel support.
     */
    private TCheckBox sixel;

    /**
     * Whether or not sixel uses a single shared palette.
     */
    private TCheckBox sixelSharedPalette;

    /**
     * 24-bit RGB color for normal system colors.
     */
    private TCheckBox rgbColor;

    /**
     * The original font size.
     */
    private int oldFontSize = 20;

    /**
     * The original font.
     */
    private Font oldFont = null;

    /**
     * The original text adjust X value.
     */
    private int oldTextAdjustX = 0;

    /**
     * The original text adjust Y value.
     */
    private int oldTextAdjustY = 0;

    /**
     * The original text adjust height value.
     */
    private int oldTextAdjustHeight = 0;

    /**
     * The original text adjust width value.
     */
    private int oldTextAdjustWidth = 0;

    /**
     * The original sixel palette (number of colors) value.
     */
    private int oldSixelPaletteSize = 1024;

    /**
     * The original wideCharImages value.
     */
    private boolean oldWideCharImages = true;

    /**
     * The original triple-buffer support.
     */
    private boolean oldTripleBuffer = true;

    /**
     * The original cursor style.
     */
    private SwingTerminal.CursorStyle oldCursorStyle;

    /**
     * The original mouse style.
     */
    private String oldMouseStyle = "default";

    /**
     * The original sixel support.
     */
    private boolean oldSixel = true;

    /**
     * The original sixelSharedPalette value.
     */
    private boolean oldSixelSharedPalette = true;

    /**
     * The original 24-bit RGB color for normal system colors.
     */
    private boolean oldRgbColor = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be centered on screen.
     *
     * @param application the TApplication that manages this window
     */
    public TScreenOptionsWindow(final TApplication application) {

        // Register with the TApplication
        super(application, i18n.getString("windowTitle"), 0, 0, 60, 23, MODAL);

        // Add shortcut text
        newStatusBar(i18n.getString("statusBar"));

        if (getScreen() instanceof SwingTerminal) {
            terminal = (SwingTerminal) getScreen();
        }
        if (getScreen() instanceof ECMA48Terminal) {
            ecmaTerminal = (ECMA48Terminal) getScreen();
        }

        addLabel(i18n.getString("fontName"), 3, 2, "ttext", false,
            new TAction() {
                public void DO() {
                    if (fontName != null) {
                        fontName.activate();
                    }
                }
            });
        addLabel(i18n.getString("fontSize"), 3, 3, "ttext", false,
            new TAction() {
                public void DO() {
                    if (fontSize != null) {
                        fontSize.activate();
                    }
                }
            });
        addLabel(i18n.getString("textAdjustX"), 3, 4, "ttext", false,
            new TAction() {
                public void DO() {
                    if (textAdjustX != null) {
                        textAdjustX.activate();
                    }
                }
            });
        addLabel(i18n.getString("textAdjustY"), 3, 5, "ttext", false,
            new TAction() {
                public void DO() {
                    if (textAdjustY != null) {
                        textAdjustY.activate();
                    }
                }
            });
        addLabel(i18n.getString("textAdjustHeight"), 3, 6, "ttext", false,
            new TAction() {
                public void DO() {
                    if (textAdjustHeight != null) {
                        textAdjustHeight.activate();
                    }
                }
            });
        addLabel(i18n.getString("textAdjustWidth"), 3, 7, "ttext", false,
            new TAction() {
                public void DO() {
                    if (textAdjustWidth != null) {
                        textAdjustWidth.activate();
                    }
                }
            });
        addLabel(i18n.getString("cursorStyle"), 3, 10, "ttext", false,
            new TAction() {
                public void DO() {
                    if (cursorStyle != null) {
                        cursorStyle.activate();
                    }
                }
            });
        addLabel(i18n.getString("mouseStyle"), 3, 11, "ttext", false,
            new TAction() {
                public void DO() {
                    if (mouseStyle != null) {
                        mouseStyle.activate();
                    }
                }
            });

        sixel = addCheckBox(3, 15, i18n.getString("sixel"),
            (ecmaTerminal != null ? ecmaTerminal.hasSixel() :
                System.getProperty("jexer.ECMA48.sixel",
                    "true").equals("true")));
        oldSixel = sixel.isChecked();
        sixelSharedPalette = addCheckBox(3, 16,
            i18n.getString("sixelSharedPalette"),
            (ecmaTerminal != null ? ecmaTerminal.hasSixelSharedPalette() :
                System.getProperty("jexer.ECMA48.sixelSharedPalette",
                    "true").equals("true")));
        oldSixelSharedPalette = sixelSharedPalette.isChecked();
        addLabel(i18n.getString("sixelPaletteSize"), 3, 17, "ttext", false,
            new TAction() {
                public void DO() {
                    if (sixelPaletteSize != null) {
                        sixelPaletteSize.activate();
                    }
                }
            });
        wideCharImages = addCheckBox(3, 18, i18n.getString("wideCharImages"),
            (ecmaTerminal != null ? ecmaTerminal.isWideCharImages() :
                System.getProperty("jexer.ECMA48.wideCharImages",
                    "true").equals("true")));
        oldWideCharImages = wideCharImages.isChecked();
        rgbColor = addCheckBox(3, 19, i18n.getString("rgbColor"),
            (ecmaTerminal != null ? ecmaTerminal.isRgbColor() :
                System.getProperty("jexer.ECMA48.rgbColor",
                    "false").equals("true")));
        oldRgbColor = rgbColor.isChecked();

        int col = 23;
        if (terminal == null) {
            // Non-Swing case: turn off stuff we can't change
            addLabel(i18n.getString("unavailable"), col, 2);
            addLabel(i18n.getString("unavailable"), col, 3);
            addLabel(i18n.getString("unavailable"), col, 4);
            addLabel(i18n.getString("unavailable"), col, 5);
            addLabel(i18n.getString("unavailable"), col, 6);
            addLabel(i18n.getString("unavailable"), col, 7);
        }
        if (ecmaTerminal == null) {
            // Swing case: turn off stuff we can't change
            addLabel(i18n.getString("unavailable"), col, 17);
            sixel.setEnabled(false);
            sixelSharedPalette.setEnabled(false);
            wideCharImages.setEnabled(false);
            rgbColor.setEnabled(false);
        }
        if (ecmaTerminal != null) {
            oldSixelPaletteSize = ecmaTerminal.getSixelPaletteSize();

            String [] sixelSizes = { "2", "256", "512", "1024", "2048" };
            List<String> sizes = new ArrayList<String>();
            sizes.addAll(Arrays.asList(sixelSizes));
            sixelPaletteSize = addComboBox(col, 17, 10, sizes, 0, 6,
                new TAction() {
                    public void DO() {
                        try {
                            ecmaTerminal.setSixelPaletteSize(Integer.parseInt(
                                sixelPaletteSize.getText()));
                        } catch (NumberFormatException e) {
                            // SQUASH
                        }
                    }
                }
            );
            sixelPaletteSize.setText(Integer.toString(oldSixelPaletteSize));
        }

        if (terminal != null) {
            oldFont = terminal.getFont();
            oldFontSize = terminal.getFontSize();
            oldTextAdjustX = terminal.getTextAdjustX();
            oldTextAdjustY = terminal.getTextAdjustY();
            oldTextAdjustHeight = terminal.getTextAdjustHeight();
            oldTextAdjustWidth = terminal.getTextAdjustWidth();
            oldCursorStyle = terminal.getCursorStyle();
            oldMouseStyle = terminal.getMouseStyle();

            String [] fontNames = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            List<String> fonts = new ArrayList<String>();
            fonts.add(0, i18n.getString("builtInTerminus"));
            fonts.addAll(Arrays.asList(fontNames));
            fontName = addComboBox(col, 2, 25, fonts, 0, 8,
                new TAction() {
                    public void DO() {
                        if (fontName.getText().equals(i18n.
                                getString("builtInTerminus"))) {

                            terminal.setDefaultFont();
                        } else {
                            terminal.setFont(new Font(fontName.getText(),
                                    Font.PLAIN, terminal.getFontSize()));
                            fontSize.setText(Integer.toString(
                                terminal.getFontSize()));
                            textAdjustX.setText(Integer.toString(
                                terminal.getTextAdjustX()));
                            textAdjustY.setText(Integer.toString(
                                terminal.getTextAdjustY()));
                            textAdjustHeight.setText(Integer.toString(
                                terminal.getTextAdjustHeight()));
                            textAdjustWidth.setText(Integer.toString(
                                terminal.getTextAdjustWidth()));
                        }
                    }
                }
            );

            // Font size
            fontSize = addField(col, 3, 3, true,
                Integer.toString(terminal.getFontSize()),
                new TAction() {
                    public void DO() {
                        int currentSize = terminal.getFontSize();
                        int newSize = currentSize;
                        try {
                            newSize = Integer.parseInt(fontSize.getText());
                        } catch (NumberFormatException e) {
                            fontSize.setText(Integer.toString(currentSize));
                        }
                        if (newSize != currentSize) {
                            terminal.setFontSize(newSize);
                            textAdjustX.setText(Integer.toString(
                                terminal.getTextAdjustX()));
                            textAdjustY.setText(Integer.toString(
                                terminal.getTextAdjustY()));
                            textAdjustHeight.setText(Integer.toString(
                                terminal.getTextAdjustHeight()));
                            textAdjustWidth.setText(Integer.toString(
                                terminal.getTextAdjustWidth()));
                        }
                    }
                },
                null);

            addSpinner(col + 3, 3,
                new TAction() {
                    public void DO() {
                        int currentSize = terminal.getFontSize();
                        int newSize = currentSize;
                        try {
                            newSize = Integer.parseInt(fontSize.getText());
                            newSize++;
                        } catch (NumberFormatException e) {
                            fontSize.setText(Integer.toString(currentSize));
                        }
                        fontSize.setText(Integer.toString(newSize));
                        if (newSize != currentSize) {
                            terminal.setFontSize(newSize);
                            textAdjustX.setText(Integer.toString(
                                terminal.getTextAdjustX()));
                            textAdjustY.setText(Integer.toString(
                                terminal.getTextAdjustY()));
                            textAdjustHeight.setText(Integer.toString(
                                terminal.getTextAdjustHeight()));
                            textAdjustWidth.setText(Integer.toString(
                                terminal.getTextAdjustWidth()));
                        }
                    }
                },
                new TAction() {
                    public void DO() {
                        int currentSize = terminal.getFontSize();
                        int newSize = currentSize;
                        try {
                            newSize = Integer.parseInt(fontSize.getText());
                            newSize--;
                        } catch (NumberFormatException e) {
                            fontSize.setText(Integer.toString(currentSize));
                        }
                        fontSize.setText(Integer.toString(newSize));
                        if (newSize != currentSize) {
                            terminal.setFontSize(newSize);
                            textAdjustX.setText(Integer.toString(
                                terminal.getTextAdjustX()));
                            textAdjustY.setText(Integer.toString(
                                terminal.getTextAdjustY()));
                            textAdjustHeight.setText(Integer.toString(
                                terminal.getTextAdjustHeight()));
                            textAdjustWidth.setText(Integer.toString(
                                terminal.getTextAdjustWidth()));
                        }
                    }
                }
            );

            // textAdjustX
            textAdjustX = addField(col, 4, 3, true,
                Integer.toString(terminal.getTextAdjustX()),
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustX();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustX.getText());
                        } catch (NumberFormatException e) {
                            textAdjustX.setText(Integer.toString(currentAdjust));
                        }
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustX(newAdjust);
                        }
                    }
                },
                null);

            addSpinner(col + 3, 4,
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustX();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustX.getText());
                            newAdjust++;
                        } catch (NumberFormatException e) {
                            textAdjustX.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustX.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustX(newAdjust);
                        }
                    }
                },
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustX();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustX.getText());
                            newAdjust--;
                        } catch (NumberFormatException e) {
                            textAdjustX.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustX.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustX(newAdjust);
                        }
                    }
                }
            );

            // textAdjustY
            textAdjustY = addField(col, 5, 3, true,
                Integer.toString(terminal.getTextAdjustY()),
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustY();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustY.getText());
                        } catch (NumberFormatException e) {
                            textAdjustY.setText(Integer.toString(currentAdjust));
                        }
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustY(newAdjust);
                        }
                    }
                },
                null);

            addSpinner(col + 3, 5,
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustY();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustY.getText());
                            newAdjust++;
                        } catch (NumberFormatException e) {
                            textAdjustY.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustY.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustY(newAdjust);
                        }
                    }
                },
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustY();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustY.getText());
                            newAdjust--;
                        } catch (NumberFormatException e) {
                            textAdjustY.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustY.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustY(newAdjust);
                        }
                    }
                }
            );

            // textAdjustHeight
            textAdjustHeight = addField(col, 6, 3, true,
                Integer.toString(terminal.getTextAdjustHeight()),
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustHeight();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustHeight.getText());
                        } catch (NumberFormatException e) {
                            textAdjustHeight.setText(Integer.toString(currentAdjust));
                        }
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustHeight(newAdjust);
                        }
                    }
                },
                null);

            addSpinner(col + 3, 6,
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustHeight();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustHeight.getText());
                            newAdjust++;
                        } catch (NumberFormatException e) {
                            textAdjustHeight.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustHeight.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustHeight(newAdjust);
                        }
                    }
                },
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustHeight();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustHeight.getText());
                            newAdjust--;
                        } catch (NumberFormatException e) {
                            textAdjustHeight.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustHeight.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustHeight(newAdjust);
                        }
                    }
                }
            );

            // textAdjustWidth
            textAdjustWidth = addField(col, 7, 3, true,
                Integer.toString(terminal.getTextAdjustWidth()),
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustWidth();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustWidth.getText());
                        } catch (NumberFormatException e) {
                            textAdjustWidth.setText(Integer.toString(currentAdjust));
                        }
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustWidth(newAdjust);
                        }
                    }
                },
                null);

            addSpinner(col + 3, 7,
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustWidth();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustWidth.getText());
                            newAdjust++;
                        } catch (NumberFormatException e) {
                            textAdjustWidth.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustWidth.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustWidth(newAdjust);
                        }
                    }
                },
                new TAction() {
                    public void DO() {
                        int currentAdjust = terminal.getTextAdjustWidth();
                        int newAdjust = currentAdjust;
                        try {
                            newAdjust = Integer.parseInt(textAdjustWidth.getText());
                            newAdjust--;
                        } catch (NumberFormatException e) {
                            textAdjustWidth.setText(Integer.toString(currentAdjust));
                        }
                        textAdjustWidth.setText(Integer.toString(newAdjust));
                        if (newAdjust != currentAdjust) {
                            terminal.setTextAdjustWidth(newAdjust);
                        }
                    }
                }
            );

        } // if (terminal != null)

        tripleBuffer = addCheckBox(3, 9, i18n.getString("tripleBuffer"),
            (terminal != null ? terminal.isTripleBuffer() :
                System.getProperty("jexer.Swing.tripleBuffer",
                    "true").equals("true")));
        oldTripleBuffer = tripleBuffer.isChecked();

        ArrayList<String> cursorStyles = new ArrayList<String>();
        cursorStyles.add(i18n.getString("cursorStyleBlock").toLowerCase());
        cursorStyles.add(i18n.getString("cursorStyleOutline").toLowerCase());
        cursorStyles.add(i18n.getString("cursorStyleUnderline").toLowerCase());
        cursorStyle = addComboBox(22, 10, 25, cursorStyles, 0, 4,
            new TAction() {
                public void DO() {
                    terminal.setCursorStyle(cursorStyle.getText());
                }
            });
        cursorStyle.setText((terminal == null ?
                System.getProperty("jexer.Swing.cursorStyle", "underline") :
                terminal.getCursorStyle().toString().toLowerCase()));

        ArrayList<String> mouseStyles = new ArrayList<String>();
        mouseStyles.add("default");
        mouseStyles.add("crosshair");
        mouseStyles.add("hand");
        mouseStyles.add("move");
        mouseStyles.add("text");
        mouseStyles.add("none");
        mouseStyle = addComboBox(22, 11, 25, mouseStyles, 0, 7,
            new TAction() {
                public void DO() {
                    terminal.setMouseStyle(mouseStyle.getText());
                }
            });
        mouseStyle.setText((terminal == null ?
                System.getProperty("jexer.Swing.mouseStyle", "default") :
                terminal.getMouseStyle().toLowerCase()));

        if (terminal == null) {
            tripleBuffer.setEnabled(false);
            cursorStyle.setEnabled(false);
            mouseStyle.setEnabled(false);
        }

        addButton(i18n.getString("okButton"),
            getWidth() - 13, getHeight() - 7,
            new TAction() {
                public void DO() {
                    // Copy values out.
                    if (ecmaTerminal != null) {
                        ecmaTerminal.setHasSixel(sixel.isChecked());
                        ecmaTerminal.setSixelSharedPalette(sixelSharedPalette.
                            isChecked());
                        ecmaTerminal.setWideCharImages(wideCharImages.
                            isChecked());
                        ecmaTerminal.setRgbColor(rgbColor.isChecked());
                    }
                    if (terminal != null) {
                        synchronized (terminal) {
                            terminal.setTripleBuffer(tripleBuffer.isChecked());
                            terminal.setFont(terminal.getFont());
                        }
                    }

                    // Close window.
                    TScreenOptionsWindow.this.close();
                }
            });

        TButton cancelButton = addButton(i18n.getString("cancelButton"),
            getWidth() - 13, getHeight() - 5,
            new TAction() {
                public void DO() {
                    // Restore old values, then close the window.
                    if (terminal != null) {
                        synchronized (terminal) {
                            terminal.setFont(oldFont);
                            terminal.setFontSize(oldFontSize);
                            terminal.setTextAdjustX(oldTextAdjustX);
                            terminal.setTextAdjustY(oldTextAdjustY);
                            terminal.setTextAdjustHeight(oldTextAdjustHeight);
                            terminal.setTextAdjustWidth(oldTextAdjustWidth);
                            terminal.setTripleBuffer(oldTripleBuffer);
                            terminal.setCursorStyle(oldCursorStyle);
                            terminal.setMouseStyle(oldMouseStyle);
                        }
                    }
                    if (ecmaTerminal != null) {
                        ecmaTerminal.setHasSixel(oldSixel);
                        ecmaTerminal.setSixelSharedPalette(oldSixelSharedPalette);
                        ecmaTerminal.setSixelPaletteSize(oldSixelPaletteSize);
                        ecmaTerminal.setWideCharImages(oldWideCharImages);
                        ecmaTerminal.setRgbColor(oldRgbColor);
                    }
                    TScreenOptionsWindow.this.close();
                }
            });

        // Save this for last: make the cancel button default action.
        activate(cancelButton);

    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Escape - behave like cancel
        if (keypress.equals(kbEsc)) {
            // Restore old values, then close the window.
            if (terminal != null) {
                synchronized (terminal) {
                    terminal.setFont(oldFont);
                    terminal.setFontSize(oldFontSize);
                    terminal.setTextAdjustX(oldTextAdjustX);
                    terminal.setTextAdjustY(oldTextAdjustY);
                    terminal.setTextAdjustHeight(oldTextAdjustHeight);
                    terminal.setTextAdjustWidth(oldTextAdjustWidth);
                    terminal.setTripleBuffer(oldTripleBuffer);
                    terminal.setCursorStyle(oldCursorStyle);
                    terminal.setMouseStyle(oldMouseStyle);
                }
            }
            if (ecmaTerminal != null) {
                ecmaTerminal.setHasSixel(oldSixel);
                ecmaTerminal.setSixelSharedPalette(oldSixelSharedPalette);
                ecmaTerminal.setSixelPaletteSize(oldSixelPaletteSize);
                ecmaTerminal.setWideCharImages(oldWideCharImages);
                ecmaTerminal.setRgbColor(oldRgbColor);
            }
            getApplication().closeWindow(this);
            return;
        }

        // Pass to my parent
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw me on screen.
     */
    @Override
    public void draw() {
        super.draw();

        int left = 34;

        CellAttributes color = getTheme().getColor("ttext");
        drawBox(2, 2, left + 24, 14, color, color);
        putStringXY(4, 2, i18n.getString("swingOptions"), color);

        drawBox(2, 15, left + 12, 22, color, color);
        putStringXY(4, 15, i18n.getString("xtermOptions"), color);

        drawBox(left + 2, 5, left + 22, 10, color, color, 3, false);
        putStringXY(left + 4, 5, i18n.getString("sample"), color);
        for (int i = 6; i < 9; i++) {
            hLineXY(left + 3, i, 18, GraphicsChars.HATCH, color);
        }

    }

    // ------------------------------------------------------------------------
    // TScreenOptionsWindow ---------------------------------------------------
    // ------------------------------------------------------------------------

}
