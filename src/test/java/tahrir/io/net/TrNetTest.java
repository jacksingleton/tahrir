package tahrir.io.net;

import java.net.InetAddress;
import java.security.interfaces.*;

import org.testng.annotations.Test;

import tahrir.TrNode;
import tahrir.io.crypto.TrCrypto;
import tahrir.io.net.TrRemoteConnection.State;
import tahrir.io.net.udp.*;
import tahrir.io.net.udp.UdpNetworkInterface.Config;
import tahrir.tools.Tuple2;

public class TrNetTest {
	@Test
	public void simpleTest() throws Exception {
		final Config conf1 = new Config();
		conf1.listenPort = 3912;
		conf1.maxUpstreamBytesPerSecond = 1024;

		final Config conf2 = new Config();
		conf2.listenPort = 3913;
		conf2.maxUpstreamBytesPerSecond = 1024;

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp1 = TrCrypto.createRsaKeyPair();

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp2 = TrCrypto.createRsaKeyPair();
		final UdpNetworkInterface iface1 = new UdpNetworkInterface(conf1, kp1);

		final UdpNetworkInterface iface2 = new UdpNetworkInterface(conf2, kp2);


		final TrNode<UdpRemoteAddress> node1 = new TrNode<UdpRemoteAddress>(iface1);
		final TrNet<UdpRemoteAddress> trn1 = new TrNet<UdpRemoteAddress>(node1);

		trn1.registerSessionClass(TestSession.class, TestSessionImpl.class);

		final TrNode<UdpRemoteAddress> node2 = new TrNode<UdpRemoteAddress>(iface2);
		final TrNet<UdpRemoteAddress> trn2 = new TrNet<UdpRemoteAddress>(node2);

		trn2.registerSessionClass(TestSession.class, TestSessionImpl.class);

		final TrRemoteConnection<UdpRemoteAddress> one2two = trn1.connectTo(
				new UdpRemoteAddress(InetAddress.getLocalHost(), conf2.listenPort), kp2.a, false);

		final TrRemoteConnection<UdpRemoteAddress> two2one = trn2.connectTo(
				new UdpRemoteAddress(InetAddress.getLocalHost(), conf1.listenPort), kp1.a, false);

		while (!one2two.getState().equals(State.CONNECTED) || !two2one.getState().equals(State.CONNECTED)) {
			Thread.sleep(100);
		}

		final TestSession remoteSession = trn1.getRemoteSession(TestSession.class, one2two, 1234, 1.0);

		remoteSession.testMethod(0);

		Thread.sleep(1000);
	}

	@Test
	public void testUnsolicited() throws Exception {
		final Config conf1 = new Config();
		conf1.listenPort = 3942;
		conf1.maxUpstreamBytesPerSecond = 1024;

		final Config conf2 = new Config();
		conf2.listenPort = 3943;
		conf2.maxUpstreamBytesPerSecond = 1024;

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp1 = TrCrypto.createRsaKeyPair();

		final Tuple2<RSAPublicKey, RSAPrivateKey> kp2 = TrCrypto.createRsaKeyPair();
		final UdpNetworkInterface iface1 = new UdpNetworkInterface(conf1, kp1);

		final UdpNetworkInterface iface2 = new UdpNetworkInterface(conf2, kp2);

		final TrNode<UdpRemoteAddress> node1 = new TrNode<UdpRemoteAddress>(iface1);
		final TrNet<UdpRemoteAddress> trn1 = new TrNet<UdpRemoteAddress>(node1);

		trn1.registerSessionClass(TestSession.class, TestSessionImpl.class);

		final TrNode<UdpRemoteAddress> node2 = new TrNode<UdpRemoteAddress>(iface2);
		final TrNet<UdpRemoteAddress> trn2 = new TrNet<UdpRemoteAddress>(node2);

		trn2.enableUnexpectedInboundConnections();

		trn2.registerSessionClass(TestSession.class, TestSessionImpl.class);

		final TrRemoteConnection<UdpRemoteAddress> one2two = trn1.connectTo(
				new UdpRemoteAddress(InetAddress.getLocalHost(), conf2.listenPort), kp2.a, true);


		while (!one2two.getState().equals(State.CONNECTED)) {
			Thread.sleep(100);
		}

		final TestSession remoteSession = trn1.getRemoteSession(TestSession.class, one2two, 1234, 1.0);

		remoteSession.testMethod(0);

		Thread.sleep(1000);
	}

	public static interface TestSession extends TrSession {
		public void testMethod(int param);
	}

	public static class TestSessionImpl extends TrSessionImpl implements TestSession {

		public TestSessionImpl(final Integer sessionId, final TrNode<?> node, final TrNet<?> trNet) {
			super(sessionId, node, trNet);
		}

		public void testMethod(final int param) {
			System.out.println(node.networkInterface + " testMethod(" + param + ")");
			if (param < 10) {
				final TestSession remote = trNet.getOrCreateRemoteSession(TestSession.class, getSender(), 1.0);
				remote.testMethod(param + 1);
			}
		}

	}
}
