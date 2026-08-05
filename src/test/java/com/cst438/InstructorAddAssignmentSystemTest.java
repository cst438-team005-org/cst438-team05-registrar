package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * System test for the instructor adding an assignment and grading it.
 * <p>
 * This test uses Selenium WebDriver to automate browser interactions.
 */
public class InstructorAddAssignmentSystemTest {

  private static final String FRONTEND_URL = "http://localhost:5173";
  private static final String INSTRUCTOR_EMAIL = "ted@csumb.edu";
  private static final String INSTRUCTOR_PASSWORD = "ted2025";
  private static final String COURSE_ID = "cst599";
  private static final String YEAR = "2025";
  private static final String SEMESTER = "Fall";
  private static final int DELAY = 3000;

  private WebDriver driver;
  private WebDriverWait wait;

  @BeforeEach
  void setUp() {
    driver = new ChromeDriver();
    wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    driver.manage().window().maximize();
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  void instructorAddsAndGradesAssignment() {
    String assignmentTitle = "assignment"
        + ThreadLocalRandom.current().nextInt(100000, 1000000);

    login();
    selectTerm();
    openAssignments();
    addAssignment(assignmentTitle);
    verifyAssignmentAppears(assignmentTitle);

    openGrades(assignmentTitle);
    enterScore("sama@csumb.edu", "60");
    enterScore("samb@csumb.edu", "88");
    enterScore("samc@csumb.edu", "98");
    pause();
    saveGrades();
    closeDialog();

    openGrades(assignmentTitle);
    verifyScore("sama@csumb.edu", "60");
    verifyScore("samb@csumb.edu", "88");
    verifyScore("samc@csumb.edu", "98");
    pause();
    closeDialog();
  }

  private void login() {
    driver.get(FRONTEND_URL);

    WebElement email = wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    WebElement password = driver.findElement(By.id("password"));

    email.sendKeys(INSTRUCTOR_EMAIL);
    password.sendKeys(INSTRUCTOR_PASSWORD);
    driver.findElement(By.id("loginButton")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h1[normalize-space()='Instructor Home']")));
    pause();
  }

  private void selectTerm() {
    WebElement year = wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.id("year")));
    WebElement semester = driver.findElement(By.id("semester"));

    year.clear();
    year.sendKeys(YEAR);
    semester.clear();
    semester.sendKeys(SEMESTER);

    WebElement button = driver.findElement(By.id("selectTermButton"));
    assertEquals("Get Sections", button.getText().trim());
    button.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
        courseRow()));
    pause();
  }

  private void openAssignments() {
    WebElement course = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            courseRow()));

    course.findElement(
        By.xpath(".//a[normalize-space()='Assignments']")
    ).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//h3[contains(normalize-space(), 'Assignments')]")));
    pause();
  }

  private void addAssignment(String assignmentTitle) {
    wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//button[normalize-space()='Add Assignment']")
    )).click();

    WebElement titleInput = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.xpath(
                "//*[normalize-space()='Add Assignment']"
                    + "/following::input[1]"
            )
        )
    );

    titleInput.sendKeys(assignmentTitle);

    // Move from the assignment title field to the due date field.
    titleInput.sendKeys(Keys.TAB);

    WebElement dateInput = driver.switchTo().activeElement();
    dateInput.sendKeys("10152025");
    pause();

    wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath(
            "//*[normalize-space()='Add Assignment']"
                + "/following::button[normalize-space()='Save'][1]"
        )
    )).click();

    wait.until(ExpectedConditions.textToBePresentInElementLocated(
        By.tagName("body"),
        "assignment added id="
    ));
    pause();

    wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath(
            "//*[normalize-space()='Add Assignment']"
                + "/following::button[normalize-space()='Close'][1]"
        )
    )).click();
  }

  private void verifyAssignmentAppears(String assignmentTitle) {
    WebElement row = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            assignmentRow(assignmentTitle)));

    assertTrue(
        row.isDisplayed(),
        "The new assignment should appear in the table."
    );
    pause();
  }

  private void openGrades(String assignmentTitle) {
    WebElement row = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            assignmentRow(assignmentTitle)));

    row.findElement(
        By.xpath(".//button[contains(normalize-space(), 'Grade')]")
    ).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
        studentRow()));
    pause();
  }

  private void enterScore(String studentEmail, String score) {
    WebElement scoreInput = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            scoreInput(studentEmail)));

    scoreInput.click();
    scoreInput.sendKeys(Keys.CONTROL, "a");
    scoreInput.sendKeys(score);
  }

  private void saveGrades() {
    By saveButtonLocator = By.xpath(
        "//*[contains(normalize-space(), 'Grades')]"
            + "/following::button[normalize-space()='Save'][1]"
    );

    WebElement saveButton = wait.until(driver -> {
      for (WebElement button :
          driver.findElements(saveButtonLocator)) {

        if (button.isDisplayed() && button.isEnabled()) {
          return button;
        }
      }
      return null;
    });

    saveButton.click();
    pause();
  }

  private void verifyScore(
      String studentEmail,
      String expectedScore
  ) {
    WebElement scoreInput = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            scoreInput(studentEmail)));

    assertEquals(
        expectedScore,
        scoreInput.getAttribute("value"),
        "Incorrect saved score for " + studentEmail
    );
  }

  private void closeDialog() {
    WebElement closeButton = wait.until(driver -> {
      List<WebElement> buttons = driver.findElements(
          By.xpath("//button[normalize-space()='Close']")
      );

      for (WebElement button : buttons) {
        if (button.isDisplayed() && button.isEnabled()) {
          return button;
        }
      }
      return null;
    });

    closeButton.click();

    wait.until(driver -> {
      List<WebElement> buttons = driver.findElements(
          By.xpath("//button[normalize-space()='Close']")
      );

      for (WebElement button : buttons) {
        if (button.isDisplayed()) {
          return false;
        }
      }
      return true;
    });
  }

  private void pause() {
    try {
      Thread.sleep(DELAY);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private By courseRow() {
    return By.xpath(
        "//tbody/tr[td[translate(normalize-space(), "
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
            + "'abcdefghijklmnopqrstuvwxyz')='"
            + InstructorAddAssignmentSystemTest.COURSE_ID.toLowerCase()
            + "']]"
    );
  }

  private By assignmentRow(String assignmentTitle) {
    return By.xpath(
        "//tbody/tr[td[normalize-space()='"
            + assignmentTitle
            + "']]"
    );
  }

  private By studentRow() {
    return By.xpath(
        "//tbody/tr[td[contains("
            + "translate(normalize-space(), "
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', "
            + "'abcdefghijklmnopqrstuvwxyz'), '"
            + "sama".toLowerCase()
            + "')]]"
    );
  }

  private By scoreInput(String studentEmail) {
    return By.xpath(
        "//tr[td[normalize-space()='"
            + studentEmail
            + "']]//input"
    );
  }
}