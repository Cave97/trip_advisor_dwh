package main.java;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Review {
    private final String memberSince;
    private final String userInfo;
    private final String userLevel;
    private final String contributions;
    private final String citiesVisited;
    private final String helpfulVotes;
    private final String photos;
    private final String atmosphere;
    private final String service;
    private final String food;
    private final String text;
    private final String date;
    private final String evaluation;
    private final String restCode;



    public Review(String restCode, String memberSince, String userInfo, String userLevel, String contributions, String citiesVisited, String helpfulVotes, String photos, String atmosphere, String service, String food, String date, String evaluation, String text) throws ParseException {
        this.restCode = restCode;
        this.memberSince = memberSince;
        this.userInfo = userInfo;
        this.userLevel = userLevel;
        this.contributions = contributions;
        this.citiesVisited = citiesVisited;
        this.helpfulVotes = helpfulVotes;
        this.photos = photos;
        this.atmosphere = atmosphere;
        this.service = service;
        this.food = food;
        if(date.contains(",")){
            SimpleDateFormat tripDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
            Date rDate = tripDate.parse(date);
            SimpleDateFormat fileDate = new SimpleDateFormat("dd-MM-yyyy");
            this.date = fileDate.format(rDate);
        }else
            this.date = date;
        this.evaluation = evaluation;
        String escapedData = text.replaceAll("\n", " ");
        escapedData = escapedData.replaceAll(";", " ");
        //byte[] bytes = escapedData.getBytes(StandardCharsets.UTF_8);
        this.text = escapedData;//new String(bytes, StandardCharsets.UTF_8);
    }


    public static boolean isValid(String userInfo){
        String[] temp = userInfo.split(" ");
        if(temp.length >= 5 && temp[0].matches(".*[0-9].*") && userInfo.contains(","))
            return true;
        else return false;
    }

    public static void writeToFile(List<String[]> reviews){
        try{
            String OS = System.getProperty("os.name").toLowerCase();
            File csvWrite;
            if(OS.contains("windows"))
                csvWrite = new File(".\\output\\output.csv");
            else
                csvWrite = new File("./output/output.csv");

            FileWriter fileWriter = new FileWriter(csvWrite, true);
            CSVWriter writer = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR);
            writer.writeAll(reviews);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String[] createsArray(){
        return new String[] {restCode, date, evaluation, memberSince, userInfo, userLevel, contributions, citiesVisited, helpfulVotes, photos, atmosphere, service, food, text};
    }

    @Override
    public String toString() {
        return "Review{" +
                "memberSince='" + memberSince + '\'' +
                ", userInfo='" + userInfo + '\'' +
                ", userLevel='" + userLevel + '\'' +
                ", contributions='" + contributions + '\'' +
                ", citiesVisited='" + citiesVisited + '\'' +
                ", helpfulVotes='" + helpfulVotes + '\'' +
                ", photos='" + photos + '\'' +
                ", atmosphere='" + atmosphere + '\'' +
                ", service='" + service + '\'' +
                ", food='" + food + '\'' +
                ", text='" + text + '\'' +
                ", date='" + date + '\'' +
                ", evaluation='" + evaluation + '\'' +
                ", restCode='" + restCode + '\'' +
                '}';
    }
}
