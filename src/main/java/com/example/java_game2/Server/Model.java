package com.example.java_game2.Server;

import com.example.java_game2.Constructor_objects.Circles;

import java.util.ArrayList;

public class Model {

    private final ArrayList<IObserver> observerArrayList = new ArrayList<>();
    private ArrayList<ClientController> clientArrayList = new ArrayList<>();
    private ArrayList<Circles> targetArrayList = new ArrayList<>();
    private ArrayList<Circles> arrowArrayList = new ArrayList<>();
    private final ArrayList<String> readyList = new ArrayList<>();
    private static final int Y_BOUND = 440;
    private final ArrayList<String> waitingList = new ArrayList<>();
    private final ArrayList<String> shootingList = new ArrayList<>();
    private String winner = null;
    private static final int WINNER_POINTS = 2;
    private volatile boolean isGameReset = true;

    public void update()
    {
        for (IObserver o : observerArrayList) {
            o.update();
        }
    }

    // Usual model data
    public void init() {
        targetArrayList.add(new Circles(330,209, 30));
        targetArrayList.add(new Circles(410,215, 15));
        arrowsCountUpdate();
    }

    // Add arrows for each player
    private synchronized void arrowsCountUpdate() {
        arrowArrayList.clear();
        int clientsCount = clientArrayList.size();
        for (int i = 1; i <= clientsCount; i++) {
            int step = Y_BOUND / (clientsCount + 1);
            arrowArrayList.add(new Circles(50, step * i, 35));
        }

    }

    // Ready state handle
    public void ready(MainServer mc, String name) {
        if (readyList.isEmpty()) { readyList.add(name); return;}

        if (readyList.contains(name)) {readyList.remove(name); }
        else {readyList.add(name);}

        if (clientArrayList.size() > 1 && readyList.size() == clientArrayList.size()) {
            isGameReset = false;
            gameStart(mc);
        }
    }

    // Pause state handle
    public void requestPause(String name) {
        if (isGameReset) return;
        if (waitingList.contains(name)) {
            waitingList.remove(name);
            if (waitingList.size() == 0){
                int a = 0;
                synchronized(this) {
                    notifyAll();
                }
            }
        } else {
            waitingList.add(name);
        }
    }

    // Shoot state handle
    public void requestShoot(String playerName) {
        if (isGameReset || waitingList.size() != 0) return;
        var player = clientArrayList.stream()
                .filter(clientData -> clientData.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);
        assert player != null;
        if (! shootingList.contains(player.getPlayerName())){
            shootingList.add(player.getPlayerName());
            player.increaseArrowsShoot(1);
        }
    }

    public void gameStart(MainServer mc) {
        Thread thread = new Thread(
                ()->
                {
                    int big_move = 5;
                    int sml_move = 10;
                    int arr_move = 15;
                    while (true) {
                        if (isGameReset) {
                            winner = null;
                            break;
                        }
                        if (waitingList.size() != 0) {
                            synchronized(this) {
                                try {
                                    wait();
                                } catch(InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if (shootingList.size() != 0) {

                                for (int i = 0; i < shootingList.size(); i++) {
                                    int finalI = i;
                                    if (shootingList.get(finalI) == null) break;
                                    ClientController client = clientArrayList.stream()
                                            .filter(clientData -> clientData.getPlayerName().equals(shootingList.get(finalI)))
                                            .findFirst()
                                            .orElse(null);
                                    int index = clientArrayList.indexOf(client);
                                    Circles p = arrowArrayList.get(index);
                                    p.setX(p.getX() + arr_move);
                                    shootManager(p, client);
                                }

                        }
                        Circles big = targetArrayList.get(0);
                        Circles small = targetArrayList.get(1);

                        if (small.getY() <= small.getR() || Y_BOUND - small.getY()-20  <= small.getR()) {
                            sml_move = -1 * sml_move;
                        }
                        small.setY(small.getY() + sml_move);
                        if (big.getY() <= big.getR() || Y_BOUND - big.getY()-30  <= big.getR()) {
                            big_move = -1 * big_move;
                        }
                        big.setY(big.getY() + big_move);

                        mc.bcast();

                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
        );
        thread.start();

    }

    private void gameReset() {
        isGameReset = true;
        readyList.clear();
        targetArrayList.clear();
        arrowArrayList.clear();
        waitingList.clear();
        shootingList.clear();
        clientArrayList.forEach(ClientController::reset);
        this.init();
    }


    private synchronized void shootManager(Circles p, ClientController player) {
        ShootStatus shootState = targetHitCheck(p);
        System.out.println(shootState);
        if (shootState.equals(ShootStatus.FLYING)) return;
        if (shootState.equals(ShootStatus.BIG_SHOT)) player.increasePointsEarned(1);
        if (shootState.equals(ShootStatus.SMALL_SHOT)) player.increasePointsEarned(2);
        p.setX(50);
        if (shootingList.size() == 1) shootingList.clear();
        else {
            shootingList.remove(player.getPlayerName());
        }
        checkWinner();


    }

    private synchronized void checkWinner() {
        clientArrayList.forEach(clientDataManager -> {
            if (clientDataManager.getPointsEarned() >= WINNER_POINTS) {
                this.winner = clientDataManager.getPlayerName();
                gameReset();
                return;
            }
        });
    }

    private synchronized ShootStatus targetHitCheck(Circles p) {

        if (contains(targetArrayList.get(1), p.getX() + p.getR(), p.getY())) {
            return ShootStatus.SMALL_SHOT;
        }
        if (contains(targetArrayList.get(0), p.getX() + p.getR(), p.getY())) {
            return ShootStatus.BIG_SHOT;
        }
        if (p.getX() > 430) {
            return ShootStatus.MISSED;
        }
        return ShootStatus.FLYING;
    }

    private boolean contains(Circles c, double x, double y) {
        return (Math.sqrt(Math.pow((x -c.getX()), 2) + Math.pow((y -c.getY()), 2)) < c.getR()) ;
    }














    public void addClient(ClientController clientData) {
        clientArrayList.add(clientData);
        this.arrowsCountUpdate();
    }
    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public  void addObserver(IObserver o)
    {
        observerArrayList.add(o);
    }

    public ArrayList<ClientController> getClientArrayList() {
        return clientArrayList;
    }

    public void setClientArrayList(ArrayList<ClientController> clientArrayList) {
        this.clientArrayList = clientArrayList;
    }

    public ArrayList<Circles> getTargetArrayList() {
        return targetArrayList;
    }

    public void setTargetArrayList(ArrayList<Circles> targetArrayList) {
        this.targetArrayList = targetArrayList;
    }

    public ArrayList<Circles> getArrowsArrayList() {
        return arrowArrayList;
    }

    public void setArrowArrayList(ArrayList<Circles> arrowArrayList) {
        this.arrowArrayList = arrowArrayList;
    }


}
