import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PostcodeLookup {

    // Base URL for the postcodes.io API
    private static final String BASE_URL = "https://api.postcodes.io/postcodes/";
    // HttpClient to send requests to the postcodes.io API
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        // Scanner for reading input from the console
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a full or partial UK postcode:");
        // Reading the user input, trimming whitespace, and normalizing spaces
        String postcode = scanner.nextLine().trim().replaceAll("\\s+", " ");

        // Check if the input is a valid full or partial postcode
        if (!isValidPostcode(postcode) && !isValidPartialPostcode(postcode)) {
            System.out.println("Not a valid full or partial UK postcode.");
            return;
        }

        // Determines if the postcode is full or partial and calls the appropriate method
        if (isFullPostcode(postcode)) {
            lookupFullPostcode(postcode.replace(" ", ""));
        } else {
            autocompletePartialPostcode(postcode);
        }
    }

    // Checks if the given postcode is a full postcode
    private static boolean isFullPostcode(String postcode) {
        // Regular expression for validating full UK postcodes
        String fullPostcodeRegex = "([Gg][Ii][Rr] ?0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z])))) ?[0-9][A-Za-z]{2})";
        Pattern pattern = Pattern.compile(fullPostcodeRegex);
        return pattern.matcher(postcode).matches();
    }

    // Validates the format of a full postcode
    private static boolean isValidPostcode(String postcode) {
        // Regex for validating both full postcodes with or without space
        String regex = "([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z])))) ?[0-9][A-Za-z]{2})";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(postcode).matches();
    }

    // Validates the format of a partial postcode
    private static boolean isValidPartialPostcode(String postcode) {
        // Regular expression for a valid partial postcode
        String partialPostcodeRegex = "^[A-Z]{1,2}[0-9R][0-9A-Z]?$";
        Pattern pattern = Pattern.compile(partialPostcodeRegex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(postcode).matches();
    }

    // Handles the autocomplete for partial postcodes
    private static void autocompletePartialPostcode(String outcode) {
        // Builds the request for the autocomplete endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + outcode + "/autocomplete"))
                .build();
        // Sends the request asynchronously, processes the response and prints the suggestions
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(PostcodeLookup::formatAutocompleteResponse)
                .exceptionally(e -> "Error: " + e.getMessage())
                .thenAccept(System.out::println)
                .join();
    }

    // Looks up a full postcode
    private static void lookupFullPostcode(String postcode) {
        // Builds the request for the full postcode lookup
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + postcode)) // Remove space for API call
                .build();
        // Sends the request asynchronously, processes the response and prints the result
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(PostcodeLookup::formatResponse)
                .exceptionally(e -> "Error: " + e.getMessage()) // Handles network/API errors
                .thenAccept(System.out::println)
                .join();
    }

    // Formats the response
    private static String formatResponse(String response) {
        // Convert the string response to a JSONObject for easy data manipulation
        JSONObject jsonResponse = new JSONObject(response);
        // Check if the API response status is not successful (200)
        if (jsonResponse.getInt("status") != 200) {
            return "Error retrieving data for postcode: " + jsonResponse.getString("error");
        }
        // Extract the 'result' object from the JSON response which contains the postcode data
        JSONObject result = jsonResponse.getJSONObject("result");
        return formatPostcodeData(result);
    }

    private static String formatPostcodeData(JSONObject data) {
        // Initialize a StringBuilder to create a formatted string
        StringBuilder formattedData = new StringBuilder();
        // Append various pieces of information about the postcode to the StringBuilder
        formattedData.append("Postcode: ").append(data.getString("postcode")).append("\n");
        formattedData.append("Area: ").append(data.getString("lsoa")).append("\n");
        formattedData.append("District: ").append(data.getString("admin_district")).append("\n");
        formattedData.append("Country: ").append(data.getString("country")).append("\n");
        // Convert the StringBuilder to a String and return it
        return formattedData.toString();
    }

    private static String formatAutocompleteResponse(String response) {
        // Parse the string response into a JSONObject
        JSONObject jsonResponse = new JSONObject(response);
        // Check if the API response indicates a failure (status not equal to 200)
        if (jsonResponse.getInt("status") != 200) {
            return "Error retrieving autocomplete data: " + jsonResponse.getString("error");
        }
        // Extract the list of suggested postcodes from the JSON response
        JSONArray postcodes = jsonResponse.getJSONArray("result");
        if (postcodes.isEmpty()) {
            return "No suggestions found for this partial postcode.";
        }
        // Initialize a StringBuilder to create a formatted list of postcode suggestions
        StringBuilder suggestions = new StringBuilder("Suggested Full Postcodes:\n");
        for (int i = 0; i < postcodes.length(); i++) {
            suggestions.append(postcodes.getString(i)).append("\n");
        }
        // Return the string representation of the StringBuilder
        return suggestions.toString();
    }
}
