//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.peer.data;

import javax.annotation.Generated;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.peer.client.PeerService;

/**
 * Provides the implementation of the {@link PeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           date="2010-04-06T22:31:00-0700",
           comments="Derived from the Service class java source.")
public class PeerMarshaller extends InvocationMarshaller
    implements PeerService
{
    /** The method id used to dispatch {@link #generateReport} requests. */
    public static final int GENERATE_REPORT = 1;

    // from interface PeerService
    public void generateReport (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GENERATE_REPORT, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #invokeAction} requests. */
    public static final int INVOKE_ACTION = 2;

    // from interface PeerService
    public void invokeAction (Client arg1, byte[] arg2)
    {
        sendRequest(arg1, INVOKE_ACTION, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #ratifyLockAction} requests. */
    public static final int RATIFY_LOCK_ACTION = 3;

    // from interface PeerService
    public void ratifyLockAction (Client arg1, NodeObject.Lock arg2, boolean arg3)
    {
        sendRequest(arg1, RATIFY_LOCK_ACTION, new Object[] {
            arg2, Boolean.valueOf(arg3)
        });
    }
}
