//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.crowd.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.CrowdCodes;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.Place;

import static com.threerings.crowd.Log.log;

/**
 * Provides the server-side side of the body-related invocation services.
 */
public class BodyProvider
    implements InvocationProvider
{
    /** Used by {@link #updateOccupantInfo}. */
    public static interface OccupantInfoOp
    {
        /**
         * Updates the supplied occupant info record, returning true if
         * changes were made and thus the object should be published anew
         * to the place object, false if no publish should be done.
         */
        public boolean update (OccupantInfo oinfo);
    }

    /**
     * Constructs and initializes a body provider instance which will be
     * used to handle all body-related invocation service requests.
     */
    public static void init (InvocationManager invmgr)
    {
        // register a provider instance
        invmgr.registerDispatcher(new BodyDispatcher(new BodyProvider()), CrowdCodes.CROWD_GROUP);
    }

    /**
     * Handles a request to set the idle state of a client to the specified value.
     */
    public void setIdle (ClientObject caller, boolean idle)
        throws InvocationException
    {
        BodyObject bobj = (BodyObject)caller;

        // determine the body's proposed new status
        byte nstatus = (idle) ? OccupantInfo.IDLE : OccupantInfo.ACTIVE;

        // report NOOP attempts
        if (bobj.status == nstatus) {
            throw new InvocationException((idle) ? "m.already_idle" : "m.already_active");
        }

        // update their status!
        log.debug("Setting user idle state [user=" + bobj.username + ", status=" + nstatus + "].");
        updateOccupantStatus(bobj, bobj.location, nstatus);
    }

    /**
     * Locates the specified body's occupant info in the specified location, applies the supplied
     * occuapnt info operation to it and then broadcasts the updated info (assuming the occop
     * returned true indicating that an update was made).
     */
    public static void updateOccupantInfo (BodyObject body, Place location, OccupantInfoOp occop)
    {
        PlaceManager pmgr = (location == null) ? null :
            CrowdServer.plreg.getPlaceManager(location.placeOid);
        if (pmgr == null) {
            return;
        }

        OccupantInfo info = pmgr.getOccupantInfo(body.getOid());
        if (info != null && occop.update(info)) {
            pmgr.updateOccupantInfo(info);
        }
    }

    /**
     * Updates the connection status for the given body object's occupant info in the specified
     * location.
     */
    public static void updateOccupantStatus (BodyObject body, Place location, final byte status)
    {
        // no need to NOOP
        if (body.status != status) {
            // update the status in their body object
            body.setStatus(status);
            body.statusTime = System.currentTimeMillis();
        }

        updateOccupantInfo(body, location, new OccupantInfoOp() {
            public boolean update (OccupantInfo info) {
                if (info.status != status) {
                    info.status = status;
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
}
