
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.net.DefaultPrivateMessageResponder;
import net.usikkert.kouchat.net.MessageParser;
import net.usikkert.kouchat.net.DefaultMessageResponder;
import net.usikkert.kouchat.net.MessageResponder;
import net.usikkert.kouchat.net.Messages;
import net.usikkert.kouchat.net.PrivateMessageParser;
import net.usikkert.kouchat.net.PrivateMessageResponder;
import net.usikkert.kouchat.net.TransferList;
import net.usikkert.kouchat.util.DayTimer;

public class Controller
{
	private static Logger log = Logger.getLogger( Controller.class.getName() );

	private ChatState chatState;
	private NickController nickController;
	private Messages messages;
	private MessageParser msgParser;
	private PrivateMessageParser privmsgParser;
	private MessageResponder msgResponder;
	private PrivateMessageResponder privmsgResponder;
	private IdleThread idleThread;
	private TransferList tList;
	private WaitingList wList;
	private NickDTO me;

	public Controller( UserInterface ui )
	{
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			public void run()
			{
				logOff();
			}
		} );

		me = Settings.getSettings().getMe();

		nickController = new NickController();
		chatState = new ChatState();
		tList = new TransferList();
		wList = new WaitingList();
		idleThread = new IdleThread( this, ui );
		msgResponder = new DefaultMessageResponder( this, ui );
		privmsgResponder = new DefaultPrivateMessageResponder( this, ui );
		msgParser = new MessageParser( msgResponder );
		privmsgParser = new PrivateMessageParser( privmsgResponder );
		messages = new Messages();

		new DayTimer( ui );
	}

	public TopicDTO getTopic()
	{
		return chatState.getTopic();
	}

	public NickList getNickList()
	{
		return nickController.getNickList();
	}

	public boolean isWrote()
	{
		return chatState.isWrote();
	}

	public void changeWriting( int code, boolean writing )
	{
		nickController.changeWriting( code, writing );

		if ( code == me.getCode() )
		{
			chatState.setWrote( writing );

			if ( writing )
				messages.sendWritingMessage();
			else
				messages.sendStoppedWritingMessage();
		}
	}

	public void changeAwayStatus( int code, boolean away, String awaymsg ) throws CommandException
	{
		if ( code == me.getCode() && !isConnected() )
			throw new CommandException( "You tried to change away mode without being connected. This should never happen..." );
		else
			nickController.changeAwayStatus( code, away, awaymsg );
	}

	public boolean isNickInUse( String nick )
	{
		return nickController.isNickInUse( nick );
	}

	public boolean isNewUser( int code )
	{
		return nickController.isNewUser( code );
	}

	public void changeMyNick( String nick ) throws CommandException
	{
		if ( me.isAway() )
			throw new CommandException( "You tried to change nick while away. This should never happen..." );
		
		else
		{
			changeNick( me.getCode(), nick );
			messages.sendNickMessage();
			Settings.getSettings().saveSettings();
		}
	}

	public void changeNick( int code, String nick )
	{
		nickController.changeNick( code, nick );
	}

	public NickDTO getNick( int code )
	{
		return nickController.getNick( code );
	}

	public NickDTO getNick( String nick )
	{
		return nickController.getNick( nick );
	}

	private void sendLogOn()
	{
		messages.sendLogonMessage();
		messages.sendClient();
		messages.sendExposeMessage();
		messages.sendGetTopicMessage();
	}

	// Not sure if this is the best way to set if I'm logged on or not
	private void runDelayedLogon()
	{
		Timer timer = new Timer();
		timer.schedule( new DelayedLogonTask(), 0 );
	}

	public void logOn()
	{
		msgParser.start();
		privmsgParser.start();
		messages.start();
		sendLogOn();
		idleThread.start();
		runDelayedLogon();
	}

	public void logOff()
	{
		idleThread.stopThread();
		messages.sendLogoffMessage();
		messages.stop();
		msgParser.stop();
		privmsgParser.stop();
		wList.setLoggedOn( false );
	}

	public void sendExposeMessage()
	{
		messages.sendExposeMessage();
	}

	public void sendExposingMessage()
	{
		messages.sendExposingMessage();
	}

	public void sendGetTopicMessage()
	{
		messages.sendGetTopicMessage();
	}

	public void sendIdleMessage()
	{
		messages.sendIdleMessage();
	}

	public void sendChatMessage( String msg ) throws CommandException
	{
		if ( !isConnected() )
			throw new CommandException( "You tried to send a chat message without being connected. This should never happen..." );
		else if ( me.isAway() )
			throw new CommandException( "You tried to send a chat message while away. This should never happen..." );
		else if ( msg.trim().length() == 0 )
			throw new CommandException( "You tried to send an empty chat message. This should never happen..." );
		else if ( msg.length() > Constants.MESSAGE_MAX_CHARACTERS )
			throw new CommandException( "You tried to send a chat message with more than " + Constants.MESSAGE_MAX_CHARACTERS + " characters. This is not allowed..." );
		else
			messages.sendChatMessage( msg );
	}

	public void sendTopicMessage()
	{
		messages.sendTopicMessage( getTopic() );
	}

	public void changeTopic( String newTopic ) throws CommandException
	{
		if ( !isConnected() )
			throw new CommandException( "You tried to change the topic without being connected. This should never happen..." );
		else if ( me.isAway() )
			throw new CommandException( "You tried to change the topic while away. This should never happen..." );

		else
		{
			long time = System.currentTimeMillis();
			TopicDTO topic = getTopic();
			topic.changeTopic( newTopic, me.getNick(), time );
			sendTopicMessage();
		}
	}

	public void sendAwayMessage()
	{
		messages.sendAwayMessage();
	}

	public void sendBackMessage()
	{
		messages.sendBackMessage();
	}

	public void sendNickCrashMessage( String nick )
	{
		messages.sendNickCrashMessage( nick );
	}

	public void sendFileAbort( int msgCode, int fileHash, String fileName )
	{
		messages.sendFileAbort( msgCode, fileHash, fileName );
	}

	public void sendFileAccept( int msgCode, int port, int fileHash, String fileName )
	{
		messages.sendFileAccept( msgCode, port, fileHash, fileName );
	}

	public void sendFile( int sendToUserCode, long fileLength, int fileHash, String fileName ) throws CommandException
	{
		if ( !isConnected() )
			throw new CommandException( "You tried to send a file without being connected. This should never happen..." );
		else if ( me.isAway() )
			throw new CommandException( "You tried to send a file while away. This should never happen..." );
		else
			messages.sendFile( sendToUserCode, fileLength, fileHash, fileName );
	}

	public TransferList getTransferList()
	{
		return tList;
	}

	public WaitingList getWaitingList()
	{
		return wList;
	}

	public boolean restart()
	{
		messages.restart();

		if ( msgParser.restart() )
		{
			if ( !isConnected() )
			{
				runDelayedLogon();
				sendLogOn();
			}

			return true;
		}

		return false;
	}

	public void sendClientInfo()
	{
		messages.sendClient();
	}
	
	public void sendPrivateMessage( String privmsg, String userIP, int userPort, int userCode ) throws CommandException
	{
		if ( !isConnected() )
			throw new CommandException( "You tried to send a private chat message without being connected. This should never happen..." );
		else if ( me.isAway() )
			throw new CommandException( "You tried to send a private chat message while away. This should never happen..." );
		else if ( privmsg.trim().length() == 0 )
			throw new CommandException( "You tried to send an empty private chat message. This should never happen..." );
		else if ( privmsg.length() > Constants.MESSAGE_MAX_CHARACTERS )
			throw new CommandException( "You tried to send a private chat message with more than " + Constants.MESSAGE_MAX_CHARACTERS + " characters. This is not allowed..." );
		else if ( userPort == 0 )
			throw new CommandException( "You tried to send a private chat message to a user with no available port number. This should never happen..." );
		else
			messages.sendPrivateMessage( privmsg, userIP, userPort, userCode );
	}
	
	public void changeNewMessage( int code, boolean newMsg )
	{
		nickController.changeNewMessage( code, newMsg );
	}
	
	public boolean isConnected()
	{
		return chatState.isConnected();
	}

	public void setConnected( boolean connected )
	{
		chatState.setConnected( connected );
	}
	
	private class DelayedLogonTask extends TimerTask
	{
		@Override
		public void run()
		{
			try
			{
				Thread.sleep( 800 );
			}

			catch ( InterruptedException e )
			{
				log.log( Level.SEVERE, e.getMessage(), e );
			}

			if ( isConnected() )
				wList.setLoggedOn( true );
		}
	}
}