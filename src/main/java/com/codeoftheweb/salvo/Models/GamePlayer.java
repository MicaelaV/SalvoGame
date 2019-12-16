package com.codeoftheweb.salvo.Models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;


@Entity
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;
    private Date joinDate;

    public GamePlayer(){

        this.joinDate = new Date();
        //tas5 m5
        //instacion listas (ships y salvoes)
        this.ships = new HashSet<>();
        this.salvos = new HashSet<>();

    }

    /*conexion entre tablas*/

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name ="playerID")
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name ="gameID")
    private Game game;

    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
     private Set<Ship> ships;

    @OneToMany(mappedBy = "gamePlayer", fetch = FetchType.EAGER)
    private Set<Salvo> salvos;

    public GamePlayer(Player player, Game game) {
        this.joinDate = new Date();
        this.player = player;
        this.game = game;
    }

    public Map<String,Object> makeGamePlayerDTO(){
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("id",this.getId());
        dto.put("player",this.getPlayer().makePlayerDTO());
        return dto;
    }

    //Getter y Setter

    public Set<Salvo> getSalvos() {
        return salvos;
    }

    public void setSalvos(Set<Salvo> salvos) {
        this.salvos = salvos;
    }

    //task3

    public Set<Ship> getShips() {
        return ships;
    }

    public void setShips(Set<Ship> ships) {
        this.ships = ships;
    }

    public long getId() {
        return id;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    //task5 m5
    //Obtien gamePLayer de ese Game y verifica el id, devuelve el gamePLayer que di. Si no lo encuentra devuelve un New gamePlayer
    public GamePlayer getOpponent() {
        return this.getGame().getGamePlayers().stream()
                .filter(gamePlayer -> gamePlayer.getId() != this.getId())
                .findFirst()
                .orElse(new GamePlayer());
    }

}
