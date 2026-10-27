
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;

public class FlightDataProcessor {

    public static void main(String[] args) {
        String csvFilePath = "flights_sample_3m.csv";

        long totalRecords = 0;
        long congestedFlights = 0;
        long cancelledFlights = 0;

        System.out.println("Bắt đầu nạp và xử lý kho dữ liệu...");
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFilePath))
                .withSkipLines(1)
                .build()) {

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                totalRecords++;

                String taxiOutStr = nextLine[14];
                if (taxiOutStr != null && !taxiOutStr.trim().isEmpty()) {
                    try {
                        double taxiOutTime = Double.parseDouble(taxiOutStr);
                        if (taxiOutTime > 20) {
                            congestedFlights++;
                        }
                    } catch (NumberFormatException ignored) {
                        // Bỏ qua nếu dữ liệu rỗng hoặc sai định dạng
                    }
                }

                String cancelledStr = nextLine[21];
                if ("1".equals(cancelledStr)) {
                    cancelledFlights++;
                }
            }

            long endTime = System.currentTimeMillis();

            System.out.println("--------------------------------------------------");
            System.out.println("BÁO CÁO PHÂN TÍCH LƯU LƯỢNG VÀ NGHẼN TẢI");
            System.out.println("--------------------------------------------------");
            System.out.printf("Tổng số chuyến bay đã xử lý: %,d chuyến%n", totalRecords);
            System.out.printf("Số chuyến bị nghẽn (Taxi-out > 20p): %,d chuyến%n", congestedFlights);
            System.out.printf("Số chuyến bay bị hủy: %,d chuyến%n", cancelledFlights);
            System.out.printf("Thời gian thực thi: %d giây%n", (endTime - startTime) / 1000);

        } catch (IOException | CsvValidationException e) {
            System.err.println("Lỗi khi đọc file: " + e.getMessage());
        }
    }
}