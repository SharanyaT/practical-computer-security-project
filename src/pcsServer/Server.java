package pcsServer;

import encryption.EncryptionHandler;

import java.net.*;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;


public class Server {
		
	private ServerSocket listener;
	
	//for now only use 2 clients since the chat works by broadcasting messages
	private ArrayList<Session> clientList; 
	public static HashMap<String,String> clientListKeys = new HashMap<String,String>();
	private DBManager db;
	
	
	Server(short port) {
		try {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			listener = new ServerSocket(port);
			clientList  = new ArrayList<Session>();
		} catch(IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public void run() throws IOException {
		
		String dbUser = "root";
		String dbPass = "snickerdoodle";

		String dbAddress = "jdbc:mysql://localhost:3306/chatdb";
		db = new DBManager(dbAddress, dbUser, dbPass);		
		
		while (true) {
			Socket client = listener.accept();				
			new Thread(new ClientHandler(client, clientList, db)).start();
		}
	}
	
	public static void main(String args[]) {
		
		final short PORT = 1337;
		Server server = new Server(PORT);
		
		try {
			server.run();
		} catch (IOException e) {
			System.err.print(e);
			e.printStackTrace();
		}
	}
	
}

class ClientHandler implements Runnable {
	private Session client;
	
	private ArrayList<Session> clientList;
	String newClientKey;
	private DBManager db;
	
	ClientHandler(Socket socket, ArrayList<Session> cl, DBManager database) {
		client = new Session(socket);
		this.clientList = cl;
		db = database;
	}
	
	public void run() {
		String clientMsg = null;
		boolean accepted = false;
		
		do {
			clientMsg = client.read();
			if (clientMsg.equals("QUIT")) {
				client.disconnect();
				return;
			}
			else if (clientMsg.startsWith("PUBLICKEY: ")) {
				newClientKey = clientMsg.split(" ")[1];// Server.clientListKeys.add(clientMsg.split(" ")[1]);
				
			}
			else if (clientMsg.startsWith("NEWUSER: ")) {
				createUser(clientMsg);
			}
			else if (clientMsg.startsWith("LOGIN: ")) {
				accepted = authenticate(clientMsg);
			}
			else
			{
				client.disconnect();
				return;
			}
		} while(!accepted);
		
		while (true) {
			
			String line = client.read();
			if (line == null) break;
			if(line.contains("give key"))
				sendPublicKey(line);
			else
				sendToRecipient(line);
		}
		
		exit();
	}
	
	private void sendPublicKey(String message) {
		String uname = message.split(" ")[3];
		broadcast("USERKEY: "+Server.clientListKeys.get(uname));
	}

	private void sendToRecipient(String message) {
		String[] parts = message.split(":");
		String recipient = null;
		if (parts.length > 1){
			recipient = parts[0].substring(message.indexOf("O")+2, message.indexOf(":"));
			message = "FROM: "+parts[1] + ":"+parts[2];
		}
		for (Session to: clientList){
			 //If no user name is specified, broadcast to all users. Otherwise, only send to that user. recipient == null ||
			if ( to.getUsername().equals(recipient)){
				sendMessage(message, to);
			}
		}
		
	}

	private synchronized void createUser(String clientMsg) {
		
		clientMsg = clientMsg.split(" ")[1];
		String username = clientMsg.split(",")[0];
		String password = clientMsg.split(",")[1];
		
		try {
			if (db.userExists(username)) {
				client.write("TAKEN");
			}
			else {
				db.createUser(username, password);			
				client.write("USERCREATED");
			}
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	
	private synchronized boolean authenticate(String clientMsg) {
		boolean accepted = false;
		
		clientMsg = clientMsg.split(" ")[1];
		String username = clientMsg.split(",")[0];
		String password = clientMsg.split(",")[1];
		
		try {
			if (db.authenticate(username, password)) {
				accepted = true;
				
	            client.setUsername(username);
	            client.write("ACCEPTED");
	            //sendMessage("ACCEPTED",client,Server.clientListKeys.get(clientList.indexOf(client)));
	            
	            //client gets added to client list 
	            // client key must also get added here
	            clientList.add(client);
	            Server.clientListKeys.put(client.getUsername(),newClientKey);
	    		updateClientUserList();
	    		
	            broadcast(client.getUsername() + " has joined the chat.");
	           // broadcast("PUBLICKEYNEWUSER: "+newClientKey);
			}
			else client.write("DENIED");
			
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
		
		return accepted;
	}
	
	private synchronized void exit() {
		String exitMsg = client.getUsername() +" has left the chat.";
		
		broadcast(exitMsg);

		client.disconnect();
		clientList.remove(client);
		updateClientUserList();
	}

	private synchronized void broadcast(String message) {
		/*String[] parts = message.split(":");
		String recipient = null;
		if (parts.length > 1){
			recipient = parts[0];
			message = message.substring(recipient.length());
		}*/
		for (Session to: clientList){
			// If no username is specified, broadcast to all users. Otherwise, only send to that user.
			//if (recipient == null || to.getUsername().equals(recipient)){
			if(to.getUsername().equals(client.getUsername()))	
			sendMessage(message, to);
			//}
		}
	}

	private synchronized void sendMessage(String message, Session to){
		//String encrypted = client.encryptMessage(message, to.getPublicKey());
		//String msg = EncryptionHandler.publicKeyToString(client.getPublicKey()) + ":" + message;
		to.write(message);
	}
	
	private synchronized void updateClientUserList() {
            String userList = "USERLIST:";
            for (int i = 0; i < clientList.size(); i++) {
            	userList += " " + clientList.get(i).getUsername();
            }
            broadcast(userList);
	}
}