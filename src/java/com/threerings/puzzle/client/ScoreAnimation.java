//
// $Id: ScoreAnimation.java,v 1.1 2003/11/26 01:42:34 mdb Exp $

package com.threerings.puzzle.client;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.samskivert.swing.Label;

import com.threerings.media.animation.Animation;

import com.threerings.puzzle.Log;

public class ScoreAnimation extends Animation
{
    /**
     * Constructs a score animation for the given score value centered at
     * the given coordinates.
     */
    public ScoreAnimation (Label label, int x, int y)
    {
        this(label, x, y, DEFAULT_FLOAT_PERIOD);
    }

    /**
     * Constructs a score animation for the given score value centered at
     * the given coordinates. The animation will float up the screen for
     * 30 pixels.
     */
    public ScoreAnimation (Label label, int x, int y, long floatPeriod)
    {
        this(label, x, y, x, y - DELTA_Y, floatPeriod);
    }

    /**
     * Constructs a score animation for the given score value starting at
     * the given coordinates and floating toward the specified
     * coordinates.
     */
    public ScoreAnimation (Label label, int sx, int sy,
                           int destx, int desty, long floatPeriod)
    {
        super(new Rectangle(sx, sy, label.getSize().width,
                            label.getSize().height));

        // save things off
        _label = label;
        _startX = _x = sx;
        _startY = _y = sy;
        _destx = destx;
        _desty = desty;
        _floatPeriod = floatPeriod;

        // calculate our deltas
        _dx = (destx - sx);
        _dy = (desty - sy);

        // initialize our starting alpha
        _alpha = 1.0f;
    }

    /**
     * Called to change the direction 180 degrees.
     */
    public void flipDirection ()
    {
        _dx = -_dx;
        _dy = -_dy;
    }

    /**
     * Returns the label used to render this score animation.
     */
    public Label getLabel ()
    {
        return _label;
    }

    // documentation inherited
    public void setLocation (int x, int y)
    {
        super.setLocation(x, y);

        // update our destination coordinates
        _destx += (x - _startX);
        _desty += (y - _startY);

        // update our initial coordinates
        _startX = _x = x;
        _startY = _y = y;

        // recalculate our deltas
        _dx = (_destx - x);
        _dy = (_desty - y);
    }

    /**
     * Sets the duration of this score animation to the specified time in
     * milliseconds.  This should be called before the animation is added
     * to the animation manager.
     */
    public void setFloatPeriod (long floatPeriod)
    {
        _floatPeriod = floatPeriod;
    }

    // documentation inherited
    public void tick (long timestamp)
    {
        boolean invalid = false;
        if (_start == 0) {
            // initialize our starting time
            _start = timestamp;
            // we need to make sure to invalidate ourselves initially
            invalid = true;
        }

        long fadeDelay = _floatPeriod/2;
        long fadePeriod = _floatPeriod - fadeDelay;

        // figure out the current alpha
        long msecs = timestamp - _start;
        float oalpha = _alpha;
        if (msecs > fadeDelay) {
            long rmsecs = msecs - fadeDelay;
            _alpha = Math.max(1.0f - (rmsecs / (float)fadePeriod), 0.0f);
            _alpha = Math.min(_alpha, 1.0f);
        }

        // get the alpha composite
        _comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, _alpha);

        // determine the new y-position of the score
        float pctdone = (float)msecs / _floatPeriod;
        int ox = _x, oy = _y;
        _x = _startX + (int)(_dx * pctdone);
        _y = _startY + (int)(_dy * pctdone);

        // only update our location and dirty ourselves if we actually
        // moved or our alpha changed
        if (ox != _x || oy != _y) {
            // dirty our old location
            invalidate();
            _bounds.setLocation(_x, _y);
            invalid = true;

        } else if (oalpha != _alpha) {
            invalid = true;
        }

        if (invalid) {
            // dirty our current location
            invalidate();
        }

        // note whether we're done
        _finished = (msecs >= _floatPeriod);
    }

    // documentation inherited
    public void fastForward (long timeDelta)
    {
        if (_start > 0) {
            _start += timeDelta;
        }
    }

    // documentation inherited
    public void paint (Graphics2D gfx)
    {
        Composite ocomp = gfx.getComposite();
        if (_comp != null) {
            gfx.setComposite(_comp);
        }
        _label.render(gfx, _x, _y);
        gfx.setComposite(ocomp);
    }

    // documentation inherited
    protected void toString (StringBuffer buf)
    {
        super.toString(buf);

        buf.append(", x=").append(_x);
        buf.append(", y=").append(_y);
        buf.append(", alpha=").append(_alpha);
    }

    /** The starting animation time. */
    protected long _start;

    /** The label we're animating. */
    protected Label _label;

    /** The starting coordinates of the score. */
    protected int _startX, _startY;

    /** The current coordinates of the score. */
    protected int _x, _y;

    /** The destination coordinates towards which the animation travels. */
    protected int _destx, _desty;

    /** The distance to be traveled by the score animation. */
    protected int _dx, _dy;

    /** The duration for which we float up the screen. */
    protected long _floatPeriod;

    /** The current alpha level used to render the score. */
    protected float _alpha;

    /** The composite used to render the score. */
    protected Composite _comp;

    /** The time in milliseconds during which the score is visible. */
    protected static final long DEFAULT_FLOAT_PERIOD = 1500L;

    /** The total vertical distance the score travels. */
    protected static final int DELTA_Y = 30;
}
