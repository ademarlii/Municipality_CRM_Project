package com.ademarli.municipality_service.integration.e2e.ui.admin;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class AdminLoginAndCreateDepartmentCategoryTest extends BaseUiE2ETest {

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    private static final String NAV_CAT = "[data-testid='nav-Kategoriler']";
    private static final String CREATE_CAT_BUTTON = "[data-testid='admin-category-new']";
    private static final String CATEGORY_NAME_INPUT = "[data-testid='admin-category-name']";

    private static final String SELECT_DEPARTMENT_DROPDOWN = ".admin-category-dropdown-department";
    private static final String SWITCH_BUTTON = "admin-category-status-switch";

    private static final String SAVE_CATEGORY_BUTTON = "[data-testid='admin-category-save']";

    private void loginAdmin() {
        driver.get(UI_BASE + "/auth/login");

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_EMAIL_OR_PHONE)
        ));
        emailInput.clear();
        emailInput.sendKeys("admin@local.com");

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_PASSWORD)
        ));
        passwordInput.clear();
        passwordInput.sendKeys("Admin123!");

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(TID_SUBMIT)
        ));
        submitButton.click();
    }

    private void pause() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    private void gotoAdminDepartmentCategoriesPage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(NAV_CAT))).click();
    }

    //varolan kategoriyi ekleme hatası
    @Test
    void succesAdminLogin_and_throwCreateCategory() {
        loginAdmin();
        gotoAdminDepartmentCategoriesPage();

        WebElement createCategoryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(CREATE_CAT_BUTTON)
        ));
        createCategoryButton.click();

        WebElement categoryNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(CATEGORY_NAME_INPUT)
        ));
        categoryNameInput.clear();
        categoryNameInput.sendKeys("Çöp Toplama");

        WebElement selectDepartmentDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(SELECT_DEPARTMENT_DROPDOWN)
        ));
       // pause();
        selectDepartmentDropdown.click();
      //  pause();

        WebElement selectDepartmentOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[contains(text(),'Temizlik İşleri')]")
        ));
      //  pause();
        selectDepartmentOption.click();

      //  pause();
        WebElement switchButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className(SWITCH_BUTTON)
        ));

        String status = switchButton.getText().trim();
        if (!status.equals("Aktif")) {
            log.info("Kategori varsayılan olarak aktif geliyor.");
        } else {
            switchButton.click();
        }

     //   pause();
        WebElement saveCategoryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(SAVE_CATEGORY_BUTTON)
        ));
        saveCategoryButton.click();
      //  pause();

        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='toast-error']")
        ));

        String msg = toast.getAttribute("data-message");
        assertEquals("Kategori adı zaten mevcut.", msg);
    }

    //başarılı yeni kaktegori ekleme
    @Test
    void succesAdminLogin_and_succesCreateCategory() {
        loginAdmin();
        gotoAdminDepartmentCategoriesPage();

        WebElement createCategoryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(CREATE_CAT_BUTTON)
        ));
        createCategoryButton.click();

        WebElement categoryNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(CATEGORY_NAME_INPUT)
        ));
        categoryNameInput.clear();
        UUID uuid = UUID.randomUUID();
        categoryNameInput.sendKeys("Yeni kategori" + uuid);

        WebElement selectDepartmentDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(SELECT_DEPARTMENT_DROPDOWN)
        ));
        pause();
        selectDepartmentDropdown.click();
        pause();

        WebElement selectDepartmentOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[contains(text(),'Temizlik İşleri')]")
        ));
        pause();
        selectDepartmentOption.click();

        pause();
        WebElement switchButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className(SWITCH_BUTTON)
        ));

        String status = switchButton.getText().trim();
        if (!status.equals("Aktif")) {
            log.info("Kategori varsayılan olarak aktif geliyor.");
        } else {
            switchButton.click();
        }

        pause();
        WebElement saveCategoryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(SAVE_CATEGORY_BUTTON)
        ));
        saveCategoryButton.click();
        pause();

        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='toast-success']")
        ));

        String msg = toast.getAttribute("data-message");
        assertEquals("Kategori oluşturuldu.", msg);
    }
}
