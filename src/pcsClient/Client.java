package pcsClient;

import encryption.EncryptionHandler;
import encryption.OneTimeKeyQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class Client {
    private Socket server;
    private PrintWriter outputWriter;
    private BufferedReader inputBuffer;
    private String username;
    private KeyPair keyPair;
    private String UserBKey;
    
    public Client(){
    	Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    	this.keyPair = EncryptionHandler.generateECKeys();
    }
    public boolean login(String user, String pass) {
        boolean accepted = false;
        
        outputWriter.println("PUBLICKEY: "+EncryptionHandler.publicKeyToString(this.keyPair.getPublic()));
        outputWriter.println("LOGIN: " + user + "," + pass);
        outputWriter.flush();
        String response;
        try {
            response = inputBuffer.readLine();
            if(response.equals("ACCEPTED")) {
                accepted = true;
                username = user;
            }
        } catch(IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        
        return accepted;
    }
    
    public void connect(String ip, short port) throws ConnectException, UnknownHostException, IOException {
        server = new Socket(ip, port);
        try {
            
            inputBuffer = new BufferedReader(new InputStreamReader(server.getInputStream()));
            outputWriter = new PrintWriter(server.getOutputStream());
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }    
    }

    
    public boolean disconnect() {
        try {
            server.close();
            inputBuffer.close();
        } catch(IOException e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
        outputWriter.close();
        return true;
    }
    
    public void write(String msg) {
    	String message=null;
    	String toUser = null;
    	if(msg.startsWith("TO"))
    	{
    		//toUser = msg.substring(msg.indexOf("O")+1,msg.indexOf(":"));
    		toUser = msg.substring(0,msg.indexOf(":"));
        	String encrypted = encryptMessage(msg.split(":")[1], EncryptionHandler.publicKeyFromString(UserBKey));
    		message = (EncryptionHandler.publicKeyToString(this.keyPair.getPublic())) + ":" + encrypted;
    		 outputWriter.println(toUser+":"+message);
    	}
    	else
    	 outputWriter.println(msg);
        outputWriter.flush();
    }
    
    public String read() {
        String line = null;
        String userMessage =null;
        try {
            line = inputBuffer.readLine();
            if(line.startsWith("USERKEY: "))
            {
            	UserBKey=line.split(" ")[1];
            	return "Chat session initiated";
            }
            if(line.startsWith("FROM: "))
            {
            	userMessage=line.split(" ")[1];
            	return decryptMessage(userMessage);
            }
        } catch(IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return line;
    }
    
    String encryptMessage(String msg, PublicKey recipientPublic){
		SecretKey secretKey = EncryptionHandler.generateSharedSecret(keyPair.getPrivate(), recipientPublic);
		return EncryptionHandler.encryptString(secretKey, msg);
	}

	String decryptMessage(String msg){
		String key = msg.split(":")[0];
		PublicKey senderPublic = EncryptionHandler.publicKeyFromString(key);
		SecretKey secretKey = EncryptionHandler.generateSharedSecret(keyPair.getPrivate(), senderPublic);
		return EncryptionHandler.decryptString(secretKey, msg.split(":")[1]);
	}
    
    public void sendChatMessage(String msg) {
        write(msg);
    }
    
    public void sendQuitMessage() {
        write("QUIT");
    }
}
