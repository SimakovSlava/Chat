package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        //который должен отправлять сообщение message всем соединениям из connectionMap.
        for (Connection connection : connectionMap.values()) {
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Извините, сообщение не было отправлено.");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Введите порт сервера - ");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Сервер запущен.");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске сервера");
        }

    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Было установлено соеденинение с " + socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене давнными с удаленным доступом");
            }

            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто. Е бой");
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            //серверное рукопожатие
            while (true) {
                ConsoleHelper.writeMessage("Введите имя пользователя - ");
                connection.send(new Message(MessageType.NAME_REQUEST));

                Message message = connection.receive();

                if (message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Полученное имя, не соответсвует. Введите заново.");
                    continue;
                }

                String userName = message.getData();
                if (userName.isEmpty()) {
                    ConsoleHelper.writeMessage("Вы ввели пустое сообщение. Введите заново.");
                    continue;
                }

                if (connectionMap.containsKey(userName)) {
                    ConsoleHelper.writeMessage("Такое имя уже зарегистрировано.");
                    continue;
                }

                connectionMap.put(userName, connection);

                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (name.equals(userName))
                    continue;
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                StringBuilder stringBuilder = new StringBuilder();
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    stringBuilder.append(userName).append(": ").append(message.getData());
                    sendBroadcastMessage(new Message(MessageType.TEXT, stringBuilder.toString()));
                } else ConsoleHelper.writeMessage("Попробкйте ввести нормальное текстовое сообщение");
            }
        }
    }
}
