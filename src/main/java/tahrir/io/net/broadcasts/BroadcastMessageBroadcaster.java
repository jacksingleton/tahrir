package tahrir.io.net.broadcasts;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tahrir.TrNode;
import tahrir.io.net.PhysicalNetworkLocation;
import tahrir.io.net.TrPeerManager;
import tahrir.io.net.broadcasts.broadcastMessages.BroadcastMessage;

import java.util.Map;
import java.util.Set;

/**
 * Schedules a single microblog for broadcast to each peer one at a time.
 *
 * @author Kieran Donegan <kdonegan.92@gmail.com>
 */
public class BroadcastMessageBroadcaster implements Runnable {
	private static Logger log = LoggerFactory.getLogger(BroadcastMessageBroadcaster.class);

	private final TrNode node;
    private boolean disabled = false;

    public BroadcastMessageBroadcaster(final TrNode node) {
		this.node = node;
	}

    @Override
    public void run() {
        if (!disabled) {
            Set<PhysicalNetworkLocation> peersThatReceiveMessageBroadcasts = findPeersThatReceiveMessageBroadcasts(node.peerManager.peers);
            final BroadcastMessage broadcastMessageForBroadcast = node.mbClasses.mbsForBroadcast.getMessageForBroadcast();
            for (PhysicalNetworkLocation recepient : peersThatReceiveMessageBroadcasts) {
                final TransmitMicroblogSessionImpl localMicroblogBroadcastSession = node.sessionMgr.getOrCreateLocalSession(TransmitMicroblogSessionImpl.class);
                localMicroblogBroadcastSession.attemptToSendMicroblogAndWaitUntilComplete(broadcastMessageForBroadcast, recepient);

            }
        }
    }

    private Set<PhysicalNetworkLocation> findPeersThatReceiveMessageBroadcasts(final Map<PhysicalNetworkLocation, TrPeerManager.TrPeerInfo> peers) {
        Set<PhysicalNetworkLocation> peersThatReceiveMessageBroadcasts = Sets.newHashSet();
        for (Map.Entry<PhysicalNetworkLocation, TrPeerManager.TrPeerInfo> peerEntry : peers.entrySet()) {
            if (peerEntry.getValue().capabilities.receivesMessageBroadcasts) {
                peersThatReceiveMessageBroadcasts.add(peerEntry.getKey());
            }
        }
        return peersThatReceiveMessageBroadcasts;
    }

    public void disable() {
        this.disabled = true;
    }
}
