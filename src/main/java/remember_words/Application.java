package remember_words;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Application {

	public static void main(String[] args) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

		List<Document> wordList = new ArrayList<>();
		String opt = "init";
		Scanner s = new Scanner(System.in);
		String word_id;
		out: while (true) {
			if (opt.equals("init") || opt.equals("")) {
				System.out.println("Input word :");
				word_id = s.nextLine();
			} else {
				word_id = opt;
			}

			opt = "init";

			if (word_id.equals("q"))
				break;

			if (!word_id.matches("[a-zA-Z]*")) {
				System.out.println(translate(word_id));
				continue;
			}

			String[] arr = getDefine(word_id);

			if (arr[0].equals("")) {
				System.out.println(arr[1].equals("NULL") ? "Found No Meanings!" : arr[1].trim().replaceAll(" ", "\n"));
				continue;
			}

			System.out.println(arr[0] + "\n" + arr[1].trim().replaceAll(" ", "\n")
					+ "\nPress ENTER->append, Another Word->next, q->quit:");
			opt = s.nextLine();
			switch (opt) {
			case "q":
				break out;
			case "":
				Document d = new Document();
				d.put("word_id", word_id);
				d.put("symbol", arr[0]);
				d.put("meaning", arr[1]);
				d.put("create_time", sdf.format(new Date()));
				wordList.add(d);
				break;
			default:
				continue;
			}
		}
		s.close();
		if (!wordList.isEmpty()) {
			MongoClientURI uri = new MongoClientURI("mongodb://holden:testonly@ds227939.mlab.com:27939/holden_remote");
			MongoClient mongoClient = new MongoClient(uri);
			MongoDatabase mongoDatabase = mongoClient.getDatabase("holden_remote");
			MongoCollection<Document> collection = mongoDatabase.getCollection("word");
			collection.insertMany(wordList);
			mongoClient.close();
		}

	}

	static String[] getDefine(String word) throws IOException {

		String url = "http://www.iciba.com/index.php?a=getWordMean&c=search&list=1&word=";
		String charset = "UTF-8";

		HttpURLConnection connection = (HttpURLConnection) new URL(url + word).openConnection();
		connection.setRequestProperty("Accept-Charset", charset);
		InputStream response = connection.getInputStream();

		Scanner scanner = new Scanner(response);
		String responseBody = scanner.useDelimiter("\\A").next().replaceAll(" ", "");
		scanner.close();

		StringBuffer sb = new StringBuffer("");

		JsonObject jsonObject = new JsonParser().parse(responseBody).getAsJsonObject();

		JsonObject baesInfo = jsonObject.getAsJsonObject("baesInfo");

		if (baesInfo == null) {
			return new String[] { "", "NULL" };
		}

		JsonArray symbols = baesInfo.getAsJsonArray("symbols");

		if (symbols == null) {
			return new String[] { "", "NULL" };
		}

		String ph_am = "";

		for (JsonElement e : symbols) {

			ph_am = e.getAsJsonObject().get("ph_am").getAsString();

			JsonArray array = e.getAsJsonObject().getAsJsonArray("parts");

			for (JsonElement o : array) {
				String part = o.getAsJsonObject().get("part").getAsString();
				sb.append(part);

				JsonArray means = o.getAsJsonObject().getAsJsonArray("means");

				for (JsonElement s : means) {
					sb.append(s.getAsString() + ";");
				}
				sb.setCharAt(sb.length() - 1, ' ');

			}
		}

		return new String[] { ph_am, sb.toString().replaceAll("，", ",").replaceAll("（", "(").replaceAll("）", ")")
				.replaceAll("〈", "<").replaceAll("〉", ">").replaceAll("、", ",") };
	}

	static String translate(String chineseWord) throws IOException {

		String url = "http://fy.iciba.com/ajax.php?a=fy&f=zh&t=en&w=";
		String charset = "UTF-8";

		HttpURLConnection connection = (HttpURLConnection) new URL(url + chineseWord).openConnection();
		connection.setRequestProperty("Accept-Charset", charset);
		InputStream response = connection.getInputStream();

		Scanner scanner = new Scanner(response);
		String responseBody = scanner.useDelimiter("\\A").next();
		scanner.close();

		JsonObject jsonObject = new JsonParser().parse(responseBody).getAsJsonObject();

		JsonObject content = jsonObject.getAsJsonObject("content");

		JsonElement out = content.get("out");
		if (out == null) {
			return "No Result";
		}

		return out.getAsString();

	}

}
