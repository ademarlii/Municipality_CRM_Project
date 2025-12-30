package com.ademarli.municipality_service.integration.e2e.ui.admin;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminLoginAndCreateDepartment {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String UI_BASE = System.getProperty("ui.baseUrl", "http://localhost:5173");

    // login
    private static final String TID_EMAIL = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASS = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT = "[data-testid='auth-login-submit']";

    // nav
    private static final String NAV_DEPTS = "[data-testid='nav-Departmanlar']";

    // departments page
    private static final String BTN_NEW_DEPT = "[data-testid='admin-dept-new']";
    private static final String INP_NAME = "#department-name-input";
    private static final String SW_ACTIVE = ".department-active-checkbox";
    private static final String BTN_SAVE = ".department-submit-button";

    @BeforeAll
    void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    private void loginAsAdmin() {
        driver.get(UI_BASE + "/auth/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TID_EMAIL)))
                .sendKeys("admin@local.com");
        driver.findElement(By.cssSelector(TID_PASS)).sendKeys("Admin123!");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(TID_SUBMIT))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(NAV_DEPTS)));
    }

    private void goDepartmentsPage() {
        pause();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(NAV_DEPTS))).click();
        pause();
        wait.until(ExpectedConditions.urlContains("/admin/departments"));
        pause();

    }

    private void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void admin_can_create_department() {
        loginAsAdmin();
        goDepartmentsPage();

        String deptName = "Bilgi işlem";

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_NEW_DEPT))).click();

        WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(INP_NAME)));
        nameInput.clear();
        nameInput.sendKeys(deptName);

        WebElement activeSwitch = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SW_ACTIVE)));
        if (!activeSwitch.isSelected()) activeSwitch.click();

        // save
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_SAVE))).click();

        // dialog kapandı mı? (MUI backdrop kaybolsun)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".MuiDialog-root")));

        // ✅ ASSERT: yeni departman listede göründü mü?
        WebElement created = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[normalize-space(text())='" + deptName + "']")
        ));
        assertTrue(created.isDisplayed());
    }

    @Test
    void admin_toogle_status_departments() {
        loginAsAdmin();
        goDepartmentsPage();

        //data-testid={`dept-card-${d.id}`}

        List<WebElement> departmentCards = wait.until(driver ->
                driver.findElements(By.cssSelector("[data-testid^='dept-card-']"
                )));

        assertFalse(departmentCards.isEmpty(), "Hiç departman bulunamadı");

        String passiveId = null;
        for (WebElement deptCard : departmentCards) {
            //data-testid={`dept-status-${d.id}`}
            WebElement statusisNoActive = deptCard.findElement(
                    By.cssSelector("[data-testid^='dept-status-']")
            );

            String status = statusisNoActive.getText().trim();
            if ("Pasif".equalsIgnoreCase(status)) {
                // card'ın testid'sinden id çek
                String tid = deptCard.getAttribute("data-testid"); // dept-card-12
                passiveId = tid.replace("dept-card-", "");
                break;
            }
        }
        Assertions.assertNotNull(passiveId, "Pasif departman bulunamadı! (Hepsi aktif olabilir)");

        //data-testid={`dept-edit-${d.id}`}

        WebElement editButtonById = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-edit-" + passiveId + "']")
        ));
        editButtonById.click();

        WebElement activeSwitch = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(SW_ACTIVE
                )));

        activeSwitch.click();

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_SAVE))).click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".MuiDialog-root")));

        WebElement updatedDeptCard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-card-" + passiveId + "']")
        ));
        WebElement statusisActive = updatedDeptCard.findElement(
                By.cssSelector("[data-testid='dept-status-" + passiveId + "']")
        );
        String updatedStatus = statusisActive.getText().trim();
        Assertions.assertEquals("Aktif", updatedStatus, "Departman aktif yapılamadı!");

    }


    @Test
    void admin_make_passife_departments_departmentName_knowlage() {
        //Bu çalışmadan önce Bilgi işlem departmanı pasif olmalı

        loginAsAdmin();
        goDepartmentsPage();

        String targetDeptName = "Bilgi işlem";
        //Önce kartları bul
        List<WebElement> cards = wait.until(d ->
                d.findElements(By.cssSelector("[data-testid^='dept-card-']")));

        Assertions.assertFalse(cards.isEmpty(), "Hiç departman bulunamadı!");
        String targetId = null;
        for (WebElement card : cards) {
//data-testid={`dept-name-${d.id}
            WebElement findTargetCard = card.findElement(By.cssSelector("[data-testid^='dept-name-']"));
            String name = findTargetCard.getText().trim();
            if (name.equalsIgnoreCase(targetDeptName)) {
                //data-testid={`dept-card-${d.id}`}
                String tid = card.getAttribute("data-testid");
                targetId = tid.replace("dept-card-", "");
                break;

            }
        }
        Assertions.assertNotNull(targetId, targetDeptName + " departmanı bulunamadı!");
        //data-testid=`dept-edit-${d.id}`
        WebElement editButtonById = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-edit-" + targetId + "']")
        ));
        String before=editButtonById.getText().trim();
        editButtonById.click();


        WebElement activeSwitch = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(SW_ACTIVE
                )));

        if(before.equals("Aktif")){
            activeSwitch.click();
        }



        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_SAVE))).click();
        // dialog kapandı mı? (MUI backdrop kaybolsun)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".MuiDialog-root")));
        // ✅ ASSERT: departman pasif oldu mu?
        WebElement updatedDeptCard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-card-" + targetId + "']")
        ));
        WebElement statusisPassive = updatedDeptCard.findElement(
                By.cssSelector("[data-testid='dept-status-" + targetId + "']")
        );
        String updatedStatus = statusisPassive.getText().trim();
        Assertions.assertEquals("Pasif", updatedStatus, "Departman pasif yapılamadı!");


    }

    @Test
    void soft_delete_target_department(){
        loginAsAdmin();
        goDepartmentsPage();

        String targetDeptName = "Bilgi işlem";

        // Kartlar (görünür olana kadar bekle)
        List<WebElement> cards = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("[data-testid^='dept-card-']")
                )
        );

        Assertions.assertFalse(cards.isEmpty(), "Hiç departman bulunamadı!");

        String targetId = null;

        for (WebElement card : cards) {
            // Kart içindeki departman adı
            WebElement nameEl = card.findElement(By.cssSelector("[data-testid^='dept-name-']"));
            String name = nameEl.getText().trim();

            if (name.equalsIgnoreCase(targetDeptName)) {
                String tid = card.getAttribute("data-testid"); // dept-card-12
                targetId = tid.replace("dept-card-", "");      // 12
                break;
            }
        }

        Assertions.assertNotNull(targetId, targetDeptName + " departmanı bulunamadı!");

        WebElement deleteButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-testid='dept-delete-" + targetId + "']")
        ));
        deleteButton.click();

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        pause();
        alert.accept();

        // ✅ ASSERT: departman listeden silindi mi?

        WebElement element= wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-status-" + targetId + "']")
        ));
        pause();
       // wait.until(d -> element.getText().trim().equals("Pasif"));
pause();
        assertEquals("Pasif", element.getText().trim());

    }
}


/*
* [data-testid='dept-card-12']
data-testid tam olarak dept-card-12 olanı seç

✅ ^= (başlıyor)
css
Kodu kopyala
[data-testid^='dept-card-']
dept-card- ile başlayanları seç

✅ $= (bitiyor)
css
Kodu kopyala
[data-testid$='-12']
-12 ile bitenleri seç (dept-card-12, dept-status-12 gibi)

✅ *= (içeriyor)
css
Kodu kopyala
[data-testid*='card']

* */