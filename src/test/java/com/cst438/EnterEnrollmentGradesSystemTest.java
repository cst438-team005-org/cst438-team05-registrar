package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Assignment 4 system test: an instructor enters final enrollment grades
 * for the students enrolled in a CST599 section and verifies the grades
 * are saved.
 *
 * Required services before running:
 * - Registrar: http://localhost:8080
 * - Gradebook: http://localhost:8081
 * - React/Vite frontend: http://localhost:5173
 * - RabbitMQ
 */
public class EnterEnrollmentGradesSystemTest {

    private static final String FRONTEND_URL = "http://localhost:5173";

    private static final String INSTRUCTOR_EMAIL = "ted@csumb.edu";
    private static final String INSTRUCTOR_PASSWORD = "ted2025";

    private static final String COURSE_ID = "cst599";
    private static final String YEAR = "2025";
    private static final String SEMESTER = "Fall";

    private static final String[][] STUDENT_GRADES = {
            {"sama@csumb.edu", "A"},
            {"samb@csumb.edu", "B"},
            {"samc@csumb.edu", "C"}
    };

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
    void instructorEntersEnrollmentGrades() {
        login(INSTRUCTOR_EMAIL, INSTRUCTOR_PASSWORD, "Instructor Home");

        selectTerm("Get Sections");
        openCourseEnrollments(COURSE_ID);

        for (String[] studentGrade : STUDENT_GRADES) {
            enterGrade(studentGrade[0], studentGrade[1]);
        }
        pause();

        saveGrades();

        for (String[] studentGrade : STUDENT_GRADES) {
            verifyGradeSaved(studentGrade[0], studentGrade[1]);
        }
    }

    private void login(String email, String password, String expectedHeading) {
        driver.get(FRONTEND_URL);

        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        WebElement passwordInput = driver.findElement(By.id("password"));

        emailInput.clear();
        emailInput.sendKeys(email);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[normalize-space()='" + expectedHeading + "']")));
        pause();
    }

    private void selectTerm(String expectedButtonText) {
        WebElement yearInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("year")));
        WebElement semesterInput = driver.findElement(By.id("semester"));

        yearInput.clear();
        yearInput.sendKeys(YEAR);
        semesterInput.clear();
        semesterInput.sendKeys(SEMESTER);

        WebElement button = driver.findElement(By.id("selectTermButton"));
        assertEquals(expectedButtonText, button.getText().trim());
        button.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(courseRow(COURSE_ID)));
        pause();
    }

    private void openCourseEnrollments(String courseId) {
        WebElement row = wait.until(
                ExpectedConditions.visibilityOfElementLocated(courseRow(courseId)));
        row.findElement(By.id("enrollmentsLink")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[contains(normalize-space(), 'Enrollments')]")));
        pause();
    }

    private void enterGrade(String studentEmail, String grade) {
        WebElement gradeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(studentGradeInput(studentEmail)));

        gradeInput.click();
        gradeInput.clear();
        gradeInput.sendKeys(grade);
    }

    private void saveGrades() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("saveGradesButton"))).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), "Grades saved successfully."));
        pause();
    }

    private void verifyGradeSaved(String studentEmail, String expectedGrade) {
        WebElement gradeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(studentGradeInput(studentEmail)));

        assertEquals(expectedGrade, gradeInput.getAttribute("value"),
                "Incorrect saved grade for " + studentEmail);
    }

    private void pause() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private By courseRow(String courseId) {
        return By.xpath(
                "//tbody/tr[td[translate(normalize-space(), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
                        + courseId.toLowerCase() + "']]"
        );
    }

    private By studentGradeInput(String studentEmail) {
        return By.xpath(
                "//tr[td[normalize-space()='" + studentEmail + "']]//input"
        );
    }
}
