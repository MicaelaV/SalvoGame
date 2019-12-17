package com.codeoftheweb.salvo.controllers;


import com.codeoftheweb.salvo.Models.*;
import com.codeoftheweb.salvo.repository.*;
import com.codeoftheweb.salvo.util.GameState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired /*Instanciar*/
     GameRepository gameRepository;

    @Autowired
    GamePlayerRepository gamePlayerRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    ShipRepository shipRepository;

    @Autowired
    SalvoRepository salvoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @RequestMapping("/games")
    public Map<String, Object> getAllGames(Authentication authentication) {
        Map<String, Object> dto=new LinkedHashMap<>();
        //chequeamos que si hay algun usuario logueado
        Player player = this.getUserAuthenticated(authentication);
        //Validacion
        if (isGuest(authentication)) {
            //caso negativo ingresamos Guest
            dto.put("player", "Guest");
        }else{
            //caso afirmativo lo incluimos al dto(lo que retornamos)
            player = playerRepository.findByEmail(authentication.getName());
            dto.put("player", player.makePlayerDTO());
        }
        //se muestran todos los juegos
        dto.put( "games", gameRepository.findAll().stream().map(game -> game.makeGameDTO()).collect(Collectors.toList()));
        return dto;
    }

    //metodo del tipo POST
    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Object> createGame(Authentication authentication) {
        if (isGuest(authentication)) {
            return new ResponseEntity<>("NO esta autorizado", HttpStatus.UNAUTHORIZED);
        }
        Player  player  = playerRepository.findByEmail(authentication.getName());

        if(player ==  null){
            return new ResponseEntity<>("NO esta autorizado", HttpStatus.UNAUTHORIZED);
        }
        Game game  = gameRepository.save(new Game());
        GamePlayer  gamePlayer  = gamePlayerRepository.save(new GamePlayer(player,game));
        return new ResponseEntity<>(makeMap("gpid",gamePlayer.getId()),HttpStatus.CREATED);
    }

    @RequestMapping("/game_view/{nn}")//M5 Task2 modificado en tareas siguientes

    public Map<String,Object> getGameViewByGamePlayerID(@PathVariable Long nn/*,Authentication authentication*/){

        GamePlayer gamePlayer = gamePlayerRepository.findById(nn).get();

        GamePlayer opponent = gamePlayer.getGame().getGamePlayers()
                                                    .stream()
                                                    .filter(gamePlayer1 -> gamePlayer1.getId() != gamePlayer.getId())
                                                                                                            .findFirst()
                                                                                                            .orElse(new GamePlayer());


        Map<String, Object> dto = new LinkedHashMap<>();
        Map<String, Object> hits = new LinkedHashMap<>();

        //task5 m5
        hits.put("self", getHits(gamePlayer, gamePlayer.getOpponent()));//task5
        hits.put("opponent", getHits(gamePlayer.getOpponent(), gamePlayer));//task5
        //

        //hits.put("self", new ArrayList<>()); // task4
        //hits.put("opponent", new ArrayList<>()); // task4


        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        /*dto.put("gameState", "PLACESHIPS");*/ // task4
        dto.put("gameState", getGameState(gamePlayer));//task5
        dto.put("gamePlayers", gamePlayer.getGame().getGamePlayers()
                .stream()
                .map(gamePlayer1 -> gamePlayer1.makeGamePlayerDTO())
                .collect(Collectors.toList()));

        dto.put("ships",gamePlayer.getShips()
                .stream()
                .map(ship -> ship.makeShipDTO())
                .collect(Collectors.toList()));//devuelve una lista de mapas

        dto.put("salvoes", gamePlayer.getGame().getGamePlayers()
                                                .stream()
                                                .flatMap(gamePlayer1 -> gamePlayer1.getSalvos()
                                                                                    .stream()
                                                                                    .map(salvo -> salvo.makeSalvoDTO()))
                                               .collect(Collectors.toList()));
        dto.put("hits", hits);
        return dto;
    }

    @RequestMapping("/leaderBoard")
    public List<Map<String,Object>> leaderBoard(){
        return playerRepository.findAll()
                                .stream()
                                .map(player -> player.makePlayerScoreDTO())
                                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Object> register(
            @RequestParam String email, @RequestParam String password) {

        if (email.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (playerRepository.findByEmail(email) !=  null) {
            return new ResponseEntity<>("Name already in use", HttpStatus.FORBIDDEN);
        }

        playerRepository.save(new Player(email, passwordEncoder.encode(password)));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    //Task1 M5 P4
    //En Spring Security, una instancia de la  clase Autenticación contendrá información sobre el usuario actual, incluido el nombre.
    //Metodo para retornar si hay usuario autenticado
    private Player getUserAuthenticated(Authentication authentication){
        Player player=new Player();
        //chequeamos que si hay algun usuario logueado
        if (authentication != null && authentication instanceof AnonymousAuthenticationToken != true) {
            //caso afirmativo lo incluimos al dto
            player = playerRepository.findByEmail(authentication.getName());
        }else{
            //caso negativo ingresamos null
            player = null;
        }
        return player;
    }

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    //crea un mapa con los datos enviados
    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    //Task2 m5 JoinGame
    @RequestMapping(path = "/game/{gameID}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameID, Authentication authentication) {
        if (isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "You can't join a Game if You're Not Logged In!"), HttpStatus.UNAUTHORIZED);
        }

        Player  player  = playerRepository.findByEmail(authentication.getName());
        Game gameToJoin = gameRepository.getOne(gameID);

        if (gameRepository.getOne(gameID) == null) {
            return new ResponseEntity<>(makeMap("error", "No such game."), HttpStatus.FORBIDDEN);
        }

        if(player ==  null){
            return new ResponseEntity<>(makeMap("error", "No such game."), HttpStatus.FORBIDDEN);
        }

        long gamePlayersCount = gameToJoin.getGamePlayers().size();

        if (gamePlayersCount == 1) {
            GamePlayer gameplayer = gamePlayerRepository.save(new GamePlayer(player, gameToJoin));
            return new ResponseEntity<>(makeMap("gpid", gameplayer.getId()), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(makeMap("error", "Game is full!"), HttpStatus.FORBIDDEN);
        }
    }

    //Task3 m5 Guardar posiciones de barcos
    @RequestMapping(value="/games/players/{gpid}/ships", method=RequestMethod.POST)
    public ResponseEntity<Map> addShips(@PathVariable long gpid, @RequestBody List<Ship> ships, Authentication authentication) {
        //Validaciones de jugador
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        GamePlayer gamePlayer = gamePlayerRepository.findById(gpid).orElse(null);
        Player  player  = playerRepository.findByEmail(authentication.getName());
        if(player ==  null){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        if(gamePlayer == null){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        if(gamePlayer.getPlayer().getId() !=  player.getId()){
            return new ResponseEntity<>(makeMap("error","Los players no coinciden"), HttpStatus.FORBIDDEN);
        }
        //validacion de barcos
        if (!gamePlayer.getShips().isEmpty()){
            return new ResponseEntity<>(makeMap("error", "You have all Ships!"), HttpStatus.FORBIDDEN);
            //prohibicion de cambiar posicion de barcos
        }
        ships.forEach(ship -> {
            ship.setGamePlayer(gamePlayer);
            shipRepository.save(ship);
        });
        return new ResponseEntity<>(makeMap("OK", "Ship Create!"), HttpStatus.CREATED);
    }

    //Task4 m5
    //método de controlador de fondo que pueda recibir un objeto salvo, que consiste en un turno y una lista de ubicaciones
    @RequestMapping(value = "/games/players/{gpid}/salvoes", method = RequestMethod.POST)
    public ResponseEntity<Map> addSalvo(@PathVariable long gpid, @RequestBody Salvo salvo, Authentication authentication){
        //validaciones de ingreso usuario
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error","You're NOT authorized"), HttpStatus.UNAUTHORIZED);
        }
        //Asigno si esta logueado
        Player player  = playerRepository.findByEmail(authentication.getName());
        GamePlayer self  = gamePlayerRepository.getOne(gpid);

        //validaciones de player
        if(player ==  null){
            return new ResponseEntity<>(makeMap("error","You're NOT authorized"), HttpStatus.UNAUTHORIZED);
        }
        if(self == null){
            return new ResponseEntity<>(makeMap("error","You're NOT authorized"), HttpStatus.UNAUTHORIZED);
        }
        if(self.getPlayer().getId() !=  player.getId()){
            return new ResponseEntity<>(makeMap("error","The players don't match"), HttpStatus.FORBIDDEN);
        }
        GamePlayer opponent  = self.getGame().getGamePlayers().stream()
                                    .filter(gamePlayer -> gamePlayer.getId()  !=  self.getId())
                                    .findFirst()
                                    .orElse(new GamePlayer());

        if(self.getSalvos().size() <=  opponent.getSalvos().size()){
            salvo.setTurn(self.getSalvos().size()  + 1);
            salvo.setGamePlayer(self);
            salvoRepository.save(salvo);
            return  new ResponseEntity<>(makeMap("OK","Salvo created!!"), HttpStatus.CREATED);
        }
        return  new ResponseEntity<>(makeMap("Error","You can´t play again"), HttpStatus.CREATED);
    }

    public String getState(GamePlayer gamePlayer, GamePlayer  opponent){
        //Verifica si lista de barcos esta vacia
        if(gamePlayer.getShips().isEmpty()){
            return "PLACESHIPS";
        }
        //Verifica si solo es un jugador
        if(gamePlayer.getGame().getGamePlayers().size() == 1){
            return "WAITINGFOROPP";
        }
        //estado "Play" task4
        if(gamePlayer.getGame().getGamePlayers().size() == 2 && gamePlayer.getId() < opponent.getId()){
            return "PLAY";
        }
        //Inicio de Juego
        if(gamePlayer.getGame().getGamePlayers().size() == 2 && gamePlayer.getId() > opponent.getId()){
            return "WAIT";
        }
        return "UNDEFINED";
    }


    //calcular el daño realizado
    private List<Map> getHits(GamePlayer self, GamePlayer opponent) {

        List<Map> hits = new ArrayList<>();

        Integer carrierDamage = 0;
        Integer battleshipDamage = 0;
        Integer submarineDamage = 0;
        Integer destroyerDamage = 0;
        Integer patrolboatDamage = 0;

        List <String> carrierLocation = getLocatiosByType("carrier",self);
        List <String> battleshipLocation = getLocatiosByType("battleship",self);
        List <String> submarineLocation = getLocatiosByType("submarine",self);
        List <String> destroyerLocation = getLocatiosByType("destroyer",self);
        List <String> patrolboatLocation = getLocatiosByType("patrolboat",self);

        for (Salvo  salvo : opponent.getSalvos()){

            long carrierHitsInTurn = 0;
            long battleshipHitsInTurn = 0;
            long submarineHitsInTurn = 0;
            long destroyerHitsInTurn = 0;
            long patrolboatHitsInTurn = 0;
            long missedShots = salvo.getSalvoLocations().size();

            Map<String, Object> hitsMapPerTurn = new LinkedHashMap<>();
            Map<String, Object> damagesPerTurn = new LinkedHashMap<>();

            List<String> salvoLocationsList = new ArrayList<>();
            List<String> hitCellsList = new ArrayList<>();

            for (String salvoShot : salvo.getSalvoLocations()) {
                if (carrierLocation.contains(salvoShot)) {
                    carrierDamage++;
                    carrierHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (battleshipLocation.contains(salvoShot)) {
                    battleshipDamage++;
                    battleshipHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (submarineLocation.contains(salvoShot)) {
                    submarineDamage++;
                    submarineHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (destroyerLocation.contains(salvoShot)) {
                    destroyerDamage++;
                    destroyerHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (patrolboatLocation.contains(salvoShot)) {
                    patrolboatDamage++;
                    patrolboatHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
            }

            damagesPerTurn.put("carrierHits", carrierHitsInTurn);
            damagesPerTurn.put("battleshipHits", battleshipHitsInTurn);
            damagesPerTurn.put("submarineHits", submarineHitsInTurn);
            damagesPerTurn.put("destroyerHits", destroyerHitsInTurn);
            damagesPerTurn.put("patrolboatHits", patrolboatHitsInTurn);
            damagesPerTurn.put("carrier", carrierDamage);
            damagesPerTurn.put("battleship", battleshipDamage);
            damagesPerTurn.put("submarine", submarineDamage);
            damagesPerTurn.put("destroyer", destroyerDamage);
            damagesPerTurn.put("patrolboat", patrolboatDamage);

            hitsMapPerTurn.put("turn", salvo.getTurn());//en la clase de Salvo
            hitsMapPerTurn.put("hitLocations", hitCellsList);
            hitsMapPerTurn.put("damages", damagesPerTurn);
            hitsMapPerTurn.put("missed", missedShots);
            hits.add(hitsMapPerTurn);

        }


        return hits;
    }

    private List<String>  getLocatiosByType(String type, GamePlayer self){
        return  self.getShips().size()  ==  0 ? new ArrayList<>() : self.getShips().stream().filter(ship -> ship.getType().equals(type)).findFirst().get().getShipLocations();
    }

    private GameState getGameState (GamePlayer gamePlayer) {

        if (gamePlayer.getShips().size() == 0) {
            return GameState.PLACESHIPS;
        }
        if (gamePlayer.getGame().getGamePlayers().size() == 1){
            return GameState.WAITINGFOROPP;
        }
        if (gamePlayer.getGame().getGamePlayers().size() == 2) {

            GamePlayer opponentGp = gamePlayer.getOpponent();

            if ((gamePlayer.getSalvos().size() == opponentGp.getSalvos().size()) && (getIfAllSunk(opponentGp, gamePlayer)) && (!getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.WON;
            }
            if ((gamePlayer.getSalvos().size() == opponentGp.getSalvos().size()) && (getIfAllSunk(opponentGp, gamePlayer)) && (getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.TIE;
            }
            if ((gamePlayer.getSalvos().size() == opponentGp.getSalvos().size()) && (!getIfAllSunk(opponentGp, gamePlayer)) && (getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.LOST;
            }

            if ((gamePlayer.getSalvos().size() == opponentGp.getSalvos().size()) && (gamePlayer.getId() < opponentGp.getId())) {
                return GameState.PLAY;
            }
            if (gamePlayer.getSalvos().size() < opponentGp.getSalvos().size()){
                return GameState.PLAY;
            }
            if ((gamePlayer.getSalvos().size() == opponentGp.getSalvos().size()) && (gamePlayer.getId() > opponentGp.getId())) {
                return GameState.WAIT;
            }
            if (gamePlayer.getSalvos().size() > opponentGp.getSalvos().size()){
                return GameState.WAIT;
            }

        }
        return GameState.UNDEFINED;
    }
    private Boolean getIfAllSunk (GamePlayer self, GamePlayer opponent) {

        if(!opponent.getShips().isEmpty() && !self.getSalvos().isEmpty()){
            return opponent.getSalvos().stream().flatMap(salvo -> salvo.getSalvoLocations().stream()).collect(Collectors.toList()).containsAll(self.getShips().stream()
                    .flatMap(ship -> ship.getShipLocations().stream()).collect(Collectors.toList()));
        }
        return false;
    }



}