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
package jexer.bits;

import java.util.List;
import java.util.ArrayList;

/**
 * StringUtils contains methods to:
 *
 *    - Convert one or more long lines of strings into justified text
 *      paragraphs.
 *
 *    - Unescape C0 control codes.
 *
 *    - Read/write a line of RFC4180 comma-separated values strings to/from a
 *      list of strings.
 */
public class StringUtils {

    /**
     * Left-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> left(final String str, final int n) {
        List<String> result = new ArrayList<String>();

        /*
         * General procedure:
         *
         *   1. Split on '\n' into paragraphs.
         *
         *   2. Scan each line, noting the position of the last
         *      beginning-of-a-word.
         *
         *   3. Chop at the last #2 if the next beginning-of-a-word exceeds
         *      n.
         *
         *   4. Return the lines.
         */

        String [] rawLines = str.split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            StringBuilder line = new StringBuilder();
            StringBuilder word = new StringBuilder();
            boolean inWord = false;
            for (int j = 0; j < rawLines[i].length(); j++) {
                char ch = rawLines[i].charAt(j);
                if ((ch == ' ') || (ch == '\t')) {
                    if (inWord == true) {
                        // We have just transitioned from a word to
                        // whitespace.  See if we have enough space to add
                        // the word to the line.
                        if (width(word.toString()) + width(line.toString()) > n) {
                            // This word will exceed the line length.  Wrap
                            // at it instead.
                            result.add(line.toString());
                            line = new StringBuilder();
                        }
                        if ((word.toString().startsWith(" "))
                            && (width(line.toString()) == 0)
                        ) {
                            line.append(word.substring(1));
                        } else {
                            line.append(word);
                        }
                        word = new StringBuilder();
                        word.append(ch);
                        inWord = false;
                    } else {
                        // We are in the whitespace before another word.  Do
                        // nothing.
                    }
                } else {
                    if (inWord == true) {
                        // We are appending to a word.
                        word.append(ch);
                    } else {
                        // We have transitioned from whitespace to a word.
                        word.append(ch);
                        inWord = true;
                    }
                }
            } // for (int j = 0; j < rawLines[i].length(); j++)

            if (width(word.toString()) + width(line.toString()) > n) {
                // This word will exceed the line length.  Wrap at it
                // instead.
                result.add(line.toString());
                line = new StringBuilder();
            }
            if ((word.toString().startsWith(" "))
                && (width(line.toString()) == 0)
            ) {
                line.append(word.substring(1));
            } else {
                line.append(word);
            }
            result.add(line.toString());
        } // for (int i = 0; i < rawLines.length; i++) {

        return result;
    }

    /**
     * Right-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> right(final String str, final int n) {
        List<String> result = new ArrayList<String>();

        /*
         * Same as left(), but preceed each line with spaces to make it n
         * chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n - width(line); i++) {
                sb.append(' ');
            }
            sb.append(line);
            result.add(sb.toString());
        }

        return result;
    }

    /**
     * Center a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> center(final String str, final int n) {
        List<String> result = new ArrayList<String>();

        /*
         * Same as left(), but preceed/succeed each line with spaces to make
         * it n chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            StringBuilder sb = new StringBuilder();
            int l = (n - width(line)) / 2;
            int r = n - width(line) - l;
            for (int i = 0; i < l; i++) {
                sb.append(' ');
            }
            sb.append(line);
            for (int i = 0; i < r; i++) {
                sb.append(' ');
            }
            result.add(sb.toString());
        }

        return result;
    }

    /**
     * Fully-justify a string into a list of lines.
     *
     * @param str the string
     * @param n the maximum number of characters in a line
     * @return the list of lines
     */
    public static List<String> full(final String str, final int n) {
        List<String> result = new ArrayList<String>();

        /*
         * Same as left(), but insert spaces between words to make each line
         * n chars long.  The "algorithm" here is pretty dumb: it performs a
         * split on space and then re-inserts multiples of n between words.
         */
        List<String> lines = left(str, n);
        for (int lineI = 0; lineI < lines.size() - 1; lineI++) {
            String line = lines.get(lineI);
            String [] words = line.split(" ");
            if (words.length > 1) {
                int charCount = 0;
                for (int i = 0; i < words.length; i++) {
                    charCount += words[i].length();
                }
                int spaceCount = n - charCount;
                int q = spaceCount / (words.length - 1);
                int r = spaceCount % (words.length - 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < words.length - 1; i++) {
                    sb.append(words[i]);
                    for (int j = 0; j < q; j++) {
                        sb.append(' ');
                    }
                    if (r > 0) {
                        sb.append(' ');
                        r--;
                    }
                }
                for (int j = 0; j < r; j++) {
                    sb.append(' ');
                }
                sb.append(words[words.length - 1]);
                result.add(sb.toString());
            } else {
                result.add(line);
            }
        }
        if (lines.size() > 0) {
            result.add(lines.get(lines.size() - 1));
        }

        return result;
    }

    /**
     * Convert raw strings into escaped strings that be splatted on the
     * screen.
     *
     * @param str the string
     * @return a string that can be passed into Screen.putStringXY()
     */
    public static String unescape(final String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if ((ch < 0x20) || (ch == 0x7F)) {
                switch (ch) {
                case '\b':
                    sb.append("\\b");
                    continue;
                case '\f':
                    sb.append("\\f");
                    continue;
                case '\n':
                    sb.append("\\n");
                    continue;
                case '\r':
                    sb.append("\\r");
                    continue;
                case '\t':
                    sb.append("\\t");
                    continue;
                case 0x7f:
                    sb.append("^?");
                    continue;
                default:
                    sb.append(' ');
                    continue;
                }
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Read a line of RFC4180 comma-separated values (CSV) into a list of
     * strings.
     *
     * @param line the CSV line, with or without without line terminators
     * @return the list of strings
     */
    public static List<String> fromCsv(final String line) {
        List<String> result = new ArrayList<String>();

        StringBuilder str = new StringBuilder();
        boolean quoted = false;
        boolean fieldQuoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            /*
            System.err.println("ch '" + ch + "' str '" + str + "' " +
                " fieldQuoted " + fieldQuoted + " quoted " + quoted);
             */

            if (ch == ',') {
                if (fieldQuoted && quoted) {
                    // Terminating a quoted field.
                    result.add(str.toString());
                    str = new StringBuilder();
                    quoted = false;
                    fieldQuoted = false;
                } else if (fieldQuoted) {
                    // Still waiting to see the terminating quote for this
                    // field.
                    str.append(ch);
                } else if (quoted) {
                    // An unmatched double-quote and comma.  This should be
                    // an invalid sequence.  We will treat it as a quote
                    // terminating the field.
                    str.append('\"');
                    result.add(str.toString());
                    str = new StringBuilder();
                    quoted = false;
                    fieldQuoted = false;
                } else {
                    // A field separator.
                    result.add(str.toString());
                    str = new StringBuilder();
                    quoted = false;
                    fieldQuoted = false;
                }
                continue;
            }

            if (ch == '\"') {
                if ((str.length() == 0) && (!fieldQuoted)) {
                    // The opening quote to a quoted field.
                    fieldQuoted = true;
                } else if (quoted) {
                    // This is a double-quote.
                    str.append('\"');
                    quoted = false;
                } else {
                    // This is the beginning of a quote.
                    quoted = true;
                }
                continue;
            }

            // Normal character, pass it on.
            str.append(ch);
        }

        // Include the final field.
        result.add(str.toString());

        return result;
    }

    /**
     * Write a list of strings to on line of RFC4180 comma-separated values
     * (CSV).
     *
     * @param list the list of strings
     * @return the CSV line, without any line terminators
     */
    public static String toCsv(final List<String> list) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (String str: list) {

            if (!str.contains("\"") && !str.contains(",")) {
                // Just append the string with a comma.
                result.append(str);
            } else if (!str.contains("\"") && str.contains(",")) {
                // Contains commas, but no quotes.  Just double-quote it.
                result.append("\"");
                result.append(str);
                result.append("\"");
            } else if (str.contains("\"")) {
                // Contains quotes and maybe commas.  Double-quote it and
                // replace quotes inside.
                result.append("\"");
                for (int j = 0; j < str.length(); j++) {
                    char ch = str.charAt(j);
                    result.append(ch);
                    if (ch == '\"') {
                        result.append("\"");
                    }
                }
                result.append("\"");
            }

            if (i < list.size() - 1) {
                result.append(",");
            }
            i++;
        }
        return result.toString();
    }

    /**
     * Determine display width of a Unicode code point.
     *
     * @param ch the code point, can be char
     * @return the number of text cell columns required to display this code
     * point, one of 0, 1, or 2
     */
    public static int width(final int ch) {
        /*
         * This routine is a modified version of mk_wcwidth() available
         * at: http://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c
         *
         * The combining characters list has been omitted from this
         * implementation.  Hopefully no users will be impacted.
         */

        // 8-bit control characters: width 0
        if (ch == 0) {
            return 0;
        }
        if ((ch < 32) || ((ch >= 0x7f) && (ch < 0xa0))) {
            return 0;
        }

        // All others: either 1 or 2
        if ((ch >= 0x1100)
            && ((ch <= 0x115f)
                // Hangul Jamo init. consonants
                || (ch == 0x2329)
                || (ch == 0x232a)
                // CJK ... Yi
                || ((ch >= 0x2e80) && (ch <= 0xa4cf) && (ch != 0x303f))
                // Hangul Syllables
                || ((ch >= 0xac00) && (ch <= 0xd7a3))
                // CJK Compatibility Ideographs
                || ((ch >= 0xf900) && (ch <= 0xfaff))
                // Vertical forms
                || ((ch >= 0xfe10) && (ch <= 0xfe19))
                // CJK Compatibility Forms
                || ((ch >= 0xfe30) && (ch <= 0xfe6f))
                // Fullwidth Forms
                || ((ch >= 0xff00) && (ch <= 0xff60))
                || ((ch >= 0xffe0) && (ch <= 0xffe6))
                || ((ch >= 0x20000) && (ch <= 0x2fffd))
                || ((ch >= 0x30000) && (ch <= 0x3fffd))
                // emoji
                || ((ch >= 0x1f004) && (ch <= 0x1fffd))
            )
        ) {
            return 2;
        }
        return 1;
    }

    /**
     * Determine display width of a string.  This ASSUMES that no characters
     * are combining.  Hopefully no users will be impacted.
     *
     * @param str the string
     * @return the number of text cell columns required to display this string
     */
    public static int width(final String str) {
        int n = 0;
        for (int i = 0; i < str.length();) {
            int ch = str.codePointAt(i);
            n += width(ch);
            i += Character.charCount(ch);
        }
        return n;
    }

}
