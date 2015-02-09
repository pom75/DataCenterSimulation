package fr.upmc.components.registry;

// Copyright Jacques Malenfant, Univ. Pierre et Marie Curie.
// 
// Jacques.Malenfant@lip6.fr
// 
// This software is a computer program whose purpose is to provide a
// basic component programming model to program with components
// distributed applications in the Java programming language.
// 
// This software is governed by the CeCILL-C license under French law and
// abiding by the rules of distribution of free software.  You can use,
// modify and/ or redistribute the software under the terms of the
// CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
// URL "http://www.cecill.info".
// 
// As a counterpart to the access to the source code and  rights to copy,
// modify and redistribute granted by the license, users are provided only
// with a limited warranty  and the software's author,  the holder of the
// economic rights,  and the successive licensors  have only  limited
// liability. 
// 
// In this respect, the user's attention is drawn to the risks associated
// with loading,  using,  modifying and/or developing or reproducing the
// software by the user in light of its specific status of free software,
// that may mean  that it is complicated to manipulate,  and  that  also
// therefore means  that it is reserved for developers  and  experienced
// professionals having in-depth computer knowledge. Users are therefore
// encouraged to load and test the software's suitability as regards their
// requirements in conditions enabling the security of their systems and/or 
// data to be ensured and,  more generally, to use and operate it in the 
// same conditions as regards security. 
// 
// The fact that you are presently reading this means that you have had
// knowledge of the CeCILL-C license and that you accept its terms.

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.upmc.components.cvm.config.ConfigurationFileParser;
import fr.upmc.components.cvm.config.ConfigurationParameters;

/**
 * The class <code>GlobalRegistry</code> implements the global registry for the
 * component model that registers connection information to remotely access
 * components through their ports.
 *
 * <p><strong>Description</strong></p>
 * 
 * The Registry implements a global registry for the component model allowing
 * to bind port URI to information required for the connection between
 * components through ports.  The registry must be run on one host which
 * name is given in the static variable <code>REGISTRY_HOSTNAME</code>.  It
 * listens to request on a port which number is given by the static variable
 * <code>REGISTRY_PORT</code>
 * 
 * Protocol (spaces are used to split the strings, so they are meaningful):
 * 
 * <pre>
 * Requests              Responses
 * 
 * lookup <key>          ok <value>
 *                       nok
 * put <key> <value>     ok
 *                       nok bound!
 * remove <key>          ok
 *                       nok not_bound!
 * shutdown              ok
 * <anything else>       nok unkonwn_command!
 * </pre>
 * 
 * When the static variable <code>DEBUG</code> is set to true, the registry
 * provides with a log on STDOUT of the commands it executes.
 *  
 * <p><strong>Invariant</strong></p>
 * 
 * <pre>
 * invariant	true
 * </pre>
 * 
 * <p>Created on : 2012-10-22</p>
 * 
 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
 * @version	$Name$ -- $Revision$ -- $Date$
 */
public class			GlobalRegistry
{
	/** Default name of the host running the registry; is configurable.		*/
	public static String				REGISTRY_HOSTNAME = "localhost" ;
	/** Registry port number listen for commands; is configurable.			*/
	public static int					REGISTRY_PORT = 55252 ;

	/** If true, an echo of some commands executed is provided on STDOUT.	*/
	public static final boolean			DEBUG0 = true ;
	/** If true, a log of the commands executed is provided on STDOUT.		*/
	public static final boolean			DEBUG1 = false ;

	/** Directory of information, a hashtable with String keys and values.	*/
	protected Hashtable<String,String>	directory ;
	/** Configuration parameters from the configuration file.				*/
	protected ConfigurationParameters	configurationParameters ;
	/** Number of JVM in the current distributed component virtual machine.	*/
	protected final int					numberOfJVMsInDCVM ;

	/** The server socket used to listen on the port number REGISTRY_PORT.	*/
	protected ServerSocket	ss ;

	protected static final int			MAX_NUMBER_OF_THREADS = 100 ; 
	/** The executor service in charge of handling component requests.		*/
	protected static ExecutorService	REQUEST_HANDLER ;
	/**	synchroniser to finish the execution of this global registry.		*/
	protected CountDownLatch			finished ;

	// ------------------------------------------------------------------------
	// Tasks for the executor framework
	// ------------------------------------------------------------------------

	/**
	 * The class <code>ProcessLookup</code> implements a runnable task used to
	 * look up the registry for a given key.
	 *
	 * <p><strong>Description</strong></p>
	 * 
	 * <p><strong>Invariant</strong></p>
	 * 
	 * <pre>
	 * invariant	true
	 * </pre>
	 * 
	 * <p>Created on : 2012-10-22</p>
	 * 
	 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
	 * @version	$Name$ -- $Revision$ -- $Date$
	 */
	protected static class	ProcessLookup
	implements Runnable
	{

		protected PrintStream				ps ;
		protected String					key ;
		protected Hashtable<String,String>	directory ;

		public				ProcessLookup(
			PrintStream ps,
			String key,
			Hashtable<String,String> directory
			)
		{
			super();
			this.ps = ps ;
			this.key = key ;
			this.directory = directory ;
		}


		@Override
		public void		run()
		{
			String result = null ;
			synchronized(this.directory) {
				result = this.directory.get(this.key) ;
			}
			if (result == null) {
				this.ps.println("nok") ;
			} else {
				if (DEBUG1) {
					System.out.println("GLobal registry looking up " +
									   this.key + " found " + result) ;
				}
				this.ps.println("ok " + result) ;
			}
		}
	}

	/**
	 * The class <code>ProcessPut</code> implements a runnable task used to
	 * update the registry with the association of a given key to a given
	 * value.
	 *
	 * <p><strong>Description</strong></p>
	 * 
	 * <p><strong>Invariant</strong></p>
	 * 
	 * <pre>
	 * invariant	true
	 * </pre>
	 * 
	 * <p>Created on : 2012-10-22</p>
	 * 
	 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
	 * @version	$Name$ -- $Revision$ -- $Date$
	 */
	protected static class	ProcessPut implements Runnable {

		protected PrintStream				ps ;
		protected String					key ;
		protected String					value ;
		protected Hashtable<String,String>	directory ;

		public				ProcessPut(
			PrintStream ps,
			String key,
			String value,
			Hashtable<String,String> directory
			)
		{
			super();
			this.ps = ps ;
			this.key = key;
			this.value = value;
			this.directory = directory;
		}

		@Override
		public void run() {
			String result = null ;
			synchronized (this.directory) {
				result = this.directory.get(key) ;
			}
			if (result != null) {
				ps.println("nok bound!") ;
			} else {
				this.directory.put(this.key, this.value) ;
				ps.println("ok") ;
			}
		}		
	}

	/**
	 * The class <code>ProcessRemove</code> implements a runnable task used to
	 * remove the association of a given key from the registry.
	 *
	 * <p><strong>Description</strong></p>
	 * 
	 * <p><strong>Invariant</strong></p>
	 * 
	 * <pre>
	 * invariant	true
	 * </pre>
	 * 
	 * <p>Created on : 2012-10-22</p>
	 * 
	 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
	 * @version	$Name$ -- $Revision$ -- $Date$
	 */
	protected static class	ProcessRemove implements Runnable
	{
		protected PrintStream				ps ;
		protected String					key ;
		protected Hashtable<String,String>	directory ;

		public				ProcessRemove(
			PrintStream ps,
			String key,
			Hashtable<String, String> directory
			)
		{
			super();
			this.ps = ps ;
			this.key = key;
			this.directory = directory;
		}

		@Override
		public void run() {
			String result = null ;
			synchronized (this.directory) {
				result = this.directory.get(key) ;
			}
			if (result == null) {
				ps.println("nok not_bound!") ;
			} else {
				this.directory.remove(this.key) ;
				ps.println("ok") ;
			}
		}		
	}

	/**
	 * The class <code>ServiceRunnable</code> implements the behaviour of the
	 * registry exchanging with one client; its processes the requests from the
	 * clients until the latter explicitly disconnects with a "shutdown" request
	 * of implicitly with a null string request.
	 *
	 * <p><strong>Description</strong></p>
	 * 
	 * <p><strong>Invariant</strong></p>
	 * 
	 * <pre>
	 * invariant	true
	 * </pre>
	 * 
	 * <p>Created on : 30 janv. 2014</p>
	 * 
	 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
	 * @version	$Name$ -- $Revision$ -- $Date$
	 */
	protected static class	ServiceRunnable
	implements	Runnable
	{
		protected Hashtable<String,String>	directory ;
		protected Socket					s ;
		protected BufferedReader			br ;
		protected PrintStream				ps ;
		protected CountDownLatch			finished ;

		public			ServiceRunnable(
			Socket 						s,
			Hashtable<String,String>	directory,
			CountDownLatch				finished
			)
		{
			if (DEBUG1) { System.out.println("Registry creating a service runnable") ; }
			this.s = s ;
			this.directory = directory ;
			this.finished = finished ;
			try {
				this.br = new BufferedReader(
						new InputStreamReader(this.s.getInputStream())) ;
				this.ps = new PrintStream(s.getOutputStream(), true) ;
			} catch (IOException e) {
				e.printStackTrace();
				if (DEBUG1) { System.out.println("...service runnable created") ; }
			}
		}

		@Override
		public void		run()
		{
			if (DEBUG1) {
				System.out.println("Service runnable running...") ;
			}
			String message = null ;
			try {
				message = br.readLine() ;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (DEBUG0) {
				System.out.println("GlobalRegistry processing " + message) ;
			}
			String[] tokens = message.split("\\s") ;
			while (message != null && !tokens[0].equals("shutdown")) {
				if (tokens[0].equals("lookup")) {
					new ProcessLookup(this.ps, tokens[1], this.directory).run() ;
				} else if (tokens[0].equals("put")) {
					new ProcessPut(this.ps, tokens[1], tokens[2], this.directory).run() ;
				} else if (tokens[0].equals("remove")) {
					new ProcessRemove(this.ps, tokens[1], this.directory).run() ;
				} else {
					ps.println("nok unkonwn_command!") ;
				}
				try {
					message = br.readLine() ;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				if (DEBUG0) {
					System.out.println("GlobalRegistry processing " + message) ;
				}
				if (message != null) {
					tokens = message.split("\\s") ;
					if (DEBUG1) {
						System.out.println(
								"GlobalRegistry next command " + tokens[0] +
								" " + (!tokens[0].equals("shutdown"))) ;
					}
				}
			}
			try {
				this.ps.print("ok") ;
				this.ps.close() ;
				this.br.close() ;
				s.close() ;
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (DEBUG1) {
				System.out.println("GlobalRegistry exits ") ;
			}
			this.finished.countDown() ;
		}
	}

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	/**
	 * create a registry object, using the configuration file to know the number
	 * of clients that will connect, and therefore that will have to disconnect
	 * for the registry to terminate its execution.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param configFileName
	 * @throws Exception
	 */
	public				GlobalRegistry(String configFileName) throws Exception
	{
		super();

		File configFile = new File(configFileName) ;
		ConfigurationFileParser cfp = new ConfigurationFileParser() ;
		if (!cfp.validateConfigurationFile(configFile)) {
			throw new Exception("invalid configuration file " + configFileName) ;
		}
		this.configurationParameters = cfp.parseConfigurationFile(configFile) ;
		this.numberOfJVMsInDCVM =
								this.configurationParameters.getJvms().length ;

		this.directory =
					new Hashtable<String,String>(this.numberOfJVMsInDCVM) ;
		REQUEST_HANDLER =
					Executors.newFixedThreadPool(this.numberOfJVMsInDCVM) ;
		this.finished = new CountDownLatch(this.numberOfJVMsInDCVM) ;
		try {
			this.ss = new ServerSocket(REGISTRY_PORT) ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	/**
	 * runs the registry, repeated accepting connections on its server socket,
	 * decoding the request (in the format defined by the above protocol),
	 * executing it and returning the result (in the format defined by the
	 * above protocol) on the output stream of the socket.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 */
	public void			run()
	{
		Socket s = null ;
		if (DEBUG0) {
			System.out.println("Registry up and running!") ;
		}
		int count = 0 ;
		while (count < this.numberOfJVMsInDCVM) {
			try {
				REQUEST_HANDLER.submit(new ServiceRunnable(ss.accept(),
														   this.directory,
														   this.finished)) ;
				count++ ;
				if (DEBUG1) {
					System.out.println(
							"Global registry accepted a new connection.") ;
				}
			} catch (IOException e) {
				try {
					if (s != null) { s.close() ; } ;
					ss.close() ;
				} catch (IOException e1) {
					;
				}
				e.printStackTrace();
			}
		}
		if (DEBUG0) {
			System.out.println("All connected!") ;
		}
		try {
			this.ss.close() ;
		} catch (IOException e) {
			;
		}
	}

	// ------------------------------------------------------------------------
	// Main method
	// ------------------------------------------------------------------------

	/**
	 * initialise and run the registry.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param args
	 */
	public static void	main(String[] args) {
		GlobalRegistry reg;
		try {
			reg = new GlobalRegistry(args[0]);
			reg.run() ;
			reg.finished.await() ;
			if (DEBUG0) {
				System.out.println("Global registry shuts down!") ;
			}
			REQUEST_HANDLER.shutdownNow() ;
			System.exit(0) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
