//
// $Id: LineSegmentPath.java,v 1.22 2002/05/17 21:13:26 mdb Exp $

package com.threerings.media.sprite;

import java.awt.Color;
import java.awt.Point;
import java.awt.Graphics2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.StringUtil;

import com.threerings.util.DirectionCodes;
import com.threerings.util.DirectionUtil;

import com.threerings.media.Log;
import com.threerings.media.util.MathUtil;

/**
 * The line segment path is used to cause a sprite to follow a path
 * that is made up of a sequence of line segments. There must be at
 * least two nodes in any worthwhile path. The direction of the first
 * node in the path is meaningless since the sprite begins at that
 * node and will therefore never be heading towards it.
 */
public class LineSegmentPath
    implements DirectionCodes, Path
{
    /**
     * Constructs a line segment path.
     */
    public LineSegmentPath ()
    {
        _nodes = new ArrayList();
    }

    /**
     * Constructs a line segment path that consists of a single segment
     * connecting the point <code>(x1, y1)</code> with <code>(x2,
     * y2)</code>.  The orientation for the first node is set arbitrarily
     * and the second node is oriented based on the vector between the two
     * nodes (in top-down coordinates).
     */
    public LineSegmentPath (int x1, int y1, int x2, int y2)
    {
        this();
        addNode(x1, y1, NORTH);
        Point p1 = new Point(x1, y1), p2 = new Point(x2, y2);
        int dir = DirectionUtil.getDirection(p1, p2);
        addNode(x2, y2, dir);
    }

    /**
     * Constructs a line segment path with the specified list of
     * points.  An arbitrary direction will be assigned to the
     * starting node.
     *
     * @param x the starting node x-position.
     * @param y the starting node y-position.
     */
    public LineSegmentPath (List points)
    {
        this();
        createPath(points);
    }

    /**
     * Add a node to the path with the specified destination point and
     * facing direction.
     *
     * @param x the x-position.
     * @param y the y-position.
     * @param dir the facing direction.
     */
    public void addNode (int x, int y, int dir)
    {
        _nodes.add(new PathNode(x, y, dir));
    }

    /**
     * Return the requested node index in the path, or null if no such
     * index exists.
     *
     * @param idx the node index.
     *
     * @return the path node.
     */
    public PathNode getNode (int idx)
    {
        return (PathNode)_nodes.get(idx);
    }

    /**
     * Return the number of nodes in the path.
     */
    public int size ()
    {
        return _nodes.size();
    }

    /**
     * Sets the velocity of this sprite in pixels per millisecond. The
     * velocity is measured as pixels traversed along the path that the
     * sprite is traveling rather than in the x or y directions
     * individually.  Note that the sprite velocity should not be changed
     * while a path is being traversed; doing so may result in the sprite
     * position changing unexpectedly.
     *
     * @param velocity the sprite velocity in pixels per millisecond.
     */
    public void setVelocity (float velocity)
    {
        _vel = velocity;
    }

    /**
     * Computes the velocity at which the sprite will need to travel along
     * this path such that it will arrive at the destination in
     * approximately the specified number of milliseconds. Efforts are
     * taken to get the sprite there as close to the desired time as
     * possible, but framerate variation may prevent it from arriving
     * exactly on time.
     */
    public void setDuration (long millis)
    {
        // if we have only zero or one nodes, we don't have enough
        // information to compute our velocity
        int ncount = _nodes.size();
        if (ncount < 2) {
            Log.warning("Requested to set duration of bogus path " +
                        "[path=" + this + ", duration=" + millis + "].");
            return;
        }

        // compute the total distance along our path
        float distance = 0;
        PathNode start = (PathNode)_nodes.get(0);
        for (int ii = 1; ii < ncount; ii++) {
            PathNode end = (PathNode)_nodes.get(ii);
            distance += MathUtil.distance(
                start.loc.x, start.loc.y, end.loc.x, end.loc.y);
            start = end;
        }

        // set the velocity accordingly
        setVelocity(distance/millis);
    }

    // documentation inherited from interface
    public void viewWillScroll (int dx, int dy)
    {
        // adjust the coordinates of our path nodes
        int ncount = _nodes.size();
        for (int ii = 0; ii < ncount; ii++) {
            PathNode node = (PathNode)_nodes.get(ii);
            node.loc.x -= dx;
            node.loc.y -= dy;
        }
    }

    // documentation inherited
    public void init (Sprite sprite, long timestamp)
    {
        // give the sprite a chance to perform any starting antics
        sprite.pathBeginning();

        // if we have only one node then let the sprite know that we're
        // done straight away
        if (size() < 2) {
            // move the sprite to the location specified by the first node
            // (assuming we have a first node)
            if (size() == 1) {
                PathNode node = (PathNode)_nodes.get(0);
                sprite.setLocation(node.loc.x, node.loc.y);
            }
            // and let the sprite know that we're done
	    sprite.pathCompleted();
	    return;
	}

        // and an enumeration of the path nodes
        _niter = _nodes.iterator();

	// pretend like we were previously heading to our starting position
        _dest = getNextNode();

        // begin traversing the path
        headToNextNode(sprite, timestamp, timestamp);
    }

    // documentation inherited
    public boolean updatePosition (Sprite sprite, long timestamp)
    {
        // figure out how far along this segment we should be
        long msecs = timestamp - _nodestamp;
        float travpix = msecs * _vel;
        float pctdone = travpix / _seglength;

        // if we've moved beyond the end of the path, we need to adjust
        // the timestamp to determine how much time we used getting to the
        // end of this node, then move to the next one
        if (pctdone >= 1.0) {
            long used = (long)(_seglength / _vel);
            return headToNextNode(sprite, _nodestamp + used, timestamp);
        }

        // otherwise we position the sprite along the path
        int ox = sprite.getX();
        int oy = sprite.getY();
        int nx = _src.loc.x + (int)((_dest.loc.x - _src.loc.x) * pctdone);
        int ny = _src.loc.y + (int)((_dest.loc.y - _src.loc.y) * pctdone);

//         Log.info("Moving sprite [msecs=" + msecs + ", pctdone=" + pctdone +
//                  ", travpix=" + travpix + ", seglength=" + _seglength +
//                  ", dx=" + (nx-ox) + ", dy=" + (ny-oy) + "].");

        // only update the sprite's location if it actually moved
        if (ox != nx || oy != ny) {
            sprite.setLocation(nx, ny);
            return true;
        }        

        return false;
    }

    // documentation inherited
    public void fastForward (long timeDelta)
    {
        _nodestamp += timeDelta;
    }

    /**
     * Place the sprite moving along the path at the end of the
     * previous path node, face it appropriately for the next node,
     * and start it on its way.  Returns whether the sprite position
     * moved.
     */
    protected boolean headToNextNode (Sprite sprite, long startstamp, long now)
    {
        // check to see if we've completed our path
        if (!_niter.hasNext()) {
            // move the sprite to the location of our last destination
            sprite.setLocation(_dest.loc.x, _dest.loc.y);
            sprite.pathCompleted();
            return true;
        }

        // our previous destination is now our source
        _src = _dest;

        // pop the next node off the path
        _dest = getNextNode();

        // adjust the sprite's orientation
        if (_dest.dir != NONE) {
            sprite.setOrientation(_dest.dir);
        }

        // make a note of when we started traversing this node
        _nodestamp = startstamp;

        // figure out the distance from source to destination
        _seglength = MathUtil.distance(
            _src.loc.x, _src.loc.y, _dest.loc.x, _dest.loc.y);

        // if we're already there (the segment length is zero), we skip to
        // the next segment
        if (_seglength == 0) {
            return headToNextNode(sprite, startstamp, now);
        }

        // now update the sprite's position based on our progress thus far
        return updatePosition(sprite, now);
    }

    // documentation inherited
    public void paint (Graphics2D gfx)
    {
	gfx.setColor(Color.red);
	Point prev = null;
	int size = size();
	for (int ii = 0; ii < size; ii++) {
	    PathNode n = (PathNode)getNode(ii);
	    if (prev != null) {
                gfx.drawLine(prev.x, prev.y, n.loc.x, n.loc.y);
            }
	    prev = n.loc;
	}
    }

    // documentation inherited
    public String toString ()
    {
        return StringUtil.toString(_nodes.iterator());
    }

    /**
     * Populate the path with the path nodes that lead the sprite from
     * its starting position to the given destination coordinates
     * following the given list of screen coordinates.
     */
    protected void createPath (List points)
    {
        Point last = null;
        int size = points.size();
        for (int ii = 0; ii < size; ii++) {
            Point p = (Point)points.get(ii);

            int dir = (ii == 0) ? NORTH : DirectionUtil.getDirection(last, p);
            addNode(p.x, p.y, dir);
            last = p;
        }
    }        

    /**
     * Gets the next node in the path.
     */
    protected PathNode getNextNode ()
    {
        return (PathNode)_niter.next();
    }        

    /** The nodes that make up the path. */
    protected ArrayList _nodes;

    /** We use this when moving along this path. */
    protected Iterator _niter;

    /** When moving, the sprite's source path node. */
    protected PathNode _src;

    /** When moving, the sprite's destination path node. */
    protected PathNode _dest;

    /** The time at which we started traversing the current node. */
    protected long _nodestamp;

    /** The length in pixels of the current path segment. */
    protected float _seglength;

    /** The path velocity in pixels per millisecond. */
    protected float _vel = DEFAULT_VELOCITY;

    /** When moving, the sprite position including fractional pixels. */ 
    protected float _movex, _movey;

    /** When moving, the distance to move on each axis per tick. */
    protected float _incx, _incy;

    /** The distance to move on the straight path line per tick. */
    protected float _fracx, _fracy;

    /** Default sprite velocity. */
    protected static final float DEFAULT_VELOCITY = 200f/1000f;
}
