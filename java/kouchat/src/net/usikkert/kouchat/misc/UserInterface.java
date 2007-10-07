
/***************************************************************************
 *   Copyright 2006-2007 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package net.usikkert.kouchat.misc;

import java.io.File;

import net.usikkert.kouchat.net.FileReceiver;

public interface UserInterface
{
	public void showSystemMessage( String message );
	public void showUserMessage( String user, String message, int color );
	public boolean askFileSave( String user, String fileName, String size );
	public File showFileSave( String fileName );
	public void showTransfer( FileReceiver fileRes );
	public void showTopic();
	public void clearChat();
	public void showOwnMessage( String message );
	public void changeAway( boolean away, String reason );
	public void startFileSend( NickDTO user, File file );
	public void fixTopic( String args );
}
