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

import javax.servlet.ServletException;

/**
 * An extension of ServletException that is thrown by the Command Broker in the event that an invalid view name is
 * referenced.
 */
public final class ViewNotFoundException extends ServletException {

    public ViewNotFoundException( String viewName, Exception e ) {
        super( (viewName == null || viewName.length() == 0) ? "Cannot display view with missing name" : ("View with name of " + viewName + " not found"), e );
    }

    public ViewNotFoundException( String viewName ) {
        this( viewName, null );
    }

}
