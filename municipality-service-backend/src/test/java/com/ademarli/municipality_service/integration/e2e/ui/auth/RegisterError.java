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
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@Import(TestcontainersConfiguration.class)
public class RegisterError {

///Case açıklaması
/// Burda kullanıcı giriş aşamasında gerekli bilgilerin girilmediği zaman formik yuptan gelen hataların doğruluğunu test  ediyoruz.
/// Birinci testte telefon numarası alanının boş bırakılması durumunda gerekli hata mesajının gelip gelmediğini kontrol ediyoruz.
/// İkinci testte ise şifre ve şifre onay alanlarının birbirleriyle uyuşmaması durumunda doğru hata mesajının gelip gelmediğini kontrol ediyoruz.

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
    void registerError_emptyPhoneFields() {
        driver.get("http://localhost:5173/auth/register");


        WebElement email=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_EMAIL
        )));
        email.clear();
        email.sendKeys("adem@gmail.com");
        WebElement phone=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PHONE
        )));
        phone.clear();
        phone.sendKeys("");

        email.click();

        WebElement err = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='auth-register-phone-error']")
        ));
        assert err.getText().contains("gerekli");



    }

    @Test
    void registerError_passwordMismatch(){

        driver.get("http://localhost:5173/auth/register");

        WebElement email=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_EMAIL
        )));
        email.clear();
        email.sendKeys("adem@gmail.com");
        WebElement phone=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PHONE
        )));
        phone.clear();
        phone.sendKeys("5551234567");
        WebElement password=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PASSWORD
        )));
        password.clear();
        password.sendKeys("Password123");
        WebElement confirm=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(REGISTER_PASSWORD_CONFIRM
        )));
        confirm.clear();
        confirm.sendKeys("Password321");
        password.click();


        WebElement err= wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='auth-register-confirm-error']")
        ));

        assert err.getText().equals("Şifreler eşleşmiyor");
    }




}
