import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DataWarehouseLoader {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/flight_dw?useSSL=false&rewriteBatchedStatements=true";
        String user = "root";
        String password = "";

        System.out.println("Bắt đầu quy trình ETL vào Data Warehouse...");
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            // Xóa  dữ liệu cũ
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("TRUNCATE TABLE staging_flights");
                stmt.executeUpdate("TRUNCATE TABLE fact_flight_operations");
                System.out.println("Đã làm sạch dữ liệu cũ trong cơ sở dữ liệu.");
            }

            String sqlStaging = "INSERT INTO staging_flights (Flight_Date, Airline_Code, Flight_Number, Origin_Airport, Origin_City, Dest_Airport, Dest_City, Dep_Delay, Taxi_Out, Cancelled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStaging);
                 CSVReader reader = new CSVReaderBuilder(new FileReader("flights_sample_3m.csv")).withSkipLines(1).build()) {

                String[] line;
                int count = 0;
                int batchSize = 10000;

                while ((line = reader.readNext()) != null) {
                    pstmt.setString(1, line[1]);  // FL_DATE (Ngày tháng)
                    pstmt.setString(2, line[4]);  // AIRLINE_CODE (Mã hãng: UA, DL...)
                    pstmt.setString(3, line[6]);  // FL_NUMBER
                    pstmt.setString(4, line[7]);  // ORIGIN
                    pstmt.setString(5, line[8]);  // ORIGIN_CITY_NAME
                    pstmt.setString(6, line[9]);  // DEST
                    pstmt.setString(7, line[10]); // DEST_CITY_NAME

                    pstmt.setDouble(8, parseDoubleSafe(line[13])); // DEP_DELAY
                    pstmt.setDouble(9, parseDoubleSafe(line[14])); // TAXI_OUT
                    pstmt.setInt(10, "1".equals(line[21]) ? 1 : 0); // CANCELLED

                    pstmt.addBatch();
                    count++;

                    if (count % batchSize == 0) {
                        pstmt.executeBatch();
                        System.out.println("Đã nạp thành công " + count + " dòng vào Staging...");
                    }
                }
                pstmt.executeBatch();
                conn.commit();
                System.out.println("Hoàn tất nạp " + count + " dòng vào Staging!");
            }

            System.out.println("Đang đồng bộ dữ liệu sang bảng Fact (fact_flight_operations)...");
            String sqlFact = "INSERT INTO fact_flight_operations (Flight_Date, Airline_Code, Flight_Number, Origin_Airport, Dest_Airport, Dep_Delay, Taxi_Out, Cancelled) " +
                    "SELECT Flight_Date, Airline_Code, Flight_Number, Origin_Airport, Dest_Airport, Dep_Delay, Taxi_Out, Cancelled FROM staging_flights";

            try (Statement stmtFact = conn.createStatement()) {
                int rowsInserted = stmtFact.executeUpdate(sqlFact);
                conn.commit();
                System.out.println("Đã đồng bộ thành công " + rowsInserted + " dòng sang Fact Table!");
            }

            long endTime = System.currentTimeMillis();
            System.out.println("------------------------------------------");
            System.out.println("HOÀN TẤT TOÀN BỘ QUY TRÌNH ETL!");
            System.out.println("Thời gian thực thi: " + (endTime - startTime)/1000 + " giây");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.replace("\"", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}