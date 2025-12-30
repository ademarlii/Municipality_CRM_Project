package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.TestcontainersConfiguration;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=6969",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=update"
        }
)
@Import(TestcontainersConfiguration.class)
public class RegisterUiE2ETest {

    private static final String UI_BASE_URL = System.getProperty("ui", "http://localhost:5173");
    private static final String BACKEND_BASE_URL = System.getProperty("backend", "http://localhost:6969");

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String REGISTER_EMAIL            = "[data-testid='auth-register-email']";
    private static final String REGISTER_PHONE            = "[data-testid='auth-register-phone']";
    private static final String REGISTER_PASSWORD         = "[data-testid='auth-register-password']";
    private static final String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";
    private static final String REGISTER_SUBMIT           = "[data-testid='auth-register-submit']";





    @BeforeEach
    public void setUp() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver= new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }


    @Test
    void registerSuccess() {
        String email="adembava@gmail.com";
        String phone="5555555554";
        String password="Password123!";

        driver.get(UI_BASE_URL + "/auth/register");
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_EMAIL)));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement phoneInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector(REGISTER_PHONE)));
        phoneInput.clear();
        phoneInput.sendKeys(phone);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PASSWORD)));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        WebElement passwordConfirmInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PASSWORD_CONFIRM)));
        passwordConfirmInput.clear();
        passwordConfirmInput.sendKeys(password);

        WebElement submitButton=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_SUBMIT)
        ));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("citizen"));
    }

}
