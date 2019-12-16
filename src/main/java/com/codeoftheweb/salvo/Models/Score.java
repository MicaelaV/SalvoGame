package com.codeoftheweb.salvo.Models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private Double	score;
    private Date finishDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "playerID")
    private	Player player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gameID")
    private	Game game;

    public	Score(){
        this.finishDate = new Date();
    }/*Igual que en Game, el vacio*/

    public Score(Player player, Game game,  Double  score) {
        this.player = player;
        this.game = game;
        this.score = score;
        this.finishDate = new Date();
    }

    public Map<String,Object> makeScoreDTO(){
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("player",this.getPlayer().getId());//Conecta IdPlayer con su Score
        dto.put("score",this.getScore());
        return dto;
    }

    /*Get y Set*/

    public long getId() {
        return id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
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
}
