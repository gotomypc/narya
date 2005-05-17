//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2005 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.parlor.data;

/**
 * This should be implemented some user-interface element that allows
 * the user to configure whichever TableConfig options are relevant.
 */
public interface TableConfigurator
{
    /**
     * If true, the tableConfigurator is empty, useful if the lobby code
     * can skip the step of letting the table creator configure things
     * if the game configurator is also empty.
     */
    public boolean isEmpty ();

    /**
     * Return the fully configured table config when the user is ready
     * to create their table.
     */
    public TableConfig getTableConfig ();
}