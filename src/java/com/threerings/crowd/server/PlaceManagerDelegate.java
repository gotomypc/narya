//
// $Id: PlaceManagerDelegate.java,v 1.3 2003/05/26 21:24:56 mdb Exp $

package com.threerings.crowd.server;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;

/**
 * Provides an extensible mechanism for encapsulating delegated
 * functionality that works with the place services.
 *
 * <p> Thanks to Java's lack of multiple inheritance, it will likely
 * become necessary to factor certain services that might be used by a
 * variety of {@link PlaceManager} derived classes into delegate classes
 * because they do not fit into the single inheritance hierarchy that
 * makes sense for a particular application. To facilitate this process,
 * this delegate class is provided which the standard place manager can be
 * made to call out to for all of the standard methods.
 */
public class PlaceManagerDelegate
{
    /**
     * Provides the delegate with a reference to the manager for which it
     * is delegating.
     */
    public PlaceManagerDelegate (PlaceManager plmgr)
    {
        _plmgr = plmgr;
    }

    /**
     * Called when the place manager is initialized.
     */
    public void didInit (PlaceConfig config)
    {
    }

    /**
     * Called when the place manager is started up.
     */
    public void didStartup (PlaceObject plobj)
    {
    }

    /**
     * Called when the place manager is shut down.
     */
    public void didShutdown ()
    {
    }

    /**
     * Called when a body enters the place.
     */
    public void bodyEntered (int bodyOid)
    {
    }

    /**
     * Called when a body leaves the place.
     */
    public void bodyLeft (int bodyOid)
    {
    }

    /**
     * Called when a body occupant info is updated.
     */
    public void bodyUpdated (OccupantInfo info)
    {
    }

    /**
     * Called when the last body leaves the place.
     */
    public void placeBecameEmpty ()
    {
    }

    /**
     * Invokes {PlaceManager#where}.
     */
    public String where ()
    {
        return _plmgr.where();
    }

    /** A reference to the manager for which we are delegating. */
    protected PlaceManager _plmgr;
}
