// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.

import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static String seriesName; // so we can assign the series name during loops to save a copy of the name during that iteration.
    static CSVWriter writer;

    static List<String> defaultSeries = new ArrayList<String>(); // default series which can be expanded should we wish to increase the default series.

    public static void main(String[] args) {

        // main consists of checking the length of the args array, executing dependent on the size of the array.

        if (args.length == 1 || args.length == 2) { // if only 1 or 2 parameters are provided - exit the program.
            System.out.println("First two arguments must be <start date> <end-date> YYYY-MM-DD format followed by a maximum of 4 series\nIf no parameters are provided, default series will execute.");
            System.out.println("Start-date supplied: " + args[0]);
            System.out.println("End-date supplied: " + args[0]);
            System.out.println("No series provided.");
        }

        if (args.length == 0) { // check if the user has supplied arguments
            System.out.println("No parameters detected - initiating default configuration");
            // add default series to array list
            defaultSeries.add("FXCADUSD");
            defaultSeries.add("FXAUDCAD");

            File newFile = new File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "csv" + File.separator + "test.csv");

            if (newFile.length() > 0) { // either headers have been already written or data exists from previous write.
                System.out.println("Data found - skipping header write...");
                try {
                    Thread.sleep(2000); // we sleep the thread so the user can read the rolling updates.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (String series : defaultSeries) {
                    seriesName = series;
                    apiCall(series); // call overloaded method
                }

            } else {
                System.out.println("Writing headers...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    writeHeaders(writer);
                    for (String series : defaultSeries) {
                        seriesName = series;
                        apiCall(series);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (args.length > 6) {
            System.out.println("A maximum of 4 series allowed");
            System.exit(0);
        } else {
            // get the start and end date from the args array
            String startDate = args[0];
            String endDate = args[1];

            File newFile = new File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "csv" + File.separator + "test.csv");

            if (newFile.length() > 0) {
                System.out.println("Data found - skipping header write...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 2; i < args.length; i++) {
                    seriesName = args[i]; // on each iteration, we store the series name.
                    apiCall(args[i], startDate, endDate); // api call per series name.
                }
            } else {
                System.out.println("Writing headers...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    writeHeaders(writer);
                    for (int i = 2; i < args.length; i++) {
                        seriesName = args[i]; // on each iteration, we store the series name.
                        apiCall(args[i], startDate, endDate); // api call per series name.
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Operation completed - please navigate to My documents\\csv directory for requested data");
        }
    }

    public static void writeHeaders(CSVWriter writer) throws IOException { // open a new writer, set the path with file separators and add the final test.csv, using append to prevent overwriting.
        writer = new CSVWriter(new FileWriter(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "csv" + File.separator + "test.csv", true));
        String[] columns = {"Date", "Series", "Label", "Description", "Value"};
        writer.writeNext(columns); // write the columns.
        writer.close(); // close the writer to prevent errors.
    }

    public static void apiCall(String series) { // overloaded api call for default series.

        // confirm which series is being queried
        System.out.println("Querying: " + series);
        // setup client
        HttpClient client = HttpClient.newHttpClient();
        // build a request
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://www.bankofcanada.ca/valet/observations/" + series + "?recent_weeks=" + 1)).build(); // request to get recent week.
        //send request async using client
        //second param to tell server we want to receive response as string
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                //then apply means we want to apply a method to the previous results
                //double colon is used to invoke a method of the specified class, HttpResponse class, method: body
                .thenApply(HttpResponse::body) // Apply returns a value
                .thenAccept(Main::defaultOutputToFile) // Accept returns a void
                .join(); // must always use join to return the result.

    }

    public static void apiCall(String seriesName, String sDate, String eDate) {
        System.out.println("Querying: " + seriesName);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // setup client
        HttpClient client = HttpClient.newHttpClient();
        // build a request
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://www.bankofcanada.ca/valet/observations/" + seriesName + "?" + "start_date=" + sDate + "&" + "end_date=" + eDate)).build();
        //send request async using client
        //second param to tell server we want to receive response as string
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                //then apply means we want to apply a method to the previous results
                //double colon is used to invoke a method of the specificied class, HttpResponse class, method: body

                .thenApply(HttpResponse::body)
                .thenAccept(Main::outputToFile)
                .join(); // must always use join to return the result.
    }

    public static void defaultOutputToFile(String response) {
//      System.out.println("Default Response: " + response); during development - always log out parameter to confirm we're getting the data we want.

        String date = null;
        String label = null;
        String description = null;
        String value = null;

        // get data, value
        JSONObject observations = new JSONObject(response);
        JSONArray data = observations.getJSONArray("observations");

        System.out.println("Writing query to file for Series Name: " + seriesName);

        try {
            writer = new CSVWriter(new FileWriter(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "csv" + File.separator + "test.csv", true));

            JSONObject series = new JSONObject(response).getJSONObject("seriesDetail").getJSONObject(seriesName);
            description = series.getString("description");
            label = series.getString("label");

            for (int i = 0; i < data.length(); i++) {
                date = data.getJSONObject(i).getString("d");

                JSONObject record = data.getJSONObject(i).getJSONObject(seriesName);
                value = record.getString("v");
                String[] rowData = {date, seriesName, label, description, value};
                // writer prints data to file using writeNext
                writer.writeNext(rowData);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputToFile(String response) {
//        System.out.println("Response: " + response);

        // initialized to null method scope variables
        String date = null;
        String label = null;
        String description = null;
        String value = null;

        // isolating data based on their object keys.
        JSONObject seriesDetail = new JSONObject(response);
        // if message property exists - exit, else proceed.

        if (seriesDetail.has("message")) { // this checks if a given series name doesn't exist - api will return error message; this will then skip that incorrect series and move onto the next.
            System.out.println("Series not found");
        } else {
            label = seriesDetail.getJSONObject("seriesDetail").getJSONObject(seriesName).getString("label");
            description = seriesDetail.getJSONObject("seriesDetail").getJSONObject(seriesName).getString("description");

            JSONObject observations = new JSONObject(response);
            JSONArray data = observations.getJSONArray("observations");

            System.out.println("Writing query to file for Series Name: " + seriesName);

            try {
                writer = new CSVWriter(new FileWriter(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "csv" + File.separator + "test.csv", true));

                for (int i = 0; i < data.length(); i++) {
                    // get date
                    date = data.getJSONObject(i).getString("d");
                    // get value of series
                    JSONObject record = data.getJSONObject(i).getJSONObject(seriesName);
                    value = record.getString("v");
                    // building row data
                    String[] rowData = {date, seriesName, label, description, value};
                    // writer prints data to file using writeNext
                    writer.writeNext(rowData);
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
