package org.opensextant.giscore.test.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Ignore;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;

/**
 * A test server for testing SSL requests.
 */
@Ignore
public class SslTestServer extends Server {

	private final Connector secureConnector;

	public SslTestServer( File base, String remotePathFragment ) throws IOException {
		super( 0 );

		if ( !base.isDirectory() )
		{
			throw new IllegalArgumentException( "Specified base directory does not exist: " + base.getCanonicalPath() );
		}

		HandlerCollection handlers = new HandlerCollection();
		setHandler( handlers );

		Context context = new Context( handlers, remotePathFragment );
		handlers.addHandler( new DefaultHandler() );

		context.addServlet( DefaultServlet.class, "/" );
		context.setResourceBase( base.getCanonicalPath() );

		secureConnector = createSecureConnector();
		getServer().addConnector(secureConnector);
	}

	public int getPort()
	{
		return secureConnector.getLocalPort();
	}

	private Connector createSecureConnector() {
		SslSocketConnector connector = new SslSocketConnector();
		//connector.setPort(HTTPS_PORT);
		URL keystoreUrl = getClass().getResource("keystore");
		connector.setKeystore(keystoreUrl.getFile());
		// System.out.println("storeUrl=" + keystoreUrl);
		connector.setTruststore(connector.getKeystore());
		connector.setKeyPassword("test");
		connector.setTrustPassword("test123");
		return connector;
	}

	public static void main(String[] args) {
		try {
			SslTestServer server = new SslTestServer(new File("data"), "/");
			server.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}