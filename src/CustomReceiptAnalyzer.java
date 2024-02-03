import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomReceiptAnalyzer {
    private static final String CUSTOM_RECEIPT_UR = "https://interview-task-api.mca.dev/qr-scanner-codes/alpha-qr-gFpwhsQ8fkY1";

    public static void main(String[] args) {
        try {
            String customReceiptJson = fetchCustomReceiptDetails();
            if (customReceiptJson != null && !customReceiptJson.isEmpty()) {
                List<CustomProduct> customProducts = parseCustomReceiptJson(customReceiptJson);

                if (!customProducts.isEmpty()) {
                    displayCustomReport(customProducts);
                } else {
                    System.out.println("Failed to parse receipt details.");
                }
            } else {
                System.out.println("Failed to fetch receipt details.");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String fetchCustomReceiptDetails() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CUSTOM_RECEIPT_UR))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static List<CustomProduct> parseCustomReceiptJson(String json) {
        List<CustomProduct> customProducts = new ArrayList<>();
        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

        for (JsonElement element : jsonArray) {
            JsonObject productJson = element.getAsJsonObject();

            String name = productJson.has("name") ? productJson.get("name").getAsString() : "N/A";
            BigDecimal price = productJson.has("price") ? productJson.get("price").getAsBigDecimal() : BigDecimal.ZERO;
            boolean isDomestic = productJson.has("domestic") && productJson.get("domestic").getAsBoolean();
            String description = productJson.has("description") ? productJson.get("description").getAsString() : "N/A";
            String weight = productJson.has("weight") ? productJson.get("weight").getAsString() : "N/A";

            CustomProduct product = new CustomProduct(name, price, isDomestic, description, weight);
            customProducts.add(product);
        }
        
        Collections.sort(customProducts);

        return customProducts;
    }

    private static void displayCustomReport(List<CustomProduct> products) {
        List<CustomProduct> domesticProducts = new ArrayList<>();
        List<CustomProduct> importedProducts = new ArrayList<>();

        for (CustomProduct product : products) {
            if (product.isSpecial()) {
                domesticProducts.add(product);
            } else {
                importedProducts.add(product);
            }
        }

        System.out.println(". Domestic");
        displayCustomGroupedProducts(domesticProducts);

        System.out.println(". Imported");
        displayCustomGroupedProducts(importedProducts);

        System.out.println("Domestic cost: $" + calculateCustomTotalCost(domesticProducts));

        System.out.println("Imported cost: $" + calculateCustomTotalCost(importedProducts));

        System.out.println("Domestic count: " + domesticProducts.size());
        System.out.println("Imported count: " + importedProducts.size());
    }

    private static void displayCustomGroupedProducts(List<CustomProduct> products) {
        for (CustomProduct product : products) {
            System.out.println("... " + product.customName());
            System.out.println("Price: $" + formatPriceAsString(product.customPrice()));
            System.out.println(product.customDescription());

            String formattedWeight = "N/A".equals(product.customWeight()) ? "N/A" : product.customWeight() + "g";
            System.out.println("Weight: " + formattedWeight);
        }
    }

    private static String calculateCustomTotalCost(List<CustomProduct> products) {
        BigDecimal totalCost = products.stream().map(CustomProduct::customPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCost.toString().replace('.', ',');
    }

    private static String formatPriceAsString(BigDecimal price) {
        return price.toString().replace('.', ',');
    }

    private record CustomProduct(String customName, BigDecimal customPrice, boolean isSpecial, String customDescription,
                           String customWeight) implements Comparable<CustomProduct> {
            private CustomProduct(String customName, BigDecimal customPrice, boolean isSpecial, String customDescription, String customWeight) {
                this.customName = customName.length() > 10 ? customName.substring(0, 10) : customName;
                this.customPrice = customPrice;
                this.isSpecial = isSpecial;
                this.customDescription = customDescription;
                this.customWeight = customWeight;
            }

            @Override
            public int compareTo(CustomProduct other) {
                return this.customName.compareTo(other.customName);
            }
        }
}