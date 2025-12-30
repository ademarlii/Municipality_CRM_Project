package com.ademarli.municipality_service.integration.e2e.ui.auth;

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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginAndCreateComplaint {

    private static final String UI_BASE = System.getProperty("ui.baseUrl", "http://localhost:5173");

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    private static final String CreateComplaintButton    = "[data-testid='citizen-new-complaint']";
    private static final String SelectDepartment         = ".citizen-create-departmentId";
    private static final String SelectKategori           = ".citizen-create-categoryId";
    private static final String Title                    = "[data-testid='citizen-create-title']";
    private static final String Description              = "[data-testid='citizen-create-description']";
    private static final String CreateComplaintSubmit    = "[data-testid='citizen-create-submit']"; // ✅ düzeltildi
    private static final String ComplaintSuccessMessage  = "[data-testid='tracking-code-display']";

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    @Test
    void succesLogin_and_successCreateComplaint() {
        driver.get(UI_BASE + "/auth/login");

        // login
        WebElement emailOrPhoneInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TID_EMAIL_OR_PHONE))
        );
        emailOrPhoneInput.sendKeys("adem@gmail.com");

        WebElement passwordInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TID_PASSWORD))
        );
        passwordInput.sendKeys("123456");

        WebElement submitButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(TID_SUBMIT))
        );
        submitButton.click();
//şikayet oluşturmaya git
        WebElement createComplaintButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(CreateComplaintButton))
        );
        createComplaintButton.click();

        wait.until(ExpectedConditions.urlContains("/citizen/complaints/new"));
//Departman ve kategori seçimi
        selectMuiOptionByText(SelectDepartment, "Temizlik İşleri");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SelectKategori)));

        selectMuiOptionByText(SelectKategori, "Çöp Toplama");

        WebElement titleInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(Title))
        );
//Başlık ve     açıklama gir
        titleInput.sendKeys("Test otomasyon şikayet başlığıdır bu kaydeşş");

        WebElement descriptionInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(Description))
        );
        descriptionInput.sendKeys("Test otomasyon şikayet açıklamasıdır bu kardeişşş");

        WebElement createComplaintSubmitButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(CreateComplaintSubmit))
        );
        createComplaintSubmitButton.click();
//Şikayet oluşturulduktan sonra takip kodunu doğrula
        WebElement trackingCodeDisplay = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(ComplaintSuccessMessage))
        );
        String trackingCodeText = trackingCodeDisplay.getText();

        assertTrue(trackingCodeText.startsWith("TRK"));
    }

    private void selectMuiOptionByText(String selectCss, String optionText) {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selectCss)));
        select.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ul[role='listbox']")));

        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[@role='option' and normalize-space(.)='" + optionText + "']")
        ));
        option.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("ul[role='listbox']")));
    }
}
