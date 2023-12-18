import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PostcodeLookup {

    private static final String BASE_URL = "https://api.postcodes.io/postcodes/";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a full UK postcode:");
        String postcode = scanner.nextLine().trim().replaceAll("\\s+", " "); // Normalize space

        if (!isValidPostcode(postcode)) {
            System.out.println("Invalid postcode format.");
            return;
        }

        lookupPostcode(postcode);
    }

    private static boolean isValidPostcode(String postcode) {
        // Regex to validate both full postcodes with or without space
        String regex = "([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z])))) ?[0-9][A-Za-z]{2})";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(postcode).matches();
    }

    private static void lookupPostcode(String postcode) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + postcode.replace(" ", ""))) // Remove space for API call
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(PostcodeLookup::formatResponse)
                .exceptionally(e -> "Error: " + e.getMessage()) // Handling network/API errors
                .thenAccept(System.out::println)
                .join();
    }

    private static String formatResponse(String response) {
        JSONObject jsonResponse = new JSONObject(response);
        if (jsonResponse.getInt("status") != 200) {
            return "Error retrieving data for postcode: " + jsonResponse.getString("error");
        }

        JSONObject result = jsonResponse.getJSONObject("result");
        return formatPostcodeData(result);
    }

    private static String formatPostcodeData(JSONObject data) {
        StringBuilder formattedData = new StringBuilder();
        formattedData.append("Postcode: ").append(data.getString("postcode")).append("\n");
        formattedData.append("Area: ").append(data.getString("lsoa")).append("\n");
        formattedData.append("District: ").append(data.getString("admin_district")).append("\n");
        formattedData.append("Country: ").append(data.getString("country")).append("\n");
        // Add more fields

        return formattedData.toString();
    }

    // Add methods
}
