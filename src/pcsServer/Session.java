package pcsServer;

import encryption.EncryptionHandler;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Session {
	private String username;
	private Socket socket;
	private PrintWriter outputWriter;
	private BufferedReader inputBuffer;
	private KeyPair keyPair;

	Session(Socket socket) {
		this.keyPair = EncryptionHandler.generateECKeys();
		this.socket = socket;
		try {
			inputBuffer = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			outputWriter = new PrintWriter(this.socket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public void write(String message) {
		//String decrypted = decryptMessage(message);
		outputWriter.println(message);
		outputWriter.flush();
	}

	public String read() {
		String line = null;
		try {
			line = inputBuffer.readLine();
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return line;
	}

	public boolean disconnect() {
		try {
			socket.close();
			inputBuffer.close();
		} catch (IOException e) {
			System.out.println(e);
			return false;
		}
		outputWriter.close();
		return true;
	}

	PublicKey getPublicKey(){
		return keyPair.getPublic();
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Socket getSocket() {
		return socket;
	}

	public String getUsername() {
		return username;
	}
}