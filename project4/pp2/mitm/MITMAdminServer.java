/**
 * CSE 490K Project 2
 */

package mitm;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.naming.AuthenticationException;
import java.security.GeneralSecurityException;

// You need to add code to do the following
// 1) use SSL sockets instead of the plain sockets provided
// 2) check user authentication
// 3) perform the given administration command

class MITMAdminServer implements Runnable
{
    private ServerSocket m_serverSocket;
    private Socket m_socket = null;
    private HTTPSProxyEngine m_engine;
    private PrintWriter m_out;

    public MITMAdminServer( String localHost, int adminPort, HTTPSProxyEngine engine ) throws IOException, GeneralSecurityException {
	MITMSocketFactory socketFactory =
	    new MITMSSLSocketFactory();
	m_serverSocket = socketFactory.createServerSocket( localHost, adminPort, 0 );
	m_engine = engine;
    }

    public void run() {
	System.out.println("Admin server initialized, listening on port " + m_serverSocket.getLocalPort());
	while( true ) {
	    try {
		m_socket = m_serverSocket.accept();
                m_out = new PrintWriter(m_socket.getOutputStream(),true);

                byte[] buffer = new byte[40960];

		Pattern userPwdPattern =
		    Pattern.compile("username:(\\S+)\\s+password:(\\S+)\\s+command:(\\S+)\\sCN:(\\S*)\\s");

		BufferedInputStream in =
		    new BufferedInputStream(m_socket.getInputStream(),
					    buffer.length);

		// Read a buffer full.
		int bytesRead = in.read(buffer);

		String line =
		    bytesRead > 0 ?
		    new String(buffer, 0, bytesRead) : "";

		Matcher userPwdMatcher =
		    userPwdPattern.matcher(line);

		// parse username and pwd
		if (userPwdMatcher.find()) {
		    String userName = userPwdMatcher.group(1);
		    String password = userPwdMatcher.group(2);
		    System.out.println("Try to authenticate as: "+userName+" with password: "+password);
		    IPasswordManager passwordManager = new EncryptedFileBasedPasswordManager();

		    // authenticate
		    // if authenticated, do the command
		    boolean authenticated = passwordManager.authenticate(userName,password);
		    if( authenticated ) {
			String command = userPwdMatcher.group(3);
			String commonName = userPwdMatcher.group(4);

			doCommand( command, m_out );
		    }
		    else {
			throw new AuthenticationException("Couldn't authenticate user: "+userName);
		    }
		}
	    }
	    catch( InterruptedIOException e ) {
	    }
	    catch( Exception e ) {
		e.printStackTrace();
	    }
	}
    }

    // TODO implement the commands
    private void doCommand( String cmd, PrintWriter out ) throws IOException {

	if (cmd.contains("shutdown")){
            m_out.println("Shutdown command received");
            m_engine.shutdown();

        }
	else if (cmd.contains("stats")){

            String statInfo = "Statistics query on number of proxied SSL connections returns: "+m_engine.getSSLConnectionCount();
            out.println(statInfo);
	}

        m_out.close();
	m_socket.close();

    }

}
