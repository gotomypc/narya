//
// $Id: ResourceBundle.java,v 1.1 2001/11/20 00:21:41 mdb Exp $

package com.threerings.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A resource bundle provides access to the resources in a jar file.
 */
public class ResourceBundle
{
    /**
     * Constructs a resource bundle with the supplied jar file.
     *
     * @param source a file object that references our source jar file.
     *
     * @exception IOException thrown if an error occurs reading our jar
     * file.
     */
    public ResourceBundle (File source)
        throws IOException
    {
        _source = new JarFile(source);
    }

    /**
     * Fetches the named resource from this bundle. The path should be
     * specified as a relative, platform independent path (forward
     * slashes). For example <code>sounds/scream.au</code>.
     *
     * @param path the path to the resource in this jar file.
     *
     * @return an input stream from which the resource can be loaded or
     * null if no such resource exists.
     *
     * @exception IOException thrown if an error occurs locating the
     * resource in the jar file.
     */
    public InputStream getResource (String path)
        throws IOException
    {
        // TBD: determine whether or not we need to convert the path into
        // a platform-dependent path if we're on Windows
        JarEntry entry = _source.getJarEntry(path);
        InputStream stream = null;
        if (entry != null) {
            stream = _source.getInputStream(entry);
        }
        return stream;
    }

    /** The jar file from which we load resources. */
    protected JarFile _source;
}
