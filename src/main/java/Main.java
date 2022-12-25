package main.java;

import com.opencsv.*;
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

        else{
            System.setProperty("webdriver.chrome.driver",  "./webdrivers/chromedriver_mac64/chromedriver");
            csvRead = new File("./read/tripadvisor_italian_restaurants_for_reviews_retrieval.csv");
            index = new File("./read/index.txt");
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

        driver.get("https://www.tripadvisor.com/");

        System.out.println("Starting execution...");


        wait.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
        driver.findElement(By.id("onetrust-accept-btn-handler")).click();

        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//input[@placeholder='Where to?']")));
        driver.findElement(By.xpath("//input[@placeholder='Where to?']")).sendKeys("Le 147");
        Thread.sleep(4000);
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']")));
        List<WebElement> temp=driver.findElements(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']"));
        temp.get(0).click();

        String[] line;
        int restaurants = 0;
        boolean first = true;
        String restCode = null;
        String restName;
        String restCity;
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvRead)).withCSVParser(csvParser).build()) {
            //Skip header
            if(!index.exists()){
                reader.readNext();
                File csvWrite;
                if(OS.contains("windows"))
                    csvWrite = new File(".\\output\\output.csv");
                else
                    csvWrite = new File("./output/output.csv");

                FileWriter fileWriter = new FileWriter(csvWrite, true);
                CSVWriter writer = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_ESCAPE_CHARACTER);
                String[] data = new String[] {"restCode", "date", "evaluation", "memberSince", "userInfo", "userLevel", "contributions", "citiesVisited", "helpfulVotes", "photos", "atmosphere", "service", "food", "text"};
                writer.writeNext(data);
                writer.close();
            }

            do{
                restaurants++;
                if ((line = reader.readNext()) != null) {
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


                    Thread.sleep(1000);
                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")));
                    //driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")).sendKeys(restName + " " + restCity);

                    driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0']")).sendKeys("Ma'Kaura, Santa Croce Camerina, Sicily");


                    Thread.sleep(4000);

                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']")));
                    List<WebElement> results;
                    results = driver.findElements(By.xpath("//a[@class='GzJDZ w z _S _F Wc Wh Q B- _G']"));
                    if(results.get(0).findElement(By.xpath("//div[@class='biGQs _P fiohW fOtGX']")).getText().contains("Ma")){//restName)){
                        System.out.println("Analyzing " + restName + " in " + restCity + " with code " + restCode);
                        results.get(0).click();
                        Thread.sleep(3000);
                        if(existsElement("//div[@data-tracker='All languages']")){
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
                                if(existsElement(xpathReview + "[1]//span[text()='More']")){
                                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathReview + "[1]//span[text()='More']")));
                                    driver.findElement(By.xpath(xpathReview + "[1]//span[text()='More']")).click();
                                    Thread.sleep(3000);
                                }
                                else if(existsElement(xpathReview + "[2]//span[text()='More']")){
                                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathReview + "[2]//span[text()='More']")));
                                    driver.findElement(By.xpath(xpathReview + "[2]//span[text()='More']")).click();
                                    Thread.sleep(3000);
                                }

                                wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//div[@class='member_info']")));
                                List<WebElement> users = driver.findElements(By.xpath("//div[@class='member_info']"));
                                wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//div[@class='ui_column is-9']")));
                                List<WebElement> reviews = driver.findElements(By.xpath("//div[@class='ui_column is-9']"));

                                //System.out.println(reviews.size());


                                for (int i = 0; i < users.size(); i++) {
                                    String memberSince = "";
                                    String userInfo;
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
                                    String text="";

                                    users.get(i).click();

                                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='memberOverlayRedesign g10n']")));

                                    if(existsElement("//div[@class='badgeinfo']"))
                                        userLevel = driver.findElement(By.xpath("//div[@class='badgeinfo']/span")).getText();

                                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//ul[@class='memberdescriptionReviewEnhancements']/li")));
                                    List<WebElement> userElements = driver.findElements(By.xpath("//ul[@class='memberdescriptionReviewEnhancements']/li"));

                                    memberSince = userElements.get(0).getText().split(" ")[3];

                                    if(userElements.size() == 1)
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

                                    wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//div[@class='ui_close_x'])[2]")));
                                    driver.findElement(By.xpath("(//div[@class='ui_close_x'])[2]")).click();



                                    wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("ui_bubble_rating"))));
                                    WebElement ev = reviews.get(i).findElement(By.className("ui_bubble_rating"));
                                    evaluation = setBubbleEvaluation(ev);

                                    //System.out.println(evaluation);
                                    wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("ratingDate"))));
                                    String tempDate = reviews.get(i).findElement(By.className("ratingDate")).getText();
                                    date = tempDate.substring(9);
                                    //System.out.println(date);

                                    wait.until(ExpectedConditions.visibilityOf(reviews.get(i).findElement(By.className("partial_entry"))));
                                    text = reviews.get(i).findElement(By.className("partial_entry")).getText();
                                    //System.out.println(text);

                                    if(areThereMoreInfo(reviews.get(i))){
                                        wait.until(ExpectedConditions.visibilityOfAllElements(reviews.get(i).findElements(By.className("recommend-answer"))));
                                        List<WebElement> firstColumn = reviews.get(i).findElements(By.className("recommend-answer"));
                                        for(WebElement wl : firstColumn){
                                            String cat = wl.findElement(By.className("recommend-description")).getText();
                                            WebElement eval = wl.findElement(By.className("ui_bubble_rating"));
                                            if(cat.equals("Atmosphere"))
                                                atmosphere = setBubbleEvaluation(eval);
                                            else if(cat.equals("Service"))
                                                service = setBubbleEvaluation(eval);
                                            else if(cat.equals("Food"))
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

                                    Review r = new Review(restCode, memberSince, userInfo, userLevel, contributions, citiesVisited, helpfulVotes, photos, atmosphere, service, food, date, evaluation, text);
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


                        }
                        else{
                            System.out.println(restName + " has 0 reviews! Starting another restaurant...");
                        }

                        try {
                            FileWriter fw = new FileWriter(index);
                            fw.write(restCode);
                            System.out.println(restCode);
                            fw.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    else{
                        System.out.println(restName + " in " + restCity + " with code " + restCode + " NOT FOUND!");
                        if(OS.contains("windows"))
                            driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.CONTROL + "a");
                        else
                            driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.COMMAND + "a");
                        driver.findElement(By.xpath("//input[@class='qjfqs _G B- z _J Cj R0 UXKdo']")).sendKeys(Keys.DELETE);
                    }
                }
                else
                    documentFinished = true;
            } while (restaurants < restToScan && !documentFinished);

        } catch (IOException e){//| ParseException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        driver.quit();
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
