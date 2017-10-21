package com.example.eurekamoviehouse;

import com.example.eurekamoviehouse.dao.search.SearchQuery;
import com.example.eurekamoviehouse.dao.movie.Movie;
import com.example.eurekamoviehouse.dao.user.UserProfile;
import com.example.eurekamoviehouse.dao.movie.Rating;
import com.example.eurekamoviehouse.service.RecommendFutureItem;
import com.example.eurekamoviehouse.service.RecommendService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.tomcat.dbcp.dbcp.DriverManagerConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool;
import org.apache.tomcat.dbcp.dbcp.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolingDataSource;

import org.apache.mahout.cf.taste.impl.model.jdbc.PostgreSQLJDBCDataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;


import java.sql.Connection;
import java.sql.ResultSet;
import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


@SpringBootApplication
@RestController
public class EurekaMovieHouseApplication {

	private final String POSTER_URL_PREFIX = "https://s3-us-west-1.amazonaws.com/qzhoutests3/movie_poster/";

	private PoolingDataSource dataSource;
	private StandardAnalyzer analyzer;
	private Directory index;
	private IndexWriterConfig config;
	private IndexWriter w;

	private Log log = LogFactory.getLog(EurekaMovieHouseApplication.class);

	Recommender recommender;
	PostgreSQLJDBCDataModel model;
	RecommendService recommendService;

	List<RecommendFutureItem> queue;

	@RequestMapping(value="/register", method = RequestMethod.POST)
	@ResponseBody
	public int register(@RequestBody String content){
		ObjectMapper mapper = new ObjectMapper();
		try {
			UserProfile profile = mapper.readValue(content, UserProfile.class);
			log.info( "register user with a name of " + profile.getUsername() + " and password of " + profile.getPassword() );

			Connection conn = dataSource.getConnection();
			int id;
			PreparedStatement userExistsPs = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
			userExistsPs.setString(1, profile.getUsername());
			ResultSet rs = userExistsPs.executeQuery();
			if(rs.next()){
				return -1;
			}

			PreparedStatement insertUserPs = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?,?,?) RETURNING id");
			insertUserPs.setString(1, profile.getUsername());
			insertUserPs.setString(2, profile.getPassword());
			insertUserPs.setString(3, profile.getEmail());
			ResultSet insertUserRs = insertUserPs.executeQuery();
			if(rs.next()){
				id = rs.getInt("id");
				if( id > 0 )
					return id;
				else
					return -1;
			}else{
				return -1;
			}
		} catch (IOException e){
			log.info( e.getMessage() );
		} catch(SQLException e){
			log.info( e.getMessage() );
		}
		return -1;
	}

	protected List<Movie> getMovieByID(String idString){
		List<Movie> result = new LinkedList<Movie>();
		try {
			Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(
					"select * from movies where id in (" + idString + ")");
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String title = rs.getString("omdb_title");
				String plot = rs.getString("plot");
				String actors = rs.getString("actors");
				Float rating = rs.getFloat("imdb_rating");
				String director = rs.getString("director");
				String poster = rs.getString("poster");

				poster = POSTER_URL_PREFIX + poster;
				result.add( new Movie(title, director, actors, plot, poster, rating) );
			}
		}catch(SQLException e){
				log.info( e.getMessage());
		}
		return result;
	}

	@RequestMapping(value="/recommend/{id}", method = RequestMethod.GET)
	@ResponseBody
	public List<Movie> recommend(@PathVariable("id") long id){
 		try {
			log.info("recommend for user with an id of " + id);
			List<RecommendedItem> recommendations = recommender.recommend(id, 10);
			if(recommendations.size() == 0){
				return null;
			}
			String movieQuery = "";
			for (RecommendedItem recommendation : recommendations) {
				log.info(recommendation);
				movieQuery += recommendation.getItemID() + ",";
			}
			log.info("query to select movies with ID of " + movieQuery );
			movieQuery = movieQuery.substring(0, movieQuery.length() - 1);
			return getMovieByID(movieQuery);
		} catch(TasteException e){
			log.info( e.getMessage() );
		}
		return null;
	}

	@RequestMapping(value="/authenticate", method = RequestMethod.POST)
	@ResponseBody
	public boolean authenticate(@RequestBody String content){
		ObjectMapper mapper = new ObjectMapper();
		try {
			UserProfile profile = mapper.readValue(content, UserProfile.class);
			log.info( "authenticate user profile with name of " + profile.getUsername() + " and password of " + profile.getPassword() );
			Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE password=? AND username=?");
			ps.setString( 1, profile.getPassword() );
			ps.setString( 2, profile.getUsername() );
			int id=-1;
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				id = rs.getInt("id");
				break;
			}
			if( id > 0 ) {
				return true;
			}else{
				return false;
			}
		} catch(IOException e){
			log.info( e.getMessage() );
		} catch(SQLException e){
			log.info( e.getMessage() );
		}
		return false;
	}

	@Configuration
	@EnableWebSecurity
	protected static class SecurityConfiguration extends WebSecurityConfigurerAdapter{
		@Override
		protected void configure(HttpSecurity http) throws Exception{
			http.authorizeRequests().antMatchers(
					"/index.html", "/home.html", "/login.html", "/resource", "/", "/user", "/user_profile").permitAll()
					.and().formLogin()
					.and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
		}
	}

	@RequestMapping(value="/topmoviesbygenre/{genre}", method = RequestMethod.POST)
	public List<Movie> getTopMoviesByGenre(@PathVariable("genre") String genre){
		try {
			List<Movie> movies = new LinkedList<Movie>();
			Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement("SELECT omdb_title, plot, actors, imdb_rating, director, poster FROM movies WHERE genre = ? ORDER BY imdb_rating LIMIT 20");
			ps.setString(1, genre);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				String title = rs.getString("omdb_title");
				String plot = rs.getString("plot");
				String actors = rs.getString("actors");
				Float rating = rs.getFloat("imdb_rating");
				String director = rs.getString("director");
				String poster = rs.getString("poster");
				poster = POSTER_URL_PREFIX + poster;
				movies.add(new Movie(title, director, actors, plot, poster, rating));
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@RequestMapping(value="/rateMovies", method=RequestMethod.POST)
	public boolean rateMovies(@RequestBody String movieRatings){
		ObjectMapper mapper = new ObjectMapper();
		try {
			Connection conn = dataSource.getConnection();
			PreparedStatement addRatingPS = conn.prepareStatement("INSERT INTO ratings (userid, movieid, rating) VALUES (?, ?, ?)");
			Rating rating = mapper.readValue(movieRatings, Rating.class);

			addRatingPS.setString(1, String.valueOf(rating.getUserid()));
			addRatingPS.setString(2, String.valueOf(rating.getMovieid()));
			addRatingPS.setString(3, String.valueOf(rating.getRating()));
			addRatingPS.executeQuery();

			PreparedStatement needRecommend = conn.prepareStatement("SELECT COUNT(*) AS count FROM ratings WHERE userid = ?");
			needRecommend.setString(1, String.valueOf(rating.getUserid()));
			ResultSet result = needRecommend.executeQuery();
			if(result.next()){
				int ratingCount = Integer.parseInt( result.getString("count") );
				if(ratingCount > 10){
					PreparedStatement hasRecommedations = conn.prepareStatement("SELECT COUNT(*) AS reccount FROM recommendations WHERE userid = ?");
					hasRecommedations.setString(1, String.valueOf(rating.getUserid()));
					ResultSet recResult = hasRecommedations.executeQuery();
					if(recResult.next()){
						int recCount = Integer.parseInt( recResult.getString("reccount"));
						if(recCount == 0){
							// kick off a new thread to generate recommendations
							recommendService.recommendForUser(recommender, dataSource, rating.getUserid(), 10, queue );
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@RequestMapping(value="/search", method = RequestMethod.POST)
	public List<Movie> search(@RequestBody String query){
		List<Movie> searchResults = new ArrayList<Movie>();
		QueryParser q = new QueryParser("title", analyzer);
		try {
			log.info("user entered query is " + query);
			ObjectMapper mapper = new ObjectMapper();
			SearchQuery searchquery = mapper.readValue(query, SearchQuery.class);

			Query parsedQuery = q.parse(searchquery.getSearch());
			int hitsPerPage = 10;
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search( parsedQuery, hitsPerPage);
			ScoreDoc[] hits = docs.scoreDocs;
			Connection conn = dataSource.getConnection();
			String idString = "";
			if(hits.length == 0){
				return searchResults;
			}
			for(ScoreDoc doc : hits){
				Document d = reader.document(doc.doc);
				log.info("doc " + doc.doc + " " + d.getField("id").numericValue().intValue() + " " + d.get("title"));
				idString += d.get("id") + ",";
			}
			idString = idString.substring(0, idString.length()-1);
			log.info(idString);

			PreparedStatement ps = conn.prepareStatement("select * from movies where id in (" + idString + ")" );
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				String title = rs.getString("omdb_title");
				String plot = rs.getString("plot");
				String actors = rs.getString("actors");
				Float rating = rs.getFloat("imdb_rating");
				String director = rs.getString("director");
				String poster = rs.getString("poster");

				poster = POSTER_URL_PREFIX + poster;
				searchResults.add(new Movie(title, director, actors, plot, poster, rating));
			}
			conn.close();
		}catch(ParseException e){
			log.error(e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return searchResults;
	}

	private void addDoc(IndexWriter w, String title, String producer, String actors, int id) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new StringField("producer", producer, Field.Store.YES));
		doc.add(new StringField("actors", actors, Field.Store.YES));
		doc.add(new StoredField("id", id));
		w.addDocument(doc);
	}

	/*
		set up connection pool
		set up lucene in memory index
		use recommendation engine to produce recommendations for user 9
 	*/
	@PostConstruct
	public void setUpMovieHouse(){
		try {
			GenericObjectPool connectionPool = new GenericObjectPool(null);
			ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:postgresql://localhost:5432/movies", "zhouqiang", "Zhou1989");
			PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
			dataSource = new PoolingDataSource(connectionPool);

			log.info("start building a recommendation model...");
			model = new PostgreSQLJDBCDataModel(
					dataSource,
					"ratings",
					"userid",
					"movieid",
					"rating",
					""
			);

			recommender = new SVDRecommender(model, new ALSWRFactorizer(model, 10, 0.8, 100));

			analyzer = new StandardAnalyzer();
			index = new RAMDirectory();
			config = new IndexWriterConfig(analyzer);
			w = new IndexWriter(index, config);

			Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement("select id, omdb_title, director, actors from movies");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				String title = rs.getString("omdb_title");
				String actors = rs.getString("actors");
				String director = rs.getString("director");
				int id = rs.getInt("id");
				addDoc(w, title, director, actors, id);
			}
			w.close();
			conn.close();
		} catch (IOException e) {
			log.error("IOException while set up lucene index");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (TasteException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping("/topRatingMovies")
	public List<Movie> getTopRatingMovies(){
		try {
			Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement("select * from movies where imdb_rating is not null order by imdb_rating desc limit 30");
			ResultSet rs = ps.executeQuery();

			List<Movie> movies = new ArrayList<>();
			while(rs.next()){
				String title = rs.getString("omdb_title");
				String plot = rs.getString("plot");
				String actors = rs.getString("actors");
				Float rating = rs.getFloat("imdb_rating");
				String director = rs.getString("director");
				String poster = rs.getString("poster");

				poster = POSTER_URL_PREFIX + poster;
				movies.add(new Movie(title, director, actors, plot, poster, rating));
			}
			conn.close();
			return movies;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		SpringApplication.run(EurekaMovieHouseApplication.class, args);
	}
}
