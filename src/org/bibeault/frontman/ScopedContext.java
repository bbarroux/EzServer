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
package org.bibeault.frontman;

import javax.servlet.jsp.PageContext;

/**
 * Enumeration modeling the four context scopes.
 */
public enum ScopedContext {

    PAGE( PageContext.PAGE_SCOPE ),
    REQUEST( PageContext.REQUEST_SCOPE ),
    SESSION( PageContext.SESSION_SCOPE ),
    APPLICATION( PageContext.APPLICATION_SCOPE );

    private int value;

    private ScopedContext( int value ) {
        this.value = value;
    }

    /**
     * Returns the corresponding numeric value for this context as defined
     * by javax.servlet.jsp.PageContext.
     * @return the scope's numeric value
     */
    public int toValue() { return this.value; }

    /**
     * Case-insensitive variant of the valueOf() method.
     * @param name the name of the enum in any case
     * @return the corresponding enum
     */
    public static ScopedContext valueOfCaseInsensitive( String name ) {
        return valueOf( name.toUpperCase() );
    }

}
