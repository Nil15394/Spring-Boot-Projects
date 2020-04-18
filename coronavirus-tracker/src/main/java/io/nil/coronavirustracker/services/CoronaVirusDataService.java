package io.nil.coronavirustracker.services;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.nil.coronavirustracker.models.LocationStats;

@Service
public class CoronaVirusDataService {

    private static String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";

    private List<LocationStats> allStats = new ArrayList<>();

    public List<LocationStats> getAllStats() {
        return allStats;
    }
    
    private int totalReportedCasesInIndia = 0;

    public int getTotalReportedCasesInIndia() {
		return totalReportedCasesInIndia;
	}

	@PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    public void fetchVirusData() throws IOException, InterruptedException {
        List<LocationStats> newStats = new ArrayList<>();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(VIRUS_DATA_URL);
        CloseableHttpResponse response = httpClient.execute(request);
        //System.out.println(response.toString());
        HttpEntity entity = response.getEntity();
        StringReader csvBodyReader = new StringReader(EntityUtils.toString(entity));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);
        for (CSVRecord record : records) {
            LocationStats locationStat = new LocationStats();
            locationStat.setState(record.get("Province/State"));
            locationStat.setCountry(record.get("Country/Region"));
            int latestCases = Integer.parseInt(record.get(record.size() - 1));
            int prevDayCases = Integer.parseInt(record.get(record.size() - 2));
            locationStat.setLatestTotalCases(latestCases);
            locationStat.setDiffFromPrevDay(latestCases - prevDayCases);
            if(record.get("Country/Region").equalsIgnoreCase("India")) {
            	this.totalReportedCasesInIndia = latestCases;
            }
            //System.out.println(locationStat);
            newStats.add(locationStat);
        }
        this.allStats = newStats;
    }

}
