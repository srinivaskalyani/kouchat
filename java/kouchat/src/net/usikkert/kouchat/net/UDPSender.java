
/***************************************************************************
 *   Copyright 2006-2008 by Christian Ihle                                 *
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

package net.usikkert.kouchat.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.misc.ErrorHandler;
import net.usikkert.kouchat.util.Loggers;

/**
 * Sends UDP packets directly to a user. Useful for private chat,
 * where not everyone should get the packets.
 *
 * @author Christian Ihle
 */
public class UDPSender
{
	private static final Logger LOG = Loggers.NETWORK_LOG;

	private DatagramSocket udpSocket;
	private boolean connected;
	private final ErrorHandler errorHandler;

	/**
	 * Default constructor.
	 */
	public UDPSender()
	{
		errorHandler = ErrorHandler.getErrorHandler();
	}

	/**
	 * Sends a packet with a message to a user.
	 *
	 * @param message The message to send.
	 * @param ip The ip address of the user.
	 * @param port The port to send the message to.
	 * @return If the message was sent or not.
	 */
	public boolean send( final String message, final String ip, final int port )
	{
		if ( connected )
		{
			try
			{
				InetAddress address = InetAddress.getByName( ip );
				byte[] encodedMsg = message.getBytes( Constants.MESSAGE_CHARSET );
				int size = encodedMsg.length;

				if ( size > Constants.NETWORK_PACKET_SIZE )
				{
					LOG.log( Level.WARNING, "Message was " + size + " bytes, which is too large.\n"
							+ " The receiver might not get the complete message.\n'" + message + "'" );
				}

				DatagramPacket packet = new DatagramPacket( encodedMsg, size, address, port );
				udpSocket.send( packet );
				LOG.log( Level.FINEST, "Sent message: " + message );

				return true;
			}

			catch ( final IOException e )
			{
				LOG.log( Level.SEVERE, "Could not send message: " + message );
			}
		}

		return false;
	}

	/**
	 * Closes the UDP socket.
	 */
	public void stopSender()
	{
		LOG.log( Level.FINE, "Disconnecting..." );

		if ( !connected )
		{
			LOG.log( Level.FINE, "Not connected." );
		}

		else
		{
			connected = false;

			if ( udpSocket != null && !udpSocket.isClosed() )
			{
				udpSocket.close();
			}

			LOG.log( Level.FINE, "Disconnected." );
		}
	}

	/**
	 * Creates a new UDP socket.
	 */
	public void startSender()
	{
		LOG.log( Level.FINE, "Connecting..." );

		if ( connected )
		{
			LOG.log( Level.FINE, "Already connected." );
		}

		else
		{
			try
			{
				udpSocket = new DatagramSocket();
				connected = true;
				LOG.log( Level.FINE, "Connected." );
			}

			catch ( final IOException e )
			{
				LOG.log( Level.SEVERE, e.toString(), e );
				errorHandler.showError( "Failed to initialize network:\n" + e
						+ "\n\nYou will not be able to send private messages!" );
			}
		}
	}
}
