package com.codeoftheweb.salvo.repository;

import com.codeoftheweb.salvo.Models.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;
import java.util.Set;

@RepositoryRestResource

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Player findByEmail(@Param("name") String name);
}
