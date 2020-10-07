/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TransactionInProgressException;
import javax.resource.spi.ConnectionEvent;

public class FVTMessageConsumer implements MessageConsumer {
    private FVTSession session;
    private String tableName;

    FVTMessageConsumer(FVTSession session, Destination destination) throws JMSException {
        this.session = session;

        if (destination instanceof Queue)
            tableName = ((Queue) destination).getQueueName();
        else if (destination instanceof Topic)
            tableName = ((Topic) destination).getTopicName();
        else
            throw new InvalidDestinationException("Destination: " + destination);

        if (session.transactionInProgress)
            throw new TransactionInProgressException("Not allowed during transaction");

        try {
            Statement stmt = session.con.mc.con.createStatement();
            try {
                stmt.executeUpdate("create table " + tableName + " (messageID integer primary key generated always as identity, messageText varchar(200))");
            } catch (SQLException x) {
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        }
    }

    @Override
    public void close() throws JMSException {
        session = null;
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMessageSelector() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message receive() throws JMSException {
        return receive(0); // never times out
    }

    @Override
    public Message receive(long timeout) throws JMSException {
        try {
            if (!session.transactionInProgress) {
                session.con.mc.con.setAutoCommit(false);
                session.con.mc.notify(ConnectionEvent.LOCAL_TRANSACTION_STARTED, session.con, null);
                session.transactionInProgress = true;
            }

            PreparedStatement pstmt = session.con.mc.con.prepareStatement("select messageID, messageText from " + tableName + " order by messageID asc");
            try {
                for (long timeElapsed = Long.MIN_VALUE, start = System.currentTimeMillis(); timeout == 0 || timeElapsed < timeout; timeElapsed = System.currentTimeMillis() - start) {
                    ResultSet result = pstmt.executeQuery();
                    while (result.next()) {
                        Object messageID = result.getString(1);
                        String text = result.getString(2);
                        PreparedStatement ps = session.con.mc.con.prepareStatement("delete from " + tableName + " where messageID=?");
                        try {
                            ps.setObject(1, messageID);
                            if (ps.executeUpdate() == 1) {
                                TextMessage message = new FVTTextMessage();
                                message.setText(text);
                                return message;
                            } // else someone else got it first
                        } finally {
                            ps.close();
                        }
                    }
                    result.close();

                    Thread.sleep(200);
                }
            } finally {
                try {
                    pstmt.close();
                } catch (SQLException x) {
                }
            }
        } catch (InterruptedException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        } catch (NullPointerException x) {
            if (session != null)
                throw x;
        } catch (SQLException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        }
        return null;
    }

    @Override
    public Message receiveNoWait() throws JMSException {
        return receive(-1);
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        throw new UnsupportedOperationException();
    }
}
