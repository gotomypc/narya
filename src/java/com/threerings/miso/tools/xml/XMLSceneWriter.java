//
// $Id: XMLSceneWriter.java,v 1.13 2001/09/21 02:30:35 mdb Exp $

package com.threerings.miso.scene.xml;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;

import com.samskivert.util.ListUtil;

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import com.megginson.sax.DataWriter;

import com.threerings.media.tile.Tile;

import com.threerings.miso.Log;
import com.threerings.miso.scene.*;

/**
 * The <code>XMLSceneWriter</code> writes a {@link
 * com.threerings.miso.scene.MisoScene} object to an XML file.
 *
 * <p> The scene id is omitted as the scene id is assigned when the
 * scene template is actually loaded into a server.  Similarly,
 * portals are named and bound to their target scene ids later.
 */
public class XMLSceneWriter extends DataWriter
{
    /**
     * Construct an XMLSceneWriter object.
     */
    public XMLSceneWriter (IsoSceneViewModel model)
    {
	_model = model;
        setIndentStep(2);
    }

    /**
     * Write the scenes to the specified output file in XML format.
     *
     * @param fname the file to write to.
     */
    public void saveScene (MisoScene scene, String fname) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(fname);
        setOutput(new OutputStreamWriter(fos));

        try {
            startDocument();

            startElement("scene");

            dataElement("name", scene.getName());
            dataElement("version", "" + XMLSceneVersion.VERSION);
            dataElement("locations", getLocationData(scene));

	    startElement("clusters");
	    writeClusters(scene);
	    endElement("clusters");

            dataElement("portals", getPortalData(scene));

            startElement("tiles");
            for (int lnum = 0; lnum < MisoScene.NUM_LAYERS; lnum++) {
                writeLayer(scene, lnum);
            }
            endElement("tiles");

            endElement("scene");
            endDocument();

        } catch (SAXException saxe) {
            Log.warning("Exception writing scene to file " +
                        "[scene=" + scene + ", fname=" + fname +
                        ", saxe=" + saxe + "].");
        }
    }

    /**
     * Output XML detailing the cluster objects.  Clusters are
     * described by a list of location object indexes that are
     * contained within the cluster.
     *
     * @param scene the scene object.
     */
    protected void writeClusters (MisoScene scene) throws SAXException
    {
	Cluster[] clusters = scene.getClusters();
	Location[] locs = scene.getLocations();

	int size = clusters.length;
	for (int ii = 0; ii < size; ii++) {
	    Cluster cluster = clusters[ii];
	    ArrayList clusterlocs = cluster.getLocations();

	    StringBuffer buf = new StringBuffer();
	    int clustersize = clusterlocs.size();
	    for (int jj = 0; jj < clustersize; jj++) {
                Location cloc = (Location)clusterlocs.get(jj);
		buf.append(ListUtil.indexOfEqual(locs, cloc));
		if (jj < clustersize - 1) {
                    buf.append(",");
                }
	    }

	    dataElement("cluster", buf.toString());
	}
    }

    /**
     * Output XML detailing the tiles at the specified layer in the
     * given scene.  The first layer is outputted in its entirety,
     * whereas subsequent layers are outputted in a sparse notation
     * that details each contiguous horizontal chunk of tiles in each
     * row separately. 
     *
     * @param scene the scene object.
     * @param lnum the layer number.
     */
    protected void writeLayer (MisoScene scene, int lnum) throws SAXException
    {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "lnum", "", "CDATA", "" + lnum);

        startElement("", "layer", "", attrs);
        for (int yy = 0; yy < _model.scenehei; yy++) {
            if (lnum == MisoScene.LAYER_BASE) {
                writeTileRowData(scene, yy, lnum);
            } else {
                writeSparseTileRowData(scene, yy, lnum);
            }
        }
        endElement("layer");
    }

    /**
     * Return a string representation of the portals in the scene.  Each
     * portal is specified by a comma-delimited tuple of (location
     * index, portal name) values.
     *
     * @param scene the scene object.
     *
     * @return the portals in String format.
     */
    protected String getPortalData (MisoScene scene)
    {
	Location[] locs = scene.getLocations();
	Portal[] portals = scene.getPortals();

        StringBuffer buf = new StringBuffer();
	int size = portals.length;
        for (int ii = 0; ii < size; ii++) {
	    Portal portal = portals[ii];
	    buf.append(ListUtil.indexOfEqual(locs, portal)).append(",");
	    buf.append(portal.name);
            if (ii < size - 1) {
                buf.append(",");
            }
        }

        return buf.toString();
    }

    /**
     * Return a string representation of the locations in the scene.
     * Each location is specified by a comma-delimited triplet of
     * (x, y, orientation) values.
     *
     * @return the locations in String format.
     */
    protected String getLocationData (MisoScene scene)
    {
	Location[] locs = scene.getLocations();

        StringBuffer buf = new StringBuffer();
	int size = locs.length;
        for (int ii = 0; ii < size; ii++) {
	    Location loc = locs[ii];
	    buf.append(loc.x).append(",");
	    buf.append(loc.y).append(",");
	    buf.append(loc.orient);
            if (ii < size - 1) {
                buf.append(",");
            }
        }

        return buf.toString();
    }

    /**
     * Return a string representation of the tiles at the specified
     * row and layer in the given scene.  Only <code>len</code> tiles
     * starting at column <code>colstart</code> are included in the
     * string.
     *
     * @param scene the scene object.
     * @param rownum the row number.
     * @param lnum the layer number.
     * @param colstart the first column of data.
     * @param len the number of columns of data.
     *
     * @return the tile data in String format.
     */
    protected String getTileData (MisoScene scene, int rownum, int lnum,
                                  int colstart, int len)
    {
        StringBuffer buf = new StringBuffer();
        Tile[][][] tiles = scene.getTiles();

        int numtiles = colstart + len;
        for (int ii = colstart; ii < numtiles; ii++) {
            Tile tile = tiles[ii][rownum][lnum];
            if (tile == null) {
                Log.warning("Null tile [x=" + ii + ", rownum=" + rownum +
                            ", lnum=" + lnum + "].");
                continue;
            }

            buf.append(tile.tsid).append(",");
            buf.append(tile.tid);
            if (ii != numtiles - 1) {
                buf.append(",");
            }
        }

        return buf.toString();
    }

    /**
     * Write the row data for a specified row in the scene tile array.
     *
     * <p> The row is written as a <code>row</code> element.  Each
     * tile is specified by a comma-delimited tuple of (tile set id,
     * tile id) numbers, with the associated row number detailed in
     * the <code>rownum</code> attribute of the <code>row</code>
     * element.
     *
     * @param scene the scene object.
     * @param rownum the row in the scene tile array.
     * @param lnum the layer number in the scene tile array.
     */
    protected void writeTileRowData (MisoScene scene, int rownum, int lnum)
        throws SAXException
    {
        // set up the attributes for this row
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "rownum", "", "CDATA", "" + rownum);

        // output the full row data element
        String data = getTileData(scene, rownum, lnum, 0, _model.scenewid);
        dataElement("", "row", "", attrs, data);
    }

    /**
     * Utility routine used by <code>writeSparseTileRowData</code> to
     * obtain the sets of contiguous tile sets in each row.
     *
     * <p> The search for contiguous tiles starts at the column
     * specified in <code>info[0]</code.
     *
     * <p> Results are returned in the <code>info</code> array as
     * <code>{ colstart, len }</code>.  colstart is -1 if no
     * tiles were found in the rest of the row.
     *
     * @param scene the scene object.
     * @param rownum the row number.
     * @param lnum the layer number.
     * @param info the info array.
     *
     * @return true if any tiles were found, false if not.
     */
    protected boolean getSparseColumn (
        MisoScene scene, int rownum, int lnum, int info[])
    {
        Tile[][][] tiles = scene.getTiles();
        int start = -1, len = 0;
        for (int xx = info[0]; xx < _model.scenewid; xx++) {
            Tile tile = tiles[xx][rownum][lnum];
            if (tile == null) {
                if (start == -1) {
                    continue;
                } else {
                    break;
                }
            }

            if (start == -1) {
                start = xx;
            }
            len++;
        }

        info[0] = start;
        info[1] = len;

        return (start != -1);
    }

    /**
     * Write the row data for a specified row in the scene tile array
     * in sparse row format.
     *
     * <p> Sparse row format is identical to the format detailed in
     * <code>writeTileRowData()</code> except that a separate
     * <code>row</code> element is outputted for each contiguous set
     * of tiles, with the starting column detailed in the
     * <code>colstart</code> attribute of the <code>row</code>
     * element.
     *
     * @param scene the scene object.
     * @param rownum the row in the scene tile array.
     * @param lnum the layer number in the scene tile array.
     */
    protected void
        writeSparseTileRowData (MisoScene scene, int rownum, int lnum)
        throws SAXException
    {
        // set up the attributes for this row
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "rownum", "", "CDATA", "" + rownum);
        attrs.addAttribute("", "colstart", "", "CDATA", "0");

        int info[] = new int[] { 0, 0 };
        while (getSparseColumn(scene, rownum, lnum, info)) {
            // update the colstart attribute
            attrs.setAttribute(1, "", "colstart", "", "CDATA", "" + info[0]);

            // output the partial row data element
            String data = getTileData(scene, rownum, lnum, info[0], info[1]);
            dataElement("", "row", "", attrs, data);

            // update the colstart value
            info[0] = info[0] + info[1];
        }
    }

    /** The iso scene view data model. */
    protected IsoSceneViewModel _model;
}
