//
// $Id: TileUtil.java,v 1.7 2001/10/15 23:53:43 shaper Exp $

package com.threerings.miso.tile;

import java.awt.Image;

import com.threerings.media.sprite.MultiFrameImage;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.tile.*;

import com.threerings.miso.Log;
import com.threerings.miso.scene.AmbulatorySprite;

/**
 * Tile-related utility functions.
 */ 
public class TileUtil
{
    /**
     * Returns an array of multi-frame images corresponding to the
     * frames of animation used to render the ambulatory sprite in
     * each of the directions it may face.  The tileset id referenced
     * must contain <code>Sprite.NUM_DIRECTIONS</code> rows of tiles,
     * with each row containing <code>frameCount</code> tiles.
     *
     * @param tilemgr the tile manager to retrieve tiles from.
     * @param tsid the tileset id containing the sprite tiles.
     *
     * @return the array of multi-frame sprite images.
     */
    public static MultiFrameImage[]
        getAmbulatorySpriteFrames (TileManager tilemgr, int tsid,
                                   int frameCount)
    {
        MultiFrameImage[] anims = new MultiFrameImage[Sprite.NUM_DIRECTIONS];

	try {
	    for (int ii = 0; ii < Sprite.NUM_DIRECTIONS; ii++) {
		Tile[] tiles = new Tile[frameCount];

		for (int jj = 0; jj < frameCount; jj++) {
		    int idx = (ii * frameCount) + jj;
		    tiles[jj] = tilemgr.getTile(tsid, idx);
		}

		anims[ii] = new MultiTileImage(tiles);
	    }

	} catch (TileException te) {
	    Log.warning("Exception retrieving ambulatory sprite tiles " +
			"[te=" + te + "].");
	    return null;
	}

        return anims;
    }

    /**
     * A class that treats an array of tiles as source images for a
     * multi-frame animation to be used by the sprite engine.
     */
    protected static class MultiTileImage implements MultiFrameImage
    {
        public MultiTileImage (Tile[] tiles)
        {
            _tiles = tiles;
        }

        public int getFrameCount ()
        {
            return _tiles.length;
        }

        public Image getFrame (int index)
        {
            return _tiles[index].img;
        }

        protected Tile[] _tiles;
    }
}
