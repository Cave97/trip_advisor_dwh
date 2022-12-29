package main.java;

import com.opencsv.*;
import net.bytebuddy.implementation.bytecode.Throw;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.tracing.TracedHttpClient;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    static WebDriver driver;
    public static void main(String[] args) throws InterruptedException{
        int reviewLimit = 4;
        boolean documentFinished = false;
        boolean thereIsSomeTrouble = false;
        Scanner sc = new Scanner((System.in));
        System.out.print("How many restaurants do you want to scan? (1 restaurant per minute): ");
        int restToScan = sc.nextInt();
        File csvRead;
        File index;


        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.contains("windows")){
            System.setProperty("webdriver.chrome.driver",  ".\\webdrivers\\chromedriver_win32\\chromedriver.exe");
            csvRead = new File(".\\read\\tripadvisor_italian_restaurants_for_reviews_retrieval.csv");
            index = new File(".\\read\\index.txt");
        }
        else if (OS.contains("mac")){
            System.setProperty("webdriver.chrome.driver",  "./webdrivers/chromedriver_mac64/chromedriver");
            csvRead = new File("./read/tripadvisor_italian_restaurants_for_reviews_retrieval.csv");
            index = new File("./read/index.txt");
        }
        else {
            System.setProperty("webdriver.chrome.driver",  "/webdrivers/chromedriver_linux64/chromedriver");
            csvRead = new File("/read/tripadvisor_italian_restaurants_for_reviews_retrieval.csv");
            index = new File("/read/index.txt");
        }




        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1920,1080");
        //options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--headless");
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions");
        driver = new ChromeDriver(options);


        // wait
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        prepareForExecution(driver, wait, true);

        String[] line = new String[4];
        int restaurants = 0;
        boolean first = true;
        String restCode = null;
        String restName = "";
        String restCity = "";
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvRead)).withCSVParser(csvParser).build()) {
            //Skip header
            if(!index.exists()){
                reader.readNext();
                File csvWrite;
                if(OS.contains("windows"))
                    csvWrite = new File(".\\output\\output.csv");
                else if (OS.contains("mac"))
                    csvWrite = new File("./output/output.csv");
                else
                    csvWrite = new File("/output/output.csv");

                FileWriter fileWriter = new FileWriter(csvWrite, true);
                CSVWriter writer = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_ESCAPE_CHARACTER);
                String[] data = new String[] {"restCode", "date", "evaluation", "memberSince", "userId", "userInfo", "userLevel", "contributions", "citiesVisited", "helpfulVotes", "photos", "atmosphere", "service", "food", "title", "text"};
                writer.writeNext(data);
                writer.close();
            }

            do{
                // La seconda condizione viene valutata solo se non c'è stato alcun crash al giro precedente
                if (thereIsSomeTrouble || (line = reader.readNext()) != null) {
                    if(!thereIsSomeTrouble){
                        restaurants++;
                        if(index.exists() && first){
                            FileReader fr = new FileReader(index);
                            BufferedReader br = new BufferedReader(fr);
                            String ind = br.readLine();
                            // System.out.println("Sono nell'if");
                            do{
                                line = reader.readNext();
                                //   System.out.println("Sono nel ciclo");
                            }while (!line[0].equals(ind));
                            line = reader.readNext();
                        }
                        first = false;
                        restCode = line[0];
                        restName = line[1];
                        restCity = line[2];

                        System.out.println("Initialising analysis " + restaurants + "\\" + restToScan);
                    }
                    try {


                        Thread.sleep(1000);
                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")));
                        driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")).sendKeys(restName + " " + restCity);

                        //driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")).sendKeys("Ma'Kaura, Santa Croce Camerina, Sicily");


                        Thread.sleep(3000);

                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']")));
                        List<WebElement> results;
                        results = driver.findElements(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']"));
                        if (results.get(0).findElement(By.xpath("//div[@class='biGQs _P fiohW fOtGX']")).getText().contains(restName)) {
                            System.out.println("Analyzing " + restName + " in " + restCity + " with code " + restCode);
                            results.get(0).click();
                            Thread.sleep(3000);
                            if (driver.getCurrentUrl().contains("Restaurant_Review")) {
                                if (existsElement("//div[@data-tracker='All languages']")) {
                                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-tracker='All languages']")));
                                    driver.findElement(By.xpath("//div[@data-tracker='All languages']")).click();

                                    Thread.sleep(3000);
                                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//select[@id='sort-by']")));
                                    driver.findElement(By.xpath("//select[@id='sort-by']")).click();

                                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//option[@value='ml_sorted']")));
                                    driver.findElement(By.xpath("//option[@value='ml_sorted']")).click();

                                    boolean stop = false;
                                    int count = 0;
                                    List<String[]> reviewsList = new ArrayList<>();
                                    do {
                                        count++;
                                        Thread.sleep(3000);

                                        String xpathReview = "(//div[@class='ui_column is-9'])";
                                        if (existsElement(xpathReview + "[1]//span[text()='More']")) {
                                            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathReview + "[1]//span[text()='More']")));
                                            driver.findElement(By.xpath(xpathReview + "[1]//span[text()='More']")).click();
                                            Thread.sleep(4000);
                                        } else if (existsElement(xpathReview + "[2]//span[text()='More']")) {
                                            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathReview + "[2]//span[text()='More']")));
                                            driver.findElement(By.xpath(xpathReview + "[2]//span[text()='More']")).click();
                                            Thread.sleep(4000);
                                        }

                                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//div[@class='member_info']")));
                                        List<WebElement> users = driver.findElements(By.xpath("//div[@class='member_info']"));
                                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//div[@class='ui_column is-9']")));
                                        List<WebElement> reviews = driver.findElements(By.xpath("//div[@class='ui_column is-9']"));

                                        //System.out.println(reviews.size());


                                        for (int i = 0; i < users.size(); i++) {
                                            String memberSince = "";
                                            String userInfo = "";
                                            String evaluation = "";
                                            String date;
                                            String userLevel = "";
                                            String contributions = "";
                                            String citiesVisited = "";
                                            String helpfulVotes = "";
                                            String photos = "";
                                            String atmosphere = "";
                                            String service = "";
                                            String food = "";
                                            String text = "";
                                            String userId;
                                            String title = "";

                                            wait.until(ExpectedConditions.visibilityOf(users.get(i).findElement(By.className("avatar"))));
                                            WebElement tempId = users.get(i).findElement(By.className("avatar"));
                                            String temp = tempId.getAttribute("class");
                                            if(temp.length() > 15)
                                                userId = temp.substring(15);
                                            else
                                                userId = "";
                                            try {
                                                users.get(i).click();
                                            } catch (ElementClickInterceptedException e) {
                                                System.out.println("It has not been possible to click on user, retrying in 0,5 s...");
                                                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//div[@class='ui_close_x'])[2]")));
                                                driver.findElement(By.xpath("(//div[@class='ui_close_x'])[2]")).click();
                                                Thread.sleep(500);
                                                users.get(i).click();
                                            }

                                            try {
                                                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='memberOverlayRedesign g10n']")));

                                                try {
                                                    if (existsElement("//div[@class='badgeinfo']"))
                                                        userLevel = driver.findElement(By.xpath("//div[@class='badgeinfo']/span")).getText();

                                                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//ul[@class='memberdescriptionReviewEnhancements']/li")));
                                                    List<WebElement> userElements = driver.findElements(By.xpath("//ul[@class='memberdescriptionReviewEnhancements']/li"));
                                                    memberSince = userElements.get(0).getText().split(" ")[3];

                                                    if (userElements.size() == 1)
                                                        userInfo = "";
                                                    else
                                                        userInfo = userElements.get(1).getText();

                                                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//ul[@class='countsReviewEnhancements']/li")));
                                                    List<WebElement> userCounters = driver.findElements(By.xpath("//ul[@class='countsReviewEnhancements']/li"));
                                                    for (WebElement userCounter : userCounters) {
                                                        String tempS = userCounter.getText();
                                                        if (tempS.contains("Contribution"))
                                                            contributions = tempS.substring(0, tempS.indexOf(" "));
                                                        else if (tempS.contains("visited"))
                                                            citiesVisited = tempS.substring(0, tempS.indexOf(" "));
                                                        else if (tempS.contains("Helpful"))
                                                            helpfulVotes = tempS.substring(0, tempS.indexOf(" "));
                                                        else if (tempS.contains("Photo"))
                                                            photos = tempS.substring(0, tempS.indexOf(" "));
                                                    }
                                                } catch (Exception e) {
                                                    System.out.println("It has not been possible to get user data");
                                                }

                                                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//div[@class='ui_close_x'])[2]")));
                                                driver.findElement(By.xpath("(//div[@class='ui_close_x'])[2]")).click();
                                            } catch (TimeoutException e) {
                                                System.out.println("memberOverlayRedesign g10n not opened. Skipping related data retrieval.");
                                            }

                                            wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("ui_bubble_rating"))));
                                            WebElement ev = reviews.get(i).findElement(By.className("ui_bubble_rating"));
                                            evaluation = setBubbleEvaluation(ev);

                                            //System.out.println(evaluation);
                                            wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("ratingDate"))));
                                            date = reviews.get(i).findElement(By.className("ratingDate")).getText();
                                            //System.out.println(date);

                                            try {
                                                wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("noQuotes"))));
                                                title = reviews.get(i).findElement(By.className("noQuotes")).getText();
                                            } catch (TimeoutException e) {
                                                System.out.println("Review's title not present. Leaving blank...");
                                            }

                                            wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("partial_entry"))));
                                            text = reviews.get(i).findElement(By.className("partial_entry")).getText();
                                            //System.out.println(text);

                                            if (areThereMoreInfo(reviews.get(i))) {
                                                wait.until(ExpectedConditions.visibilityOfAllElements(reviews.get(i).findElements(By.className("recommend-answer"))));
                                                List<WebElement> firstColumn = reviews.get(i).findElements(By.className("recommend-answer"));
                                                for (WebElement wl : firstColumn) {
                                                    String cat = wl.findElement(By.className("recommend-description")).getText();
                                                    WebElement eval = wl.findElement(By.className("ui_bubble_rating"));
                                                    if (cat.equals("Atmosphere"))
                                                        atmosphere = setBubbleEvaluation(eval);
                                                    else if (cat.equals("Service"))
                                                        service = setBubbleEvaluation(eval);
                                                    else if (cat.equals("Food"))
                                                        food = setBubbleEvaluation(eval);
                                                }
                                        /*
                                        wait.until(ExpectedConditions.visibilityOfAllElements(reviews.get(i).findElements(By.xpath("//ul[@class='recommend-column']/li"))));
                                        List<WebElement> secondColumn = reviews.get(i).findElements(By.xpath("//ul[@class='recommend-column']/li"));
                                        System.out.println(secondColumn.size());
                                        for(WebElement wl : secondColumn){
                                            String cat = wl.findElement(By.xpath("//div[@class='recommend-description']")).getText();
                                           // System.out.println(cat);
                                            WebElement eval = wl.findElement(By.className("ui_bubble_rating"));
                                            if(cat.equals("Service"))
                                                service = setBubbleEvaluation(eval);
                                            else if(cat.equals("Food"))
                                                food = setBubbleEvaluation(eval);
                                        }

                                         */
                                            }

                                            Review r = new Review(restCode, date, evaluation, memberSince, userId, userInfo, userLevel, contributions, citiesVisited, helpfulVotes, photos, atmosphere, service, food, title, text);
                                            reviewsList.add(r.createsArray());

                                        }


                                        if (existsElement("//a[@class='nav next ui_button primary disabled']"))
                                            stop = true;
                                        else {
                                            if (existsElement("//a[@class='nav next ui_button primary']"))
                                                driver.findElement(By.xpath("//a[@class='nav next ui_button primary']")).click();
                                            else
                                                stop = true;
                                        }

                                    } while (!stop && count < reviewLimit);
                                    Review.writeToFile(reviewsList);
                                    System.out.println("Restaurant n° " + restaurants + " analyzed. " + reviewsList.size() + " valid reviews saved");

                                } else {
                                    System.out.println(restName + " has 0 reviews! Starting another restaurant...");
                                }
                            } else {
                                System.out.println("Page loaded is not a restaurant. Restarting tool and skipping restaurant...");
                                prepareForExecution(driver, wait, false);
                            }

                            try {
                                FileWriter fw = new FileWriter(index);
                                fw.write(restCode);
                                System.out.println(restCode);
                                fw.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        } else {
                            System.out.println(restName + " in " + restCity + " with code " + restCode + " NOT FOUND!");
                            if (OS.contains("windows"))
                                driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.CONTROL + "a");
                            else if (OS.contains("mac"))
                                driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.COMMAND + "a");
                            else
                                driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.CONTROL + "a");
                            driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.DELETE);
                        }
                        thereIsSomeTrouble = false;
                    }catch (Exception e){
                        if(!thereIsSomeTrouble){
                            System.out.println("**********************There is some troubles!!!11!!!!1!!1!!***************************");
                            thereIsSomeTrouble = true;
                            driver.navigate().refresh();
                        }else
                            throw e;
                    }
                }
                else
                    documentFinished = true;
            } while (restaurants < restToScan && !documentFinished);

        } catch (IOException | ParseException e){//| ParseException e) {
            throw new RuntimeException(e);
        }
        driver.quit();
    }

    private static void prepareForExecution(WebDriver driver, WebDriverWait wait, boolean firstLoad) throws InterruptedException {
        driver.get("https://www.tripadvisor.com/");

        System.out.println("Starting execution...");

        if (firstLoad) {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
            driver.findElement(By.id("onetrust-accept-btn-handler")).click();
        }

        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//input[@placeholder='Where to?']")));
        driver.findElement(By.xpath("//input[@placeholder='Where to?']")).sendKeys("Le 147");
        Thread.sleep(3000);
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']")));
        List<WebElement> temp=driver.findElements(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']"));
        temp.get(0).click();
    }

    private static boolean existsElement(String id) {
        try {
            driver.findElement(By.xpath(id));
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    private static String setBubbleEvaluation(WebElement ev){
        String toEval = "";
        if(ev.getAttribute("class").contains("bubble_10"))
            toEval = "1";
        else if(ev.getAttribute("class").contains("bubble_20"))
            toEval = "2";
        else if(ev.getAttribute("class").contains("bubble_30"))
            toEval = "3";
        else if(ev.getAttribute("class").contains("bubble_40"))
            toEval = "4";
        else if(ev.getAttribute("class").contains("bubble_50"))
            toEval = "5";
        return toEval;
    }

    private static boolean areThereMoreInfo(WebElement wl){
        try {
            wl.findElement(By.className("recommend")).click();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
