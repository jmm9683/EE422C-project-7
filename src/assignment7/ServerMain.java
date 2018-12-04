package assignment7;

import java.io.*;
import java.net.*;
import java.util.*;


public class ServerMain {
	private static List<ChatRoom> openChats;
	private static Map<String, ClientObserver> userObservers;
	private static String delim = Character.toString((char) 31);
	private static String userDelim = Character.toString((char) 29);
	private static String fileName = "Accounts" + File.separator + "users.txt";
	private boolean addedNewUsers = false;
	private ServerSocket serverSocket;

	public static void main(String[] args) {
		try {
			new ServerMain().start();
		} catch (Exception e) { e.printStackTrace(); }


	}


	public ServerMain() throws FileNotFoundException, IOException {
		openChats = new ArrayList<ChatRoom>();
		userObservers = new HashMap<String, ClientObserver>();


		new File("Accounts").mkdir();

		File yourFile = new File(fileName);


		try {
			yourFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	public void start() throws Exception {
		this.serverSocket = new ServerSocket(4001); 
		while (true) { 
			Socket clientSocket = serverSocket.accept();
			ClientObserver writer = new ClientObserver(clientSocket.getOutputStream());
			Thread clientThread = new Thread(new ClientHandler(clientSocket, writer)); 
			clientThread.start(); 
		}
	}


	class ClientHandler implements Runnable {
		private BufferedReader reader;
		private ClientObserver writer;
		private Socket socket;

		public ClientHandler(Socket clientSocket, ClientObserver writer) {
			this.socket = clientSocket;

			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
				this.writer = writer;
			} 
			catch (IOException e) { 
				e.printStackTrace();
			}
		}

		public void run() {
			String message;
			try {
				while ((message = reader.readLine()) != null) {
					String[] parsedMSG = message.split(delim);

					
					if(isNumeric(parsedMSG[0])) {
						openChats.get(Integer.parseInt(parsedMSG[0])).sendMessage(message);
					}
					
					else if (parsedMSG[0].equals("CLOSECHAT")) {
						for(ChatRoom room: openChats) {
							if(room.ID == Integer.parseInt(parsedMSG[1])) {
								room.sendMessage("" + Integer.toString(room.ID) + delim + "SERVER" + delim + "CHAT ENDED. CLOSE WINDOW");
								room.users.clear();
								room.setID(-1);
								break;
							}
						}
						
					}


					else if(parsedMSG[0].equals("NEWCHAT")) {
						String[] users = parsedMSG[2].split(userDelim);

				
						String[] names = new String[users.length + 1];
						names[0] = parsedMSG[1];
						for(int i = 0; i< users.length; i++) {
							names[i+1] = users[i];
						}

						boolean exist = false;
						int ID = -1;
						for(ChatRoom room: openChats) {
							if(room.users.containsAll(Arrays.asList(names)) && room.users.size() == names.length) {
								exist = true;
								ID = room.ID;
								break;
							}
						}

						if(!exist) {
							ChatRoom newChat = new ChatRoom();
							openChats.add(newChat);
							newChat.setID(openChats.indexOf(newChat));
							newChat.addUsers(parsedMSG);
							newChat.sendMessage("" + Integer.toString(newChat.ID) + delim + "SERVER" + delim + "New chat created.");
						}
					}

					
					else if(parsedMSG[0].equals("CREATE")) {
						String user = parsedMSG[1];
						String pwd = parsedMSG[2];

						boolean userExists = false;
						Scanner lines = new Scanner(new FileReader(fileName));
						while(lines.hasNext()) {
							String line = lines.nextLine();
							if (line.toUpperCase().contains((parsedMSG[1]).toUpperCase())) {
								userExists = true;
							}
						}
						lines.close();
						if(userObservers.containsKey(user) || userExists) {
							userObservers.put("ERRORNAME", this.writer);
							String error = "NOTUNIQUE" + delim + "ERRORNAME" + delim +  "ERRORNAME";
							String[] tempArray = error.split(delim);
							ChatRoom temp = new ChatRoom();
							temp.addUsers(tempArray);
							temp.sendMessage(error);
							userObservers.remove("ERRORNAME");
						}
						else {
							userObservers.put(user, this.writer);
							addedNewUsers = true;
							PrintWriter out = new PrintWriter(new FileWriter(fileName, true));
							out.println(user.toUpperCase() + "|" + pwd);
							out.close();
						}

					}

					else if(parsedMSG[0].equals("LOGIN")) {

						String user = parsedMSG[1];
						String pwd = parsedMSG[2];
						if(userObservers.containsKey(user)) {
							userObservers.put("ERROR", this.writer);
							String errorMSG = "ALREADYACTIVE" + delim + "ERRORNAME" + delim +  "ERRORNAME";
							String[] errorUSER = errorMSG.split(delim);
							ChatRoom errorROOM = new ChatRoom();
							errorROOM.addUsers(errorUSER);
							errorROOM.sendMessage(errorMSG);
							userObservers.remove("ERROR");

						}
						else {
							boolean userExists = false;
							Scanner lines = new Scanner(new FileReader(fileName));
							while(lines.hasNext()) {
								String line = lines.nextLine();
								if (line.equals((parsedMSG[1]).toUpperCase() + "|" + pwd)) {
									userExists = true;
								}
							}

							if(userExists) {
								userObservers.put(user, this.writer);
								addedNewUsers = true;
							} else {
								userObservers.put("ERROR", this.writer);
								String errorMSG = "WRONGPSWD" + delim + "ERRORNAME" + delim +  "ERRORNAME";
								String[] errorUSER = errorMSG.split(delim);
								ChatRoom errorROOM = new ChatRoom();
								errorROOM.addUsers(errorUSER);
								errorROOM.sendMessage(errorMSG);
								userObservers.remove("ERROR");
							}


							lines.close();
						}
					}

					else if(parsedMSG[0].equals("ACTIVATE")) {

						if(addedNewUsers) {
							addedNewUsers = false;
							Set<String> userList = 	userObservers.keySet();
							String names = "";
							for(String user: userList) {
								names += user + userDelim;
							}
							names = ("ACTIVATE" + delim + parsedMSG[1] + delim + names);
							String[] loginUSER = names.split(delim);
							ChatRoom loginROOM = new ChatRoom();
							loginROOM.addUsers(loginUSER);
							loginROOM.sendMessage(names);
						}
					}

					else if(parsedMSG[0].equals("LOGOUT")) {
						for(ChatRoom room: openChats) {
							if(room.users.contains(parsedMSG[1])) {
								room.sendMessage("" + Integer.toString(room.ID) + delim + "SERVER" + delim + "CHAT ENDED. CLOSE WINDOW");
								room.users.clear();
								room.setID(-1);
								break;
							}
						}
						userObservers.remove(parsedMSG[1]);
						addedNewUsers = true;
					}
				}
			} catch (IOException e) {

				e.printStackTrace();

			}
		}

			public boolean isNumeric(String str) {  
				try {  
					Double.parseDouble(str);  
				}  
				catch(NumberFormatException nfe) {  
					return false;  
				}  
				return true;  
			}
		}


		class ChatRoom extends Observable {

			public List<String> users = new ArrayList<String>();
			private int ID;

			public void setID(int ID) {
				this.ID = ID;
			}

			public void addUsers(String[] user) {
				if(userObservers.containsKey(user[1])) {
					addObserver(userObservers.get(user[1]));
					users.add(user[1]);
				}
				if(user.length > 2){
					String[] otherUsers = user[2].split(userDelim);
					for(int i = 0; i < otherUsers.length; i++) {
						addObserver(userObservers.get(otherUsers[i]));
						users.add(otherUsers[i]);
					}
				}
			}


			public void sendMessage(String message) {
				setChanged();
				notifyObservers(message);
			}
		}

	}