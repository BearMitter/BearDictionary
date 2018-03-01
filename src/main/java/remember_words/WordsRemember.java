package remember_words;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray; 
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WordsRemember {

	static Gson gson = new Gson();

	public static void main(String[] args) throws IOException, InterruptedException {
		FileWriter fw = new FileWriter("WordsQuery", true);
		Scanner s = new Scanner(System.in);
		String word;
		out: while (true) {
			System.out.println("Input word :");
			word = s.nextLine();
			if (word.equals("q"))
				break;

			String[] arr = WordsRemember.getDefine(word);
			
			if(arr[0].equals("")){		
				System.out.println(arr[1].equals("NULL")?"Found No Meanings!":arr[1].trim().replaceAll(" ", "\n"));
				continue;
			}

			System.out.println(
					arr[0] + "\n" + arr[1].trim().replaceAll(" ", "\n") + "\nPress ENTER->append, SPACE->skip, q->quit:");
			switch (s.nextLine()) {
			case " ":
				continue;
			case "q":
				break out;
			case "":
				fw.append(word + "\t" + arr[0] + "\t" + arr[1] + "\n");
				break;
			}
		}
		s.close();
		fw.close();

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

}
