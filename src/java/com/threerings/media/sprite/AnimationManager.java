//
// $Id: AnimationManager.java,v 1.14 2001/08/22 02:14:57 mdb Exp $

package com.threerings.media.sprite;

import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.samskivert.util.Interval;
import com.samskivert.util.IntervalManager;

import com.threerings.media.Log;
import com.threerings.miso.util.PerformanceMonitor;
import com.threerings.miso.util.PerformanceObserver;

/**
 * The AnimationManager handles the regular refreshing of the scene
 * view to allow for animation.  It also may someday manage special
 * scene-wide animations, such as rain, fog, or earthquakes.
 */
public class AnimationManager implements Interval, PerformanceObserver
{
    /**
     * Construct and initialize the animation manager with a sprite
     * manager and the view in which the animations will take place.
     */
    public AnimationManager (SpriteManager spritemgr, AnimatedView view)
    {
        // save off references to the objects we care about
        _spritemgr = spritemgr;
        _view = view;

        // create a ticker for queueing up tick requests on the AWT thread
        _ticker = new Runnable() {
            public void run ()
            {
                tick();
            }
        };
    }

    /**
     * This method should be called by the component the animation
     * manager is animating after it's been fully laid out within its
     * container(s); typically, after <code>Component.doLayout()</code>
     * has been called.  The animation manager will then register
     * itself with the <code>IntervalManager</code> to effect its
     * periodic repainting of the target component.
     */
    public void start ()
    {
        // register to monitor the refresh action 
        PerformanceMonitor.register(this, "refresh", 1000);

        // register ourselves with the interval manager
        IntervalManager.register(this, REFRESH_INTERVAL, null, true);
    }

    /**
     * Called by our interval when we'd like to begin a tick.  Returns
     * whether we're already ticking, and notes that we've requested
     * another tick.
     */
    protected synchronized boolean requestTick ()
    {
        if (_ticking++ > 0) return false;
        return true;
    }

    /**
     * Called by the tick task when it's finished with a tick.
     * Returns whether a new tick task should be created immediately.
     */
    protected synchronized boolean finishedTick ()
    {
        if (--_ticking > 0) {
            _ticking = 1;
            return true;
        }

        return false;
    }

    /**
     * The <code>IntervalManager</code> calls this method as often as
     * we've requested to obtain our desired frame rate.  Since we'd
     * like to avoid messy thread-synchronization between the AWT
     * thread and other threads, here we just add the tick task to the
     * AWT thread for later execution.
     */
    public void intervalExpired (int id, Object arg)
    {
        if (requestTick()) {
            // throw the tick task on the AWT thread task queue
            SwingUtilities.invokeLater(_ticker);
        }
    }

    /**
     * The <code>tick</code> method handles updating sprites and
     * repainting the target display.
     */
    protected void tick ()
    {
        // call tick on all sprites
        _spritemgr.tick();

        // invalidate screen-rects dirtied by sprites
        DirtyRectList rects = _spritemgr.getDirtyRects();
	if (rects.size() > 0) {
	    // pass the dirty-rects on to the scene view
	    _view.invalidateRects(rects);

	    // refresh the display
            _view.paintImmediately();
	}

	// update refresh-rate information
	//PerformanceMonitor.tick(AnimationManager.this, "refresh");

        if (finishedTick()) {
            // finishedTick returning true means there's been a
            // request for at least one more tick since we started
            // this tick, so we want to queue up another tick
            // immediately
            SwingUtilities.invokeLater(_ticker);
        }
    }

    public void checkpoint (String name, int ticks)
    {
        Log.info(name + " [ticks=" + ticks + "].");
    }

    /** The ticker runnable that we put on the AWT thread periodically. */
    protected Runnable _ticker;

    /** The desired number of refresh operations per second. */
    protected static final int FRAME_RATE = 70;

    /** The milliseconds to sleep to obtain desired frame rate. */
    protected static final long REFRESH_INTERVAL = 1000 / FRAME_RATE;

    /** The number of outstanding tick requests. */
    protected int _ticking = 0;

    /** The sprite manager. */
    protected SpriteManager _spritemgr;

    /** The view on which we are animating. */
    protected AnimatedView _view;
}
