package com.threerings.presents.net {

import flash.util.StringBuilder;

import com.threerings.util.Name;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

public /* abstract */ class Credentials
    implements Streamable
{
    public function Credentials (username :Name)
    {
        _username = username;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
        //throws IOError
    {
    	out.writeObject(_username);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
        //throws IOError
    {
    	_username = (ins.readObject() as Name);
    }

    public function toString () :String
    {
        var buf :StringBuilder = new StringBuilder("[");
        toStringBuf(buf);
        buf.append("]");
        return buf.toString();
    }

    internal function toStringBuf (buf :StringBuilder) :void
    {
        buf.append("username=", _username);
    }

    /** The username. */
    protected var _username :Name;
}
}