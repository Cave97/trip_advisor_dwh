package main.java;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Review {
    private String age;
    private String gender;
    private String state;
    private String city;
    private String date;
    private String evaluation;

    private String text;

    private String restCode;

    private static File csvWrite;

    public Review(String date, String evaluation, String restCode, String userInfo, String text) throws ParseException {
        SimpleDateFormat tripDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date rDate = tripDate.parse(date);
        SimpleDateFormat fileDate = new SimpleDateFormat("dd-MM-yyyy");
        this.date = fileDate.format(rDate);
        this.evaluation = evaluation;
        this.restCode = restCode;

        String escapedData = text.replaceAll("\n", " ");
        if (text.contains(",") || text.contains("\"") || text.contains("'")) {
            text = text.replace("\"", "\"\"");
        }
        this.text = escapedData;
        // TODO: Handle userInfo

        //System.out.println(userInfo);
        this.age = userInfo.substring(0, userInfo.indexOf(' '));
        String temp = userInfo.substring(userInfo.indexOf(' ') + 1);
        this.gender = temp.substring(0, temp.indexOf(' '));
        temp = temp.substring(temp.indexOf(' ') + 1);
        this.city = temp.substring(5, temp.indexOf(','));
        temp = temp.substring(city.length() + 7);
        this.state = temp;
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
            if(OS.contains("windows"))
                csvWrite  = new File(".\\output\\output.csv");
            else
                csvWrite  = new File("./output/output.csv");

            FileWriter fileWriter = new FileWriter(csvWrite, true);
            CSVWriter writer = new CSVWriter(fileWriter, CSVWriter.DEFAULT_SEPARATOR);
            writer.writeAll(reviews);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] createsArray(){
        return new String[] {restCode, date, evaluation, age, gender, state, city, text};
    }

    @Override
    public String toString(){
        return "Text: " + text + ". Age: " + age + ". Gender: " + gender + ". State: " + state + ". City: " + city + ". Evaluation: " + evaluation + ". Date: " + date + ". Restaurant: " + restCode;
    }
}
