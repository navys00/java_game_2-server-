package com.example.java_game2.Server;

import com.example.java_game2.Server.ClientActions;

public class ClientResponse {

    final ClientActions clientActions;
    public ClientResponse(ClientActions clientActions) {
        this.clientActions = clientActions;
    }
    public ClientActions getClientActions() {
        return clientActions;
    }
}
