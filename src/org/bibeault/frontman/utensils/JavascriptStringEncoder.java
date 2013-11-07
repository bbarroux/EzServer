/*
 * Copyright (c) 2006, Bear Bibeault
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - The name of Bear Bibeault may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * This software is provided by the copyright holders and contributors "as is"
 * and any express or implied warranties, including, but not limited to, the
 * implied warranties of merchantability and fitness for a particular purpose
 * are disclaimed. In no event shall the copyright owner or contributors be
 * liable for any direct, indirect, incidental, special, exemplary, or
 * consequential damages (including, but not limited to, procurement of
 * substitute goods or services; loss of use, data, or profits; or business
 * interruption) however caused and on any theory of liability, whether in
 * contract, strict liability, or tort (including negligence or otherwise)
 * arising in any way out of the use of this software, even if advised of the
 * possibility of such damage.
 */
package org.bibeault.frontman.utensils;

public class JavascriptStringEncoder {

    /**
     * <p>Encodes a source string such that the special Javascript string characters <code><bold>&#34;</bold></code>,
     * <code><bold>&#39;</bold></code> and <code><bold>&amp;</bold></code> are escaped to make it safe to sue the string
     * within a Javascript string literal.</p>
     * <p>Notes:
     * <ul>
     * <li>If the source sting is null, the empty string is returned.</li>
     * <li>If the source string requires no encoding, the original string object is returned.</li>
     * </ul>
     * </p>
     *
     * @param source the source string to encode
     * @return the resulting string after encoding
     */
    public static String encode( String source ) {
        if (source == null) return "";
        int length = source.length();
        int count = 0;
        for (int n = 0; n < length; ++n) {
            switch (source.charAt( n )) {
                case '\'':
                    count += 2;
                    break;
                case '"':
                    count += 2;
                    break;
                case '&':
                    count += 2;
                    break;
                default:
                    count++;
                    break;
            }
        }
        if (count == length) return source;
        char[] result = new char[count];
        int index = 0;
        for (int n = 0; n < length; ++n) {
            char c = source.charAt( n );
            switch (c) {
                case '\'':
                    result[index++] = '\\';
                    result[index++] = '\'';
                    break;
                case '"':
                    result[index++] = '\\';
                    result[index++] = '"';
                    break;
                case '&':
                    result[index++] = '\\';
                    result[index++] = '&';
                    break;
                default:
                    result[index++] = c;
                    break;
            }
        }
        return new String( result );
    }

}
