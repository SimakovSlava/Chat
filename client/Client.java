package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected;

    protected String getServerAddress() {
        /*должен запросить ввод адреса сервера у пользователя и вернуть введенное значение.
          Адрес может быть строкой, содержащей ip, если клиент и сервер запущен на разных машинах
          или 'localhost', если клиент и сервер работают на одной машине.*/
        ConsoleHelper.writeMessage("Введите адрес сервера пользователя - ");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        //должен запрашивать ввод порта сервера и возвращать его.
        ConsoleHelper.writeMessage("Введите номер порта сервера - ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        //должен запрашивать и возвращать имя пользователя.
        ConsoleHelper.writeMessage("Введите имя пользователя - ");
        return ConsoleHelper.readString();
    }

    public class SocketThread extends Thread {
        @Override
        public void run() {
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            //Этот метод будет представлять клиента серверу.
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    String name = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            //Этот метод будет реализовывать главный цикл обработки сообщений сервера.
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) processIncomingMessage(message.getData());
                else if (message.getType() == MessageType.USER_ADDED) informAboutAddingNewUser(message.getData());
                else if (message.getType() == MessageType.USER_REMOVED) informAboutDeletingNewUser(message.getData());
                else throw new IOException("Unexpected MessageType");
            }
        }

        protected void processIncomingMessage(String message) {
            //должен выводить текст message в консоль
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            //должен выводить в консоль информацию о том, что участник с именем userName присоединился к чату
            String message = String.format("Пользователь с именем %s подключился к чату", userName);
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutDeletingNewUser(String userName) {
            // - должен выводить в консоль, что участник с именем userName покинул чат.
            String message = String.format("Пользователь с именем %s покинул чат", userName);
            ConsoleHelper.writeMessage(message);
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
        /*- этот метод должен:
        а) Устанавливать значение поля clientConnected внешнего объекта Client в соответствии с переданным параметром.
        б) Оповещать (пробуждать ожидающий) основной поток класса Client.*/
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }
    }

    protected SocketThread getSocketThread() {
        //должен создавать и возвращать новый объект класса SocketThread
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        /*создает новое текстовое сообщение, используя переданный текст и отправляет его серверу
          через соединение connection.
          Если во время отправки произошло исключение IOException, то необходимо вывести информацию
          об этом пользователю и присвоить false полю clientConnected.*/
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Не удалось отправить сообщение на сервер.");
            clientConnected = false;
        }
    }

    protected boolean shouldSendTextFromConsole() {
        /*в данной реализации клиента всегда должен возвращать true (мы всегда отправляем текст введенный в консоль).
          Этот метод может быть переопределен, если мы будем писать какой-нибудь другой клиент,
          унаследованный от нашего, который не должен отправлять введенный в консоль текст.*/
        return true;
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException interruptedException) {
            ConsoleHelper.writeMessage("Возникла ошибка");
            return;
        }
        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено.\n" +
                "Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        while (clientConnected) {
            String consoleText = ConsoleHelper.readString();
            if (consoleText.equals("exit")) break;
            if (shouldSendTextFromConsole()) sendTextMessage(consoleText);
        }
    }
    
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
