//package com.ademarli.municipality_service.integration.e2e.ui.auth;
//
//import io.github.bonigarcia.wdm.WebDriverManager;
//import org.junit.jupiter.api.*;
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.*;
//
//import java.net.URI;
//import java.net.http.*;
//import java.time.Duration;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class LoginUiE2ETest {
///// Case açıklaması: Citizen kullanıcısının UI üzerinden başarılı bir şekilde giriş yapabilmesi ve /citizen/complaints sayfasına yönlendirilmesi test ediliyor.
///// Önce API üzerinden benzersiz bir email ve telefon ile kayıt yapılıyor. Ardından UI otomasyonu ile login sayfasına gidilip, kayıtlı email ve şifre girilerek giriş yapılıyor.
/////  Giriş sonrası URL'nin /citizen/complaints içerip içermediği doğrulanıyor.
//
//    private static final String UI_BASE = System.getProperty("ui.baseUrl", "http://localhost:5173");
//    private static final String API_BASE = System.getProperty("api.baseUrl", "http://localhost:6969");
//
//    private WebDriver driver;
//    private WebDriverWait wait;
//
//    private static final String TID_EMAIL_OR_PHONE = "auth-login-emailOrPhone";
//    private static final String TID_PASSWORD       = "auth-login-password";
//    private static final String TID_SUBMIT         = "auth-login-submit";
//
//
//    private static final long SLOW_MS = Long.parseLong(System.getProperty("slow.ms", "700"));
//
//    private void pause() {
//        if (SLOW_MS <= 0) return;
//        try {
//            Thread.sleep(SLOW_MS);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    @BeforeEach
//    void setup() {
//        WebDriverManager.chromedriver().setup();
//
//        ChromeOptions options = new ChromeOptions();
//        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
//        if (headless) options.addArguments("--headless=new");
//
//        options.addArguments("--window-size=1440,900");
//        options.addArguments("--disable-gpu");
//        options.addArguments("--no-sandbox");
//
//        driver = new ChromeDriver(options);
//        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//    }
//
//    @AfterEach
//    void teardown() {
//        if (driver != null){
//            pause();
//            driver.quit();
//        }
//
//    }
//
//    @Test
//    void citizen_login_ok_shouldRedirectToCitizenComplaints() {
//        String uniq = UUID.randomUUID().toString().substring(0, 6);
//        String email = "citizen_" + uniq + "@test.com";
//        String phone = "555000" + (int)(Math.random() * 1000);
//        String pass  = "Pass12345!";
//
//        registerCitizenViaApi(email, phone, pass);
//
//        driver.get(UI_BASE + "/auth/login");
//
//        typeByTestId(TID_EMAIL_OR_PHONE, email);
//        pause();
//
//        typeByTestId(TID_PASSWORD, pass);
//        pause();
//
//        clickByTestId(TID_SUBMIT);
//        pause();
//
//        assertTrue(wait.until(ExpectedConditions.urlContains("/citizen/complaints")),
//                "Login sonrası /citizen/complaints bekleniyordu. URL=" + driver.getCurrentUrl());
//    }
//
//
//    private void typeByTestId(String testId, String value) {
//        By by = By.cssSelector("[data-testid='" + testId + "']");
//        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
//        el.clear();
//        el.sendKeys(value);
//    }
//
//    private void clickByTestId(String testId) {
//        By by = By.cssSelector("[data-testid='" + testId + "']");
//        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(by));
//        btn.click();
//    }
//
//    private void registerCitizenViaApi(String email, String phone, String password) {
//        try {
//            String body = """
//              {
//                "email": "%s",
//                "phone": "%s",
//                "password": "%s"
//              }
//            """.formatted(email, phone, password);
//
//            HttpRequest req = HttpRequest.newBuilder()
//                    .uri(URI.create(API_BASE + "/api/auth/register"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(body))
//                    .build();
//
//            HttpClient client = HttpClient.newHttpClient();
//            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
//
//            int s = res.statusCode();
//            if (!(s == 200 || s == 201 || s == 409)) {
//                fail("Register API failed: status=" + s + " body=" + res.body());
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//}
