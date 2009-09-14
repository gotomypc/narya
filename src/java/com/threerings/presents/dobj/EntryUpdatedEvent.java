//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2009 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.presents.dobj;

import com.samskivert.util.StringUtil;

import com.threerings.presents.net.Transport;

import static com.threerings.presents.Log.log;

/**
 * An entry updated event is dispatched when an entry of a {@link DSet} is updated. It can also be
 * constructed to request the update of an entry and posted to the dobjmgr.
 *
 * @see DObjectManager#postEvent
 *
 * @param <T> the type of entry being handled by this event. This must match the type on the set
 * that generated this event.
 */
public class EntryUpdatedEvent<T extends DSet.Entry> extends NamedEvent
{
    /**
     * Constructs a new entry updated event on the specified target object for the specified set
     * name and with the supplied updated entry.
     *
     * @param targetOid the object id of the object to whose set we will add an entry.
     * @param name the name of the attribute in which to update the specified entry.
     * @param entry the entry to update.
     * @param oldEntry the previous value of the entry.
     */
    public EntryUpdatedEvent (int targetOid, String name, T entry, T oldEntry)
    {
        this(targetOid, name, entry, oldEntry, Transport.DEFAULT);
    }

    /**
     * Constructs a new entry updated event on the specified target object for the specified set
     * name and with the supplied updated entry.
     *
     * @param targetOid the object id of the object to whose set we will add an entry.
     * @param name the name of the attribute in which to update the specified entry.
     * @param entry the entry to update.
     * @param oldEntry the previous value of the entry.
     * @param transport a hint as to the type of transport desired for the event.
     */
    public EntryUpdatedEvent (int targetOid, String name, T entry, T oldEntry, Transport transport)
    {
        super(targetOid, name, transport);
        _entry = entry;
        _oldEntry = oldEntry;
    }

    /**
     * Constructs a blank instance of this event in preparation for unserialization from the
     * network.
     */
    public EntryUpdatedEvent ()
    {
    }

    /**
     * Returns the entry that has been updated.
     */
    public T getEntry ()
    {
        return _entry;
    }

    /**
     * Returns the entry that was in the set prior to being updated.
     */
    public T getOldEntry ()
    {
        return _oldEntry;
    }

    @Override
    public boolean alreadyApplied ()
    {
        return (_oldEntry != UNSET_OLD_ENTRY);
    }

    @Override
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        // only apply the change if we haven't already
        if (!alreadyApplied()) {
            DSet<T> set = target.getSet(_name);
            // fetch the previous value for interested callers
            _oldEntry = set.update(_entry);
            if (_oldEntry == null) {
                // complain if we didn't update anything
                log.warning("No matching entry to update", "entry", this, "set", set);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void notifyListener (Object listener)
    {
        if (listener instanceof SetListener<?>) {
            @SuppressWarnings("unchecked") SetListener<T> setlist = (SetListener<T>)listener;
            setlist.entryUpdated(this);
        }
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        buf.append("ELUPD:");
        super.toString(buf);
        buf.append(", entry=");
        StringUtil.toString(buf, _entry);
    }

    protected T _entry;

    @SuppressWarnings("unchecked")
    protected transient T _oldEntry = (T)UNSET_OLD_ENTRY;
}
