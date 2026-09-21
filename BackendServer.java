import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class BackendServer {
    private static final int PORT = 8080;
    private static final String DB_URL = "jdbc:sqlite:placement.db";

    public static void main(String[] args) throws IOException {
        initDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/companies", new CompaniesHandler());
        server.createContext("/api/offers", new OfferAllocationHandler());
        server.createContext("/api/audits", new AuditHandler());

        server.setExecutor(null);
        System.out.println("==================================================");
        System.out.println(" Backend Running at: http://localhost:" + PORT);
        System.out.println(" Connected to SQLite Database: placement.db");
        System.out.println("==================================================");
        server.start();
    }


 // 1. Database & Tables Init
    private static void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                        "student_id INTEGER PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "branch TEXT NOT NULL, " +
                        "cgpa REAL NOT NULL, " +
                        "placement_status TEXT DEFAULT 'UNPLACED')");

                stmt.execute("CREATE TABLE IF NOT EXISTS companies (" +
                        "company_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "company_name TEXT NOT NULL, " +
                        "min_cgpa REAL NOT NULL, " +
                        "package_lpa REAL NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS placements (" +
                        "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "student_id INTEGER, " +
                        "company_id INTEGER, " +
                        "offer_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (student_id) REFERENCES students(student_id), " +
                        "FOREIGN KEY (company_id) REFERENCES companies(company_id))");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Enable CORS headers for browser
    private static void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
    }

    // --- Handler 1: Students (/api/students) ---
    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {

                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"branch\":\"%s\",\"cgpa\":%.2f,\"status\":\"%s\"}",
                                rs.getInt("student_id"), rs.getString("name"), rs.getString("branch"), rs.getDouble("cgpa"), rs.getString("placement_status")));
                        first = false;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange.getRequestBody());
                // Parse simple JSON: {"id":101,"name":"Aman","branch":"CSE","cgpa":8.5}
                try {
                    int id = Integer.parseInt(extractValue(body, "id"));
                    String name = extractValue(body, "name");
                    String branch = extractValue(body, "branch");
                    double cgpa = Double.parseDouble(extractValue(body, "cgpa"));

                    try (Connection conn = DriverManager.getConnection(DB_URL);
                         PreparedStatement ps = conn.prepareStatement("INSERT INTO students VALUES (?, ?, ?, ?, 'UNPLACED')")) {
                        ps.setInt(1, id);
                        ps.setString(2, name);
                        ps.setString(3, branch);
                        ps.setDouble(4, cgpa);
                        ps.executeUpdate();
                    }
                    sendResponse(exchange, 201, "{\"message\":\"Student registered\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    // --- Handler 2: Companies (/api/companies) ---
    static class CompaniesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM companies")) {

                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"minCgpa\":%.2f,\"packageLpa\":%.2f}",
                                rs.getInt("company_id"), rs.getString("company_name"), rs.getDouble("min_cgpa"), rs.getDouble("package_lpa")));
                        first = false;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());

            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange.getRequestBody());
                try {
                    String name = extractValue(body, "name");
                    double minCgpa = Double.parseDouble(extractValue(body, "minCgpa"));
                    double packageLpa = Double.parseDouble(extractValue(body, "packageLpa"));

                    try (Connection conn = DriverManager.getConnection(DB_URL);
                         PreparedStatement ps = conn.prepareStatement("INSERT INTO companies (company_name, min_cgpa, package_lpa) VALUES (?, ?, ?)")) {
                        ps.setString(1, name);
                        ps.setDouble(2, minCgpa);
                        ps.setDouble(3, packageLpa);
                        ps.executeUpdate();
                    }
                    sendResponse(exchange, 201, "{\"message\":\"Company registered\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    // --- Handler 3: Offer Allocation & ACID Transaction (/api/offers) ---
    static class OfferAllocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange.getRequestBody());
                try {
                    int studentId = Integer.parseInt(extractValue(body, "studentId"));
                    int companyId = Integer.parseInt(extractValue(body, "companyId"));

                    try (Connection conn = DriverManager.getConnection(DB_URL)) {
                        conn.setAutoCommit(false); // ACID Transaction
                        try {
                            try (PreparedStatement ps1 = conn.prepareStatement("INSERT INTO placements (student_id, company_id) VALUES (?, ?)")) {
                                ps1.setInt(1, studentId);
                                ps1.setInt(2, companyId);
                                ps1.executeUpdate();
                            }

                            try (PreparedStatement ps2 = conn.prepareStatement("UPDATE students SET placement_status = 'PLACED' WHERE student_id = ?")) {
                                ps2.setInt(1, studentId);
                                ps2.executeUpdate();
                            }

                            conn.commit();
                            sendResponse(exchange, 200, "{\"message\":\"Offer Allocated Successfully\"}");
                        } catch (SQLException err) {
                            conn.rollback();
                            sendResponse(exchange, 500, "{\"error\":\"Transaction Rollback: " + err.getMessage() + "\"}");
                        }
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    // --- Handler 4: Audit Reports (/api/audits) ---
    static class AuditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                String sql = "SELECT p.record_id, s.name AS sname, s.branch, c.company_name, c.package_lpa, p.offer_date " +
                             "FROM placements p " +
                             "JOIN students s ON p.student_id = s.student_id " +
                             "JOIN companies c ON p.company_id = c.company_id ORDER BY p.record_id DESC";

                try (Connection conn = DriverManager.getConnection(DB_URL);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"recordId\":%d,\"studentName\":\"%s\",\"branch\":\"%s\",\"companyName\":\"%s\",\"packageLpa\":%.2f,\"date\":\"%s\"}",
                                rs.getInt("record_id"), rs.getString("sname"), rs.getString("branch"), rs.getString("company_name"), rs.getDouble("package_lpa"), rs.getString("offer_date")));
                        first = false;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            }
        }
    }

    // --- Utilities ---
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String readBody(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":\"?([^,\"}]+)\"?";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }
}
