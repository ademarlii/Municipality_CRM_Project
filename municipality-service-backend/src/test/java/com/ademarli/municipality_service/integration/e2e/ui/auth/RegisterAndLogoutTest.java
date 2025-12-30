package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.TestcontainersConfiguration;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=6969",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@Import(TestcontainersConfiguration.class)
public class RegisterAndLogoutTest {
/// Case Açıklaması
/// Kullanıcı kayıt sayfasına gider, geçerli bilgilerle kayıt olur, ardından menüyü açar ve çıkış yapar. Başarılı bir şekilde çıkış yapıldığını doğrular.
    private static final String UI_BASE_URL = System.getProperty("ui", "http://localhost:5173");

    private static final String REGISTER_EMAIL = "[data-testid='auth-register-email']";
    private static final String REGISTER_PHONE = "[data-testid='auth-register-phone']";
    private static final String REGISTER_PASSWORD = "[data-testid='auth-register-password']";
    private static final String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";
    private static final String REGISTER_SUBMIT = "[data-testid='auth-register-submit']";

    private static final By TOAST = By.cssSelector("[data-testid='toast']");
    private static final By TOAST_CLOSE = By.cssSelector("[data-testid='toast-close']");

    private WebDriver driver;
    private WebDriverWait wait;

//    @Autowired
//    UserRepository userRepository;

    @BeforeEach
    void setup() {
       // userRepository.deleteAll();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "true"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    void registerSuccess_and_logoutSucces() {
        String email = "adembaya@gmail.com";
        String phone = "5555549559";
        String password = "Password123!";

        driver.get(UI_BASE_URL + "/auth/register");

        type(REGISTER_EMAIL, email);
        type(REGISTER_PHONE, phone);
        type(REGISTER_PASSWORD, password);
        type(REGISTER_PASSWORD_CONFIRM, password);

        driver.findElement(By.cssSelector(REGISTER_SUBMIT)).click();

        dismissToastIfPresent();

        WebElement openMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("open-menu")));
        safeClick(openMenu);

        WebElement logoutMenuItem = wait.until(ExpectedConditions.elementToBeClickable(By.className("auth-logout")));
        logoutMenuItem.click();

        wait.until(ExpectedConditions.urlContains("/auth/login"));
    }

    private void type(String cssSelector, String value) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
        el.clear();
        el.sendKeys(value);
    }

    private void dismissToastIfPresent() {
        List<WebElement> toasts = driver.findElements(TOAST);
        if (toasts.isEmpty()) return;

        try {
            List<WebElement> closeButtons = driver.findElements(TOAST_CLOSE);
            if (!closeButtons.isEmpty()) {
                wait.until(ExpectedConditions.elementToBeClickable(TOAST_CLOSE)).click();
            }

            wait.until(ExpectedConditions.invisibilityOfElementLocated(TOAST));
        } catch (TimeoutException ignored) {

        }
    }

    private void safeClick(WebElement el) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }
}
