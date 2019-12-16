package com.codeoftheweb.salvo;

import com.codeoftheweb.salvo.Models.*;
import com.codeoftheweb.salvo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class SalvoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
		/*System.out.printf("Hello  World");*/
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/*Ahora podemos escribir
	@Autowire
	PasswordEncoder passwordEncoder;
	en cualquier clase que necesite codificar contraseñas. */

	@Bean
	public CommandLineRunner initData (PlayerRepository player,
									   GameRepository game,
									   GamePlayerRepository gamePlayer,
									   ShipRepository ship,
									   SalvoRepository salvo,
									   ScoreRepository score){
		return (args)->{
			/*
			Date date1 = new Date();
			game.save(new Game(date1));
			game.save(new Game(Date.from(date1.toInstant().plusSeconds(3600))));*/


			Player player1 = new Player("lola@lola.com", passwordEncoder().encode("123"));
			player.save(player1);

			Player player2 = new Player("pablo@lola.com", passwordEncoder().encode("123"));
			player.save(player2);

			Date date1 = new Date();
			Game game1 = new Game();
			game1.setCreationDate(Date.from(date1.toInstant().plusSeconds(3600)));
			game.save(game1);

			Game game3 = new Game();
			game3.setCreationDate(Date.from(game3.getCreationDate().toInstant().plusSeconds(3600)));
			game.save(game3);

			GamePlayer gamePlayer1 = new GamePlayer(player1,game1);
			gamePlayer.save(gamePlayer1);

			GamePlayer gamePlayer2 = new GamePlayer(player2,game1);
			gamePlayer.save(gamePlayer2);

			GamePlayer gamePlayer3 = new GamePlayer(player2,game3);
			gamePlayer.save(gamePlayer3);

			GamePlayer gamePlayer4 = new GamePlayer(player1,game3);
			gamePlayer.save(gamePlayer4);

			List<String> locations1 = new ArrayList<>();
			locations1.add("H1");
			locations1.add("H2");
			locations1.add("H3");

			Ship ship1 = new Ship("destroyer", locations1, gamePlayer1);
			Ship ship2 = new Ship("submarine", Arrays.asList("E1","F1","G1"),gamePlayer2);
			ship.save(ship1);
			ship.save(ship2);

			Salvo salvo1 = new Salvo(1,Arrays.asList("B5","F1","D5"),gamePlayer1);
			salvo.save(salvo1);

			Salvo salvo2 = new Salvo(1,Arrays.asList("E1","H1","D5"),gamePlayer2);
			salvo.save(salvo2);

			Score score1 = new Score(player1,game1,1.0D);
			score.save(score1);

			Score score2 = new Score(player2,game1,0.5D);
			score.save(score2);

		};
	}

}

@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	PlayerRepository playerRepository;

	@Override
	/*define un método que devuelve una clase UserDetailsService con un método loadUserByUsername ( name ).
	Se define que el método para obtener el jugador con el nombre, y si lo hay, y devuelven un
	DetallesUsuario objeto con el nombre del jugador y contraseña.*/
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName-> {
			Player player = playerRepository.findByEmail(inputName);/*Obtiene el player y verifica q no sea null, crea una instancia y valida el pass  recien crea un rol*/
			if (player != null) {
				return new User(player.getEmail(), player.getPassword(),
						AuthorityUtils.createAuthorityList("USER"));//Crea el Rol, se puede agregar el ADMI
			} else {
				throw new UsernameNotFoundException("Unknown user: " + inputName);
			}
		});
	}
} /* le decimos la nueva ruto es esa para el login, y le mandamos los parametros de name y password*/

/*M5 T1 P3*/
/*In this new class, the rules for what is public, how information is sent, and so on,
is specified in the definition of the configure() method.*/
@EnableWebSecurity
@Configuration
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	/*In this class, you define one method, configure(). In that method, you write the rules describing
	how the browser should get the user name and password to send the web application
	the patterns of URLs that are and are not accessible to different types of users*/
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/web/**").permitAll()
				.antMatchers("/api/game_view/**").hasAuthority("USER")
				.antMatchers("/api/games").permitAll();
		http.formLogin()
				.usernameParameter("username")
				.passwordParameter("password")
				.loginPage("/api/login");
		http.logout().logoutUrl("/api/logout");
		// turn off checking for CSRF tokens
		http.csrf().disable();
		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));
		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
	}
	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}
}