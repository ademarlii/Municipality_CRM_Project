package com.ademarli.municipality_service.integration.e2e.ui.admin;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class AdminLoginAndCreateDepartmentCategory {


    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL         = "http://localhost:5173";

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    private static final String NAV_CAT = "[data-testid='nav-Kategoriler']";
    private static final String CREATE_CAT_BUTTON = "[data-testid='admin-category-new']";
    private static final String CATEGORY_NAME_INPUT = "[data-testid='admin-category-name']";

    private static final String SELECT_DEPARTMENT_DROPDOWN = ".admin-category-dropdown-department";
    private static final String SWITCH_BUTTON = "admin-category-status-switch";

    private static final String SAVE_CATEGORY_BUTTON = "[data-testid='admin-category-save']";


    private static final String ToasMessage="[data-testid:'toast']";
    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options=new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver=new ChromeDriver(options);
        wait=new WebDriverWait(driver, Duration.ofSeconds(10));

    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    private void loginAdmin(){

        driver.get(BASE_URL+"/auth/login");
        WebElement emailInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_EMAIL_OR_PHONE)
        ));

        emailInput.clear();
        emailInput.sendKeys("admin@local.com");
        WebElement passwordInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_PASSWORD)
        ));
        passwordInput.clear();
        passwordInput.sendKeys("Admin123!");
        WebElement submitButton=wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(TID_SUBMIT)
        ));
        submitButton.click();
    }

    private void pause(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void gotoAdminDepartmentCategoriesPage(){
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(NAV_CAT)
        )).click();
    }




    @Test
    void succesAdminLogin() {
        loginAdmin();
        gotoAdminDepartmentCategoriesPage();

        //wait.until(ExpectedConditions.urlContains("categories"));

        //yeni kategori ekle butonuna tıkla
        WebElement createCategoryButton=wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(CREATE_CAT_BUTTON)
        ));
        createCategoryButton.click();
        //kategori oluştur formu doldur
        WebElement categoryNameInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(CATEGORY_NAME_INPUT)
        ));
        categoryNameInput.clear();
        categoryNameInput.sendKeys("Yeni kategori");

        WebElement selectDepartmentDropdown=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(SELECT_DEPARTMENT_DROPDOWN)
        ));
        pause();
        selectDepartmentDropdown.click();
        pause();
        WebElement selectDepartmentOption=wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[contains(text(),'Temizlik İşleri')]")
        ));
        pause();
        selectDepartmentOption.click();

        pause();
        WebElement switchButton=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className(SWITCH_BUTTON)
        ));

        String status=switchButton.getText().trim();
        if(!status.equals("Aktif")){
            log.info("Kategori varsayılan olarak aktif geliyor.");
        }else{
            switchButton.click();
        }
        pause();
        WebElement saveCategoryButton=wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(SAVE_CATEGORY_BUTTON)
        ));
        saveCategoryButton.click();
        pause();

        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='toast-error']")
        ));

        String msg = toast.getAttribute("data-message");
       // assertEquals("Kategori oluşturuldu", msg);
        assertEquals("Kategori adı zaten mevcut.", msg);

    }

}


/*
* 1) Metni tam eşit olanı bul
By.xpath("//li[text()='Temizlik İşleri']")
*
*2) İçinde geçen (senin örnek)
By.xpath("//li[contains(text(),'Temizlik')]")
*
* 4) Class ile bul
By.xpath("//li[contains(@class,'MuiMenuItem-root')]")


@class = class attribute.

5) Bir elementin içindeki elementi bul

Mesela “Temizlik İşleri” yazan bir satırın içindeki buton:

By.xpath("//li[contains(.,'Temizlik İşleri')]//button")
*
*
* Boşluk problemi (normalize-space)

UI bazen başta/sonda boşluk koyar.

By.xpath("//li[normalize-space(.)='Temizlik İşleri']")
*
* */
