package NetClient;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class NetClient extends JFrame {
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private String nickname;
	private Color nicknameColor;

	private JTextPane chatPane;
	private JTextField inputField;
	private JButton sendButton;
	private JPanel emojiPanel;

	private Map<String, String> emojiMap = new HashMap<>();

	public NetClient() {
		setTitle("Chat Client");
		setSize(450, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		initializeEmojiMap();
		setupNicknameAndColor();
		setupUI();
		connectToServer();
	}

	private void initializeEmojiMap() {
		emojiMap.put("smile", "😊");
		emojiMap.put("laugh", "😂");
		emojiMap.put("heart", "❤️");
		emojiMap.put("like", "👍");
		emojiMap.put("fire", "🔥");
		emojiMap.put("party", "🎉");
		emojiMap.put("sad", "😢");
		emojiMap.put("angry", "😠");
	}

	private void setupNicknameAndColor() {
		nickname = JOptionPane.showInputDialog(this, "Enter your nickname:", "Nickname", JOptionPane.PLAIN_MESSAGE);
		if (nickname == null || nickname.trim().isEmpty()) {
			nickname = "User" + (int)(Math.random() * 1000);
		}

		nicknameColor = JColorChooser.showDialog(this, "Choose your nickname color", Color.BLUE);
		if (nicknameColor == null) {
			nicknameColor = Color.BLUE;
		}
	}

	private void setupUI() {
		// Chat area
		chatPane = new JTextPane();
		chatPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(chatPane);
		add(scrollPane, BorderLayout.CENTER);

		// Emoji panel
		emojiPanel = new JPanel(new FlowLayout());
		for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
			JButton emojiButton = new JButton(entry.getValue());
			emojiButton.addActionListener(e -> inputField.setText(inputField.getText() + entry.getValue()));
			emojiPanel.add(emojiButton);
		}
		add(emojiPanel, BorderLayout.NORTH);

		// Input panel
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputField = new JTextField();
		inputField.addActionListener(e -> sendMessage());

		sendButton = new JButton("Send");
		sendButton.addActionListener(e -> sendMessage());

		inputPanel.add(inputField, BorderLayout.CENTER);
		inputPanel.add(sendButton, BorderLayout.EAST);
		add(inputPanel, BorderLayout.SOUTH);
	}

	private void connectToServer() {
		try {
			socket = new Socket("localhost", 1234);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Send nickname and color to server
			out.println("NICK:" + nickname);
			out.println("COLOR:" + nicknameColor.getRGB());

			// Start message listener thread
			new Thread(this::listenForMessages).start();

			setVisible(true);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not connect to server", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	private void listenForMessages() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
				String[] parts = message.split("\\|", 3);
				if (parts.length == 3) {
					String sender = parts[0];
					Color color = new Color(Integer.parseInt(parts[1]));
					String msg = parts[2];

					appendToChat(sender, color, msg);
				}
			}
		} catch (IOException e) {
			appendToChat("System", Color.RED, "Disconnected from server");
		}
	}

	private void appendToChat(String sender, Color color, String message) {
		SwingUtilities.invokeLater(() -> {
			StyledDocument doc = chatPane.getStyledDocument();

			SimpleAttributeSet senderStyle = new SimpleAttributeSet();
			StyleConstants.setForeground(senderStyle, color);
			StyleConstants.setBold(senderStyle, true);

			SimpleAttributeSet messageStyle = new SimpleAttributeSet();
			StyleConstants.setForeground(messageStyle, Color.BLACK);

			try {
				doc.insertString(doc.getLength(), sender + ": ", senderStyle);
				doc.insertString(doc.getLength(), message + "\n", messageStyle);
				chatPane.setCaretPosition(doc.getLength());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		});
	}

	private void sendMessage() {
		String message = inputField.getText().trim();
		if (!message.isEmpty()) {
			out.println(message);
			inputField.setText("");
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new NetClient());
	}
}