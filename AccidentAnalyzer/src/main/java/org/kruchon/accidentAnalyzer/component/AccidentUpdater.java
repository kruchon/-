package org.kruchon.accidentAnalyzer.component;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kruchon.accidentAnalyzer.component.impl.AccidentClustererImpl;
import org.kruchon.accidentAnalyzer.domain.Accident;
import org.kruchon.accidentAnalyzer.domain.AccidentCluster;
import org.kruchon.accidentAnalyzer.domain.ClusterReport;
import org.kruchon.accidentAnalyzer.service.AccidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AccidentUpdater {

    @Autowired
    private AccidentService accidentService;

    @Autowired
    private FileOperations fileOperations;
    private final static String resourcesPath = System.getProperty("user.dir").replace("bin","resources/");

    @Autowired
    private ClusterReport clusterReport;

    @Autowired
    private AccidentsHolder accidentsHolder;

    @Autowired
    private SummariesExecutor summariesExecutor;

    @PostConstruct
    public void createResourcesFolder(){
        //TODO Bad solution, it's better to map resources folder
        File resourcesFolder = new File(resourcesPath);
        if (!resourcesFolder.exists()) {
            resourcesFolder.mkdir();
        }
    }

    private JSONArray readAccidentsData() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(resourcesPath + "2015-crash.json"));
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray accidentsData = (JSONArray) jsonObject.get("items");

        obj = parser.parse(new FileReader(resourcesPath + "2016-crash.json"));
        jsonObject = (JSONObject) obj;
        accidentsData.addAll((JSONArray) jsonObject.get("items"));

        obj = parser.parse(new FileReader(resourcesPath + "2017-crash.json"));
        jsonObject = (JSONObject) obj;
        accidentsData.addAll((JSONArray) jsonObject.get("items"));
        return accidentsData;
    }

    private List<Accident> mapJsonArrayToObjects(JSONArray accidentsData) throws IOException {
        @SuppressWarnings("unchecked")
        List<Accident> result = new ArrayList<Accident>();
        for(Object accident : accidentsData) {
            result.add(accidentService.createFromJsonObject((JSONObject) accident));
        }
        return result;
    }

    private void mapAndSave(JSONArray accidentsData) throws IOException {
        List<Accident> accidents = mapJsonArrayToObjects(accidentsData);
        accidentService.deleteAll();
        accidentService.saveAll(accidents);
        AccidentsClusterer accidentsClusterer = new AccidentClustererImpl();
        List<AccidentCluster> clusters = accidentsClusterer.calculate(accidents);
        clusterReport.setClusters(clusters);
    }

    @Scheduled(fixedDelay = 86400000)
    public void updateAccidents() throws IOException, ParseException {
        //fileOperations.downloadUsingNIO("https://xn--80abhddbmm5bieahtk5n.xn--p1ai/opendata-storage/2015-crash.json.zip",resourcesPath+"first.zip");
        //fileOperations.unpack(resourcesPath+"first.zip",resourcesPath);

        JSONArray accidentsData = readAccidentsData();
//TODO develop OSMdataFiller
        //TODO (characteristics for accidents such as speed mode of road, road type, slope of the road..)
        //OSMAccidentsDataFiller.fill(accidentsData);

        //TODO implement full deletion in table Accidents
        //clearAccidentsTable();
        mapAndSave(accidentsData);
        summariesExecutor.executeAndSaveAllSummaries();
    }
}
