package cf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cf.DataTuple;
import cf.Rating;

/*
 * Entrance of the program, raw data and queries will be read and pre-process.
 * Then four rating algorithm will be executed and result will be outputed
 */
public class main{
	
	//File path for Input and Output
	static String DataInput = System.getProperty("user.dir") + "\\training_set\\";
	static String QueryInput = System.getProperty("user.dir") + "\\queries-small.txt";
	static String UserUserOutput = System.getProperty("user.dir") + "\\User-User-result.txt";
	static String MovieMovieOutput = System.getProperty("user.dir") + "\\Movie-Movie-result.txt";
	static String UserUserNormOutput = System.getProperty("user.dir") + "\\User-User-Norm-result.txt";
	static String CustomOutput = System.getProperty("user.dir") + "\\Custom-result.txt";

	
	static ArrayList<ArrayList<DataTuple>> MovieFirstRating = new ArrayList<ArrayList<DataTuple>>();
	static ArrayList<ArrayList<DataTuple>> UserFirstRating = new ArrayList<ArrayList<DataTuple>>();
	static ArrayList<DataTuple> MovieRating = new ArrayList<DataTuple>();
	static ArrayList<DataTuple> UserRating = new ArrayList<DataTuple>();
	static ArrayList<QueryPair> QuerySet = new ArrayList<QueryPair>();
	
	public static void main(String[] args) throws IOException{
		
		long startMili = System.currentTimeMillis();
		
		//read raw ranking data and queries
		ReadData();
		ReadQuery();
		
		//ranking for movie-indexed data
		Moviecomparator comp1 = new Moviecomparator();
		Collections.sort(MovieRating, comp1);
		
		//ranking for user-indexed data
		Usercomparator comp2 = new Usercomparator();
		Collections.sort(UserRating, comp2);
		
		//rebuild movie-indexed data copy
		int CurMovie = 0;
		ArrayList<DataTuple> tmplist = new ArrayList<DataTuple>();
		for(DataTuple tmptuple : MovieRating){
			if(tmptuple.getMovie() != CurMovie){
				MovieFirstRating.add(tmplist);
				tmplist = new ArrayList<DataTuple>();
				CurMovie = tmptuple.getMovie();
			}
			DataTuple tmp = new DataTuple();
			tmp.setMovie(tmptuple.getMovie());
			tmp.setUser(tmptuple.getUser());
			tmp.setRating(tmptuple.getRating());
			tmplist.add(tmp);
		}
		MovieFirstRating.add(tmplist);
		MovieFirstRating.remove(0);
		MovieFirstRating.trimToSize();
		
		//rebuild user-indexed data copy
		int CurUser = 0;
		tmplist = new ArrayList<DataTuple>();
		for(DataTuple tmptuple : UserRating){
			if(tmptuple.getUser() != CurUser){
				UserFirstRating.add(tmplist);
				tmplist = new ArrayList<DataTuple>();
				CurUser = tmptuple.getUser();
			}
			DataTuple tmp = new DataTuple();
			tmp.setMovie(tmptuple.getMovie());
			tmp.setUser(tmptuple.getUser());
			tmp.setRating(tmptuple.getRating());
			tmplist.add(tmp);
		}
		UserFirstRating.add(tmplist);
		UserFirstRating.remove(0);
		UserFirstRating.trimToSize();
		
		//user-user rating algorithm
		System.out.println("user-user rating start!");
		Rating.rating_user(QuerySet, UserFirstRating);
		WriteResult(QuerySet, UserUserOutput);
		System.out.println("user-user rating finish!");
		
		//movie-movie rating algorithm
		System.out.println("movie-movie rating start!");
		Rating.rating_movie(QuerySet, MovieFirstRating);
		WriteResult(QuerySet, MovieMovieOutput);
		System.out.println("movie-movie rating finish!");
		
		//user-user normalized rating algorithm
		System.out.println("user-user normalized rating start!");
		Rating.rating_user_norm(QuerySet, UserFirstRating);
		WriteResult(QuerySet, UserUserNormOutput);
		System.out.println("user-user normalized rating finish!");
		
		//custom rating algorithm
		System.out.println("custom rating start!");
		Rating.rating_custom(QuerySet, UserFirstRating);
		WriteResult(QuerySet, CustomOutput);
		System.out.println("custom rating finish!");
		
		//get running time
		long endMili = System.currentTimeMillis();
		System.out.println("Total running time is " + (endMili - startMili) + "ms");		
		System.out.println("Finished!");

	}
	
	public static void ReadData() throws IOException{
		//get all file name under the process directory
		File ReadDirectory = new File(DataInput);
		File[] files = ReadDirectory.listFiles();
		
		//if the process directory has no file, end the process.
		if (files == null){
			System.out.println("Data File Error");
			return;
		}
		
		//traverse all files under the process directory
		for (File ReadFileName : files){
			
			//Open input stream
			FileInputStream IOStream = new FileInputStream(ReadFileName);
			InputStreamReader read = new InputStreamReader(IOStream); 
			BufferedReader reader = new BufferedReader(read);
			
			String MovieItem = reader.readLine();
			int MovieNum = Integer.parseInt(MovieItem.substring(0, MovieItem.length() - 1));
			
			//read data line by line to the end of file
			String str = new String();
			while ((str = reader.readLine()) != null){
				String[] RateItem = str.split(",");
				int UserID = Integer.parseInt(RateItem[0]);
				int UserScore = Integer.parseInt(RateItem[1]);
				
				DataTuple tmp = new DataTuple();
				tmp.setMovie(MovieNum);
				tmp.setUser(UserID);
				tmp.setRating(UserScore);
				
				MovieRating.add(tmp);
				UserRating.add(tmp);	
			}
						
			reader.close();
			read.close();
			IOStream.close();
			
			MovieRating.trimToSize();
			UserRating.trimToSize();
		}
		
	}
	
	public static void ReadQuery() throws IOException{
		//Open input stream
		FileInputStream IOStream = new FileInputStream(QueryInput);
		InputStreamReader read = new InputStreamReader(IOStream); 
		BufferedReader reader = new BufferedReader(read);
		
		//read date data line by line to the end of file
		int CurMovie = 0;
		String str = new String();
		while ((str = reader.readLine()) != null){
			if (str.charAt(str.length() - 1) == ':'){
				CurMovie = Integer.parseInt(str.substring(0, str.length() - 1));
				continue;
			}
			
			int CurUser = Integer.parseInt(str);
			
			QueryPair tmp = new QueryPair();
			tmp.setMovie(CurMovie);
			tmp.setUser(CurUser);
			tmp.setRating(0);
			
			QuerySet.add(tmp);
		}
		
		reader.close();
		read.close();
		IOStream.close();
		
		QuerySet.trimToSize();
	}
	
	public static void WriteResult(ArrayList<QueryPair> QuerySet,String OutputPath) throws IOException{
		//write all predict results to output file
		
		File WriteFile = new File(OutputPath);
		FileOutputStream IOStream = new FileOutputStream(WriteFile, true);
		OutputStreamWriter write = new OutputStreamWriter(IOStream);
		BufferedWriter writer = new BufferedWriter(write);
		int CurrentMovie = 0; 

		for (int i = 0; i < QuerySet.size(); i++){
			String str = "";
			if(QuerySet.get(i).getMovie() != CurrentMovie){
				str += QuerySet.get(i).getMovie();
				str += ":\n";
				CurrentMovie = QuerySet.get(i).getMovie();
			}
			
			str += QuerySet.get(i).getRating() + "\n";
			
			writer.write(str);
			writer.flush();
			write.flush();
		}

		writer.close();
		write.close();
		IOStream.close();
	}
	
	
	public static class Moviecomparator implements Comparator<Object>{
		//Comparator for movies
		//the first sort key is MovieID
		//the second sort key is UserID

	    public int compare(Object o1,Object o2) {
	    	DataTuple tuple1 = (DataTuple)o1;
	    	DataTuple tuple2 = (DataTuple)o2;
	    	if(tuple1.getMovie() > tuple2.getMovie())
	    		return 1;
	    	else if (tuple1.getMovie() < tuple2.getMovie())
	    		return -1;
	    	else if (tuple1.getMovie() == tuple2.getMovie()) {
	    		if (tuple1.getUser() > tuple2.getUser())
	        	   return 1;
	    	}
	    	return -1;
	    }
	}
	
	public static class Usercomparator implements Comparator<Object>{
		//Comparator for users
		//the first sort key is UserID
		//the second sort key is MovieID
		
		 public int compare(Object o1,Object o2) {
		    	DataTuple tuple1 = (DataTuple)o1;
		    	DataTuple tuple2 = (DataTuple)o2;
		    	if(tuple1.getUser() > tuple2.getUser())
		    		return 1;
		    	else if (tuple1.getUser() < tuple2.getUser())
		    		return -1;
		    	else if (tuple1.getUser() == tuple2.getUser()) {
		    		if (tuple1.getMovie() > tuple2.getMovie())
		        	   return 1;
		    	}
		    	return -1;
		}
	}
	
}