import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class run {

    private static boolean checkCapacity(int maxCapacity, List<Map<String, String>> guests) {
        TreeMap<String, Integer> events = new TreeMap<>();

        for (Map<String, String> guest : guests) {
            String checkIn = guest.get("check-in");
            String checkOut = guest.get("check-out");

            if (isValidDate(checkIn) || isValidDate(checkOut)) {
                return false;
            }

            events.merge(checkIn, 1, Integer::sum);
            events.merge(checkOut, -1, Integer::sum);
        }

        int guestsAmount = 0;

        for (Map.Entry<String, Integer> entry : events.entrySet()) {
            guestsAmount += entry.getValue();

            if (guestsAmount > maxCapacity) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return false;
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();

        json = json.substring(1, json.length() - 1);

        // Регулярное выражение защищает от нахождения в json в объекте name символа ','
        // для этого с помощью lookahead разбиваем строку по запятой строго между объектами json
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");
            map.put(key, value);
        }

        return map;
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int maxCapacity = Integer.parseInt(scanner.nextLine());
            int n = Integer.parseInt(scanner.nextLine());

            List<Map<String, String>> guests = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String jsonGuest = scanner.nextLine();
                Map<String, String> guest = parseJsonToMap(jsonGuest);
                guests.add(guest);
            }

            boolean result = checkCapacity(maxCapacity, guests);
            System.out.println(result ? "True" : "False");
        }
    }
}
