package com.example.pogoda;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Pattern;

public class pogoda extends Application {

    private static final String API_KEY = "ff90414bbf3ac820348bcc77183616bb";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    private TextField cityTextField;
    private TextArea resultTextArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pogodynka");

        // Header
        Label headerLabel = new Label("Twoja pogoda na dzisiaj");
        headerLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        // City Input
        cityTextField = new TextField();
        cityTextField.setPromptText("Wprowadź nazwę miasta");

        // Search Button
        Image ikona = new Image("https://cdn3.iconfinder.com/data/icons/feather-5/24/search-256.png");
        ImageView lupa = new ImageView(ikona);
        Button searchButton = new Button();
        searchButton.setGraphic(lupa);
        lupa.setFitHeight(18);
        lupa.setFitWidth(18);
        searchButton.setOnAction(e -> searchWeather());

        // Result TextArea
        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);

        // Close Button
        Button closeButton = new Button("Zamknij");
        closeButton.setOnAction(e -> primaryStage.close());

        // Layout
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(headerLabel, cityTextField, searchButton, resultTextArea, closeButton);

        // Scene
        Scene scene = new Scene(layout, 650, 700);
        primaryStage.setScene(scene);

        // Show Stage
        primaryStage.show();
    }

    private void searchWeather() {
        String cityName = cityTextField.getText();

        try {
            validateCityName(cityName);

            String apiUrl = String.format("%s?q=%s&appid=%s&lang=pl&units=metric", API_URL, cityName, API_KEY);

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 404) {
                throw new IOException("Nie znaleziono miasta."); // Ochrona przed błędną nazwą miasta
            }

            if (connection.getResponseCode() != 200) {
                throw new IOException("Błąd połączenia: " + connection.getResponseMessage());
            }

            InputStream responseStream = connection.getInputStream();
            Scanner scanner = new Scanner(responseStream).useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";

            JSONObject json = new JSONObject(responseBody);
            displayWeatherInfo(json);

        } catch (IllegalArgumentException e) {
            resultTextArea.setText(e.getMessage());
        } catch (IOException e) {
            resultTextArea.setText("Brak wyniku. " + e.getMessage());
        } catch (Exception e) {
            resultTextArea.setText("Wystąpił błąd: " + e.getMessage());
        }
    }

    private void validateCityName(String cityName) {
        if (cityName.isEmpty()) {
            throw new IllegalArgumentException("Pole nazwy miasta nie może być puste."); //Ochrona przed zostawieniem pustego pola wyszukiwania
        }
        // Ochrona przed wpisaniem znaku specjalnego i liczby
        if (!Pattern.matches("^[a-zA-Z\\s]+$", cityName)) {
            throw new IllegalArgumentException("Nazwa miasta nie może zawierać znaków specjalnych ani cyfr.");
        }
    }

    private void displayWeatherInfo(JSONObject json) {
        String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
        JSONObject mainSection = json.getJSONObject("main");

        // Pobierz wartość "speed" jako liczba
        double windSpeed = json.getJSONObject("wind").getDouble("speed");
        String windSpeedString = String.format("%.2f", windSpeed);

        String windDirection = getWindDirection(json.getJSONObject("wind").optDouble("deg"));
        String rainInfo = getRainSnowInfo(json, "rain");
        String snowInfo = getRainSnowInfo(json, "snow");

        // Pobierz wartość "all" jako liczba całkowita
        int clouds = json.getJSONObject("clouds").getInt("all");
        String cityName = json.getString("name");

        String result = String.format("Opis: %s\nTemperatura: %.2f°C\nWilgotność: %d%%\nCiśnienie: %.2f hPa\nOdczuwalna: %.2f°C\nMin. temperatura: %.2f°C\nMax. temperatura: %.2f°C\nPrędkość wiatru: %s m/s\nKierunek wiatru: %s\nDeszcz: %s\nŚnieg: %s\nZachmurzenie: %d%%\nMiasto: %s",
                description, mainSection.getDouble("temp"), mainSection.getInt("humidity"),
                mainSection.getDouble("pressure"), mainSection.getDouble("feels_like"),
                mainSection.getDouble("temp_min"), mainSection.getDouble("temp_max"),
                windSpeedString, windDirection, rainInfo, snowInfo, clouds, cityName);

        resultTextArea.setText(result);
    }

    private String getWindDirection(double degree) {
        if (degree >= 337.5 || degree < 22.5) {
            return "N";
        } else if (degree >= 22.5 && degree < 67.5) {
            return "NE";
        } else if (degree >= 67.5 && degree < 112.5) {
            return "E";
        } else if (degree >= 112.5 && degree < 157.5) {
            return "SE";
        } else if (degree >= 157.5 && degree < 202.5) {
            return "S";
        } else if (degree >= 202.5 && degree < 247.5) {
            return "SW";
        } else if (degree >= 247.5 && degree < 292.5) {
            return "W";
        } else {
            return "NW";
        }
    }

    private String getRainSnowInfo(JSONObject json, String key) {
        if (json.optJSONObject(key) != null) {
            return json.getJSONObject(key).toString();
        } else {
            return "Brak danych";
        }
    }
}
