//
// $Id: EntryUpdatedEvent.java,v 1.4 2002/02/04 00:50:11 mdb Exp $

package com.threerings.presents.dobj;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.threerings.presents.Log;
import com.threerings.presents.dobj.io.ElementUtil;

/**
 * An element updated event is dispatched when an element of a
 * <code>DSet</code> is updated. It can also be constructed to request the
 * update of an element and posted to the dobjmgr.
 *
 * @see DObjectManager#postEvent
 */
public class ElementUpdatedEvent extends TypedEvent
{
    /** The typed object code for this event. */
    public static final short TYPE = TYPE_BASE + 10;

    /**
     * Constructs a new element updated event on the specified target
     * object for the specified set name and with the supplied updated
     * element.
     *
     * @param targetOid the object id of the object to whose set we will
     * add an element.
     * @param name the name of the attribute in which to update the
     * specified element.
     * @param elem the element to update.
     * @param qualified whether or not the element need be qualified with
     * its class when serializing (true for heterogenous sets, false for
     * homogenous sets).
     */
    public ElementUpdatedEvent (int targetOid, String name, DSet.Element elem,
                              boolean qualified)
    {
        super(targetOid);
        _name = name;
        _elem = elem;
        _qualified = qualified;
    }

    /**
     * Constructs a blank instance of this event in preparation for
     * unserialization from the network.
     */
    public ElementUpdatedEvent ()
    {
    }

    /**
     * Returns the name of the set attribute for which an element has been
     * updated.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns the element that has been updated.
     */
    public DSet.Element getElement ()
    {
        return _elem;
    }

    /**
     * Applies this event to the object.
     */
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        DSet set = (DSet)target.getAttribute(_name);

        // now that we have access to our target set, we can unflatten our
        // element (if need be)
        if (_elem == null) {
            try {
                _elem = ElementUtil.unflatten(set, _bytes);
            } catch (Exception e) {
                Log.warning("Error unflattening element " + this + ".");
                Log.logStackTrace(e);
                return false;
            }
        }

        // update the element
        if (!set.update(_elem)) {
            // complain if we didn't update anything
            Log.warning("No matching element to update " + this + ".");
            return false;
        }

        return true;
    }

    // documentation inherited
    public short getType ()
    {
        return TYPE;
    }

    // documentation inherited
    public void writeTo (DataOutputStream out)
        throws IOException
    {
        super.writeTo(out);
        out.writeUTF(_name);
        ElementUtil.flatten(out, _elem, _qualified);
    }

    // documentation inherited
    public void readFrom (DataInputStream in)
        throws IOException
    {
        super.readFrom(in);
        _name = in.readUTF();

        // we read in the raw element data now and decode it later when we
        // have access to the object and the DSet instance that knows what
        // type of element we need to decode
        int bcount = in.readInt();
        _bytes = new byte[bcount];
        in.readFully(_bytes, 0, bcount);
    }

    // documentation inherited
    protected void notifyListener (Object listener)
    {
        if (listener instanceof SetListener) {
            ((SetListener)listener).elementUpdated(this);
        }
    }

    // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append("ELUPD:");
        super.toString(buf);
        buf.append(", name=").append(_name);
        buf.append(", elem=").append(_elem);
    }

    protected String _name;
    protected byte[] _bytes;
    protected DSet.Element _elem;
    protected boolean _qualified;
}
