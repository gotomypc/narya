//
// $Id: MiCasaFrame.java,v 1.2 2001/10/09 17:47:33 mdb Exp $

package com.threerings.micasa.client;

import java.awt.BorderLayout;
import javax.swing.*;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;

/**
 * The micasa frame contains the user interface for the MiCasa client
 * application. It divides the screen into space for the primary panel and
 * the side-bar controls and allows user interface elements to be placed
 * in either region.
 */
public class MiCasaFrame
    extends JFrame implements ControllerProvider
{
    public MiCasaFrame ()
    {
        super("MiCasa Client");
        // for now, quit if we're closed
        // setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Sets the panel that makes up the entire client display.
     */
    public void setPanel (JPanel panel)
    {
        // remove the old panel
        getContentPane().removeAll();
	// add the new one
	getContentPane().add(panel, BorderLayout.CENTER);
        // swing doesn't properly repaint after adding/removing children
        validate();
    }

    /**
     * Sets the match-making panel to be used when the lobby panel is
     * being displayed.
     */
    public void setMatchMakingPanel (JPanel panel)
    {
        _lopanel.setPrimary(panel);
    }

    /**
     * Provides a reference to the lobby panel that will be used to
     * provide our lobby interfaces.
     */
    protected void setLobbyPanel (LobbyPanel panel)
    {
        _lopanel = panel;
    }

    /**
     * Sets the controller for the outermost scope. This controller will
     * handle all actions that aren't handled by controllers of tigher
     * scope.
     */
    public void setController (Controller controller)
    {
        _controller = controller;
    }

    // documentation inherited
    public Controller getController ()
    {
        return _controller;
    }

    protected Controller _controller;

    protected LobbyPanel _lopanel;
}
