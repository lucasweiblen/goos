package com.belano.auctionsniper;

import com.belano.auctionsniper.ui.MainWindow;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static javax.swing.SwingUtilities.invokeAndWait;

public class Main {

    private static final int ARG_HOSTNAME = 0;
    private static final int ARG_PORT = 1;
    private static final int ARG_USERNAME = 2;
    private static final int ARG_PASSWORD = 3;
    private static final int ARG_ITEM_ID = 4;

    public static final String AUCTION_RESOURCE = "Auction";
    public static final String ITEM_ID_AS_LOGIN = "auction-%s";
    public static final String AUCTION_ID_FORMAT = ITEM_ID_AS_LOGIN + "@%s/" + AUCTION_RESOURCE;

    public static final String JOIN_COMMAND_FORMAT = "";
    public static final String BID_COMMAND_FORMAT = "SOLVersion: 1.1; Command: BID; Price: %d;";

    private MainWindow ui;
    private Chat notToBeGCd;

    public static void main(String... args) throws Exception {
        Main main = new Main();
        String hostname = args[ARG_HOSTNAME];
        String port = args[ARG_PORT];
        String username = args[ARG_USERNAME];
        String password = args[ARG_PASSWORD];
        String itemId = args[ARG_ITEM_ID];
        main.joinAuction(
                connectTo(hostname, port, username, password),
                itemId
        );
    }

    public Main() throws Exception {
        startUserInterface();
    }

    private void joinAuction(XMPPConnection connection, String itemId) throws XMPPException {
        disconnectWhenUICloses(connection);
        final Chat chat = connection.getChatManager()
                .createChat(
                        auctionId(itemId, connection), null);
        this.notToBeGCd = chat;
        Auction auction = new XMPPAuction(chat);
        String username = getUsernameFrom(connection);
        chat.addMessageListener(
                new AuctionMessageTranslator(username, new AuctionSniper(auction, new SniperStateDisplayer()))
        );
        auction.join();
    }

    private String getUsernameFrom(XMPPConnection connection) {
        return connection.getUser().substring(0, connection.getUser().indexOf('@'));
    }

    private void disconnectWhenUICloses(XMPPConnection connection) {
        ui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                connection.disconnect();
            }
        });
    }

    private void startUserInterface() throws Exception {
        invokeAndWait(() -> ui = new MainWindow());
    }

    private static XMPPConnection connectTo(String hostname, String port, String username, String password) throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(hostname, Integer.parseInt(port));
        XMPPConnection connection = new XMPPConnection(config);
        connection.connect();
        connection.login(username, password, AUCTION_RESOURCE);
        return connection;
    }

    private static String auctionId(String itemId, XMPPConnection connection) {
        // "auction-item-xxxxx@serviceName/Auction"
        return String.format(AUCTION_ID_FORMAT, itemId, connection.getServiceName());
    }

    public class SniperStateDisplayer implements SniperListener {
        @Override
        public void sniperLost() {
            showStatus(MainWindow.STATUS_LOST);
        }

        @Override
        public void sniperBidding() {
            showStatus(MainWindow.STATUS_BIDDING);
        }

        @Override
        public void sniperWinning() {
            showStatus(MainWindow.STATUS_WINNING);
        }

        @Override
        public void sniperWon() {
            showStatus(MainWindow.STATUS_WON);
        }

        private void showStatus(String status) {
            SwingUtilities.invokeLater(() -> ui.showStatus(status));
        }
    }

}
