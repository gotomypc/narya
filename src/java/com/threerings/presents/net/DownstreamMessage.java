//
// $Id: DownstreamMessage.java,v 1.10 2002/12/20 23:28:24 mdb Exp $

package com.threerings.presents.net;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

/**
 * This class encapsulates a message in the distributed object protocol
 * that flows from the server to the client. Downstream messages include
 * object subscription, event forwarding and session management.
 */
public abstract class DownstreamMessage extends SimpleStreamableObject
{
    /**
     * The message id of the upstream message with which this downstream
     * message is associated (or -1 if it is not associated with any
     * upstream message).
     */
    public short messageId = -1;

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return "[msgid=" + messageId + "]";
    }
}
