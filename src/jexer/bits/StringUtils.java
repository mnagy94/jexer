/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2017 Kevin Lamonte
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
import java.util.LinkedList;

/**
 * StringUtils contains methods to:
 *
 *    - Convert one or more long lines of strings into justified text
 *      paragraphs.
 *
 *    - Unescape C0 control codes.
 *
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
        List<String> result = new LinkedList<String>();

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
                        if (word.length() + line.length() > n) {
                            // This word will exceed the line length.  Wrap
                            // at it instead.
                            result.add(line.toString());
                            line = new StringBuilder();
                        }
                        if ((word.toString().startsWith(" "))
                            && (line.length() == 0)
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

            if (word.length() + line.length() > n) {
                // This word will exceed the line length.  Wrap at it
                // instead.
                result.add(line.toString());
                line = new StringBuilder();
            }
            if ((word.toString().startsWith(" "))
                && (line.length() == 0)
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
        List<String> result = new LinkedList<String>();

        /*
         * Same as left(), but preceed each line with spaces to make it n
         * chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n - line.length(); i++) {
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
        List<String> result = new LinkedList<String>();

        /*
         * Same as left(), but preceed/succeed each line with spaces to make
         * it n chars long.
         */
        List<String> lines = left(str, n);
        for (String line: lines) {
            StringBuilder sb = new StringBuilder();
            int l = (n - line.length()) / 2;
            int r = n - line.length() - l;
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
        List<String> result = new LinkedList<String>();

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

}
