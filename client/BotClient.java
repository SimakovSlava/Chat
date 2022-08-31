package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client{
    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        int x = (int) (Math.random() * 100);
        return "date_bot_" + x;
    }

    public static void main(String[] args) {
        // Создаем и запускаем бот клиента
        Client client = new BotClient();
        client.run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            String [] arrayString = message.split(": ");
            if (arrayString.length != 2) return;

            String messageDate = arrayString[1];
            String dateFormat = null;
            switch (messageDate){
                case "дата":
                    dateFormat = "d.MM.YYYY";
                    break;
                case "день":
                    dateFormat = "d";
                    break;
                case "месяц":
                    dateFormat = "MMMM";
                    break;
                case "год":
                    dateFormat = "YYYY";
                    break;
                case "время":
                    dateFormat = "H:mm:ss";
                    break;
                case "час":
                    dateFormat = "H";
                    break;
                case "минуты":
                    dateFormat = "m";
                    break;
                case "секунды":
                    dateFormat = "s";
                    break;
            }

            if(dateFormat != null){
                String answer = new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime()); // instance - пример!!!!
                BotClient.this.sendTextMessage("Информация для " + arrayString[0] + ": " + answer);
            }
        }
    }
}
