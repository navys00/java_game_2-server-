package com.example.java_game2.Server;

import com.google.gson.Gson;

import java.io.*;

public class Client implements Runnable{

    final MainServer mainServer;
    final SocketWriter socketwriter;
    final Gson gson = new Gson();
    final Model model = ModelBuilder.build();
    final ClientInfo clientData;

    public Client(SocketWriter socketwriter, MainServer mainServer, String playerName)  {
        this.socketwriter = socketwriter;
        this.mainServer = mainServer;
        clientData = new ClientInfo(playerName);
    }
    public String getPlayerName() {
        return clientData.getPlayerName();
    }

    public void sendInfoToClient() {
        try {
            ServerResponse serverResp = new ServerResponse();
            serverResp.clientArrayList = model.getClientArrayList();
            serverResp.targetArrayList = model.getArrowsArrayList();
            serverResp.circleArrayList = model.getTargetArrayList();
            serverResp.theWinnerIs = model.getWinner();

            socketwriter.writeData(gson.toJson(serverResp));
        } catch (IOException ignored) {
        }
    }



    @Override
    public void run() {
        try {

            System.out.println("Client thread " + clientData.getPlayerName() + " started");
            model.addClient(clientData);
            mainServer.bcast();

            while(true)
            {
                String s = socketwriter.getData();
                System.out.println("Msg: " + s);


                ClientResponse msg = gson.fromJson(s, ClientResponse.class);

                if(msg.getClientActions() == ClientActions.READY)
                {
                    System.out.println("READY " + getPlayerName());
                    model.requestReady(mainServer, this.getPlayerName());
                }

                if(msg.getClientActions() == ClientActions.STOP)
                {
                    model.requestPause(getPlayerName());
                }
                if (msg.getClientActions() == ClientActions.SHOOT) {
                    model.requestShoot(getPlayerName());
                }


            }
        } catch (IOException ignored) {

        }
    }
    public ClientInfo getClientData() {
        return clientData;
    }

}
