
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.MutableAttributeSet;
import org.json.JSONArray;
import org.json.JSONObject;

class ReadRunData
{
  static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
  static final String URL_FORMAT = "https://www.parkrun.org.uk/%s/parkrunner/%s/";
  static final String URL_EVENT_NAME = "dulwich";

  List<String> runners;
  Map<String, List<EventData>> runnersEvents;
  List<String> names;

  class EventData {
    public final String eventNumber;
    public final String time;

    EventData(String eventNumber, String time){
      this.eventNumber = eventNumber;
      this.time = time;
    }
  }

  
  class RunPageParserCallback extends HTMLEditorKit.ParserCallback {

	  static final String DIV1_MAIN = "main";
	  static final String DIV2_PRIMARY = "primary";
	  static final String DIV3_CONTENT = "content";

    private String parsedName;
    private List<EventData> parsedEventData;
  
	  static final int EVENT_NUMBER_COLUMN = 2;
	  static final int TIME_COLUMN = 4;

	  private String parsedEventNumber;
	  private String parsedTime;
  
	  private String currentDiv = null;
	  private int divDepth = 0;

	  private boolean inContentDiv = false;
	  private int contentDivDepth = 0;
	  private boolean inTable = false;
	  private boolean inTableRow = false;
    private boolean inTableData = false;
	  private int columnNumber = 0;
	  private boolean inCaption = false;
	  private boolean inAllResultsTable = false;
    private boolean inH2 = false;

    RunPageParserCallback() {
      parsedEventData = new ArrayList<>();
    }
  
    public String getParsedName(){
      return parsedName;
    }
    
    public List<EventData> getParsedEventData(){
      return parsedEventData;
    }
    
	  public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos){
	    //System.out.println("Start HTML.Tag " + t);
	    if (t == HTML.Tag.DIV) {
		    divDepth++;
		    String divName = (String)a.getAttribute(HTML.Attribute.ID);
		    if (divName != null && divName.contains(DIV1_MAIN)) {
  			  System.out.println(" **** " + divName);
	  		  if (currentDiv == null) {
		  	    currentDiv = DIV1_MAIN;
			    } else {
			    }
		    } else if (divName != null && divName.contains(DIV2_PRIMARY)) {
			    System.out.println(" **** " + divName);
			    if (currentDiv == DIV1_MAIN) {
			      currentDiv = DIV2_PRIMARY;
			    } else {
			    }
		    } else if (divName != null && divName.contains(DIV3_CONTENT)) {
			    System.out.println(" **** " + divName);
			    if (currentDiv == DIV2_PRIMARY) {
			      currentDiv = DIV3_CONTENT;
			      contentDivDepth = divDepth;
			      inContentDiv = true;
			    } else {
			    }
		    }
	    }
	    if (t == HTML.Tag.TABLE) {
		    inTable = true;
		    System.out.println("Table Start");
	    }
	    if (t == HTML.Tag.CAPTION) {
		    inCaption = true;
		    System.out.println("Caption Start");
      }
	    if (t == HTML.Tag.TR) {
		    inTableRow = true;
		    columnNumber = 0;
	    }
	    if (t == HTML.Tag.TD) {
		    inTableData = true;
		    columnNumber++;
	    }
      if (t == HTML.Tag.H2) {
        inH2 = true;
        System.out.println("H2 Start");
      }
	  }

	  public void handleEndTag(HTML.Tag t, int pos){
	    if (t == HTML.Tag.DIV) {
		    divDepth--;
	    }
	    if (t == HTML.Tag.TABLE) {
		    inTable = false;
		    inAllResultsTable = false;
		    System.out.println("Table End");
	    }
	    if (t == HTML.Tag.CAPTION) {
		    inCaption = false;
		    System.out.println("Caption End");
      }
	    if (t == HTML.Tag.TR) {
		    inTableRow = false;
		    if (inAllResultsTable) {
			    System.out.println(parsedEventNumber + " " + parsedTime);
			    parsedEventData.add(new EventData(parsedEventNumber, parsedTime));
		    }
	    }
	    if (t == HTML.Tag.TD) {
		    inTableData = false;
	    }
      if (t == HTML.Tag.H2) {
        inH2 = false;
        System.out.println("H2 End");
      }
	  }

	  public void handleText(char[] data, int pos){
      if (inContentDiv && inH2 && parsedName == null) {
        String header2Data = new String(data);
		    System.out.println("H2 Text: " + header2Data);
        parsedName = header2Data;
      }
		  if (inContentDiv && inTable && inCaption) {
		    String captionData = new String(data);
		    System.out.println("Caption Text: " + captionData);
		    if (captionData.contains("All Results at Dulwich")) {
          System.out.println(" **** " + captionData);
			    inAllResultsTable = true;
		    } else {
			    inAllResultsTable = false;
			  }
		  }
		  if (inAllResultsTable && inTableRow && inTableData) {
		    String tableData = new String(data);
		    if (columnNumber == EVENT_NUMBER_COLUMN) {
			    parsedEventNumber = new String(tableData);
		    } else if (columnNumber == TIME_COLUMN){
		      parsedTime = new String(tableData);
		    }
		  }
	  }
  }
  
  private void run(){
    System.out.println("Run");

    runners = new ArrayList<String>();
    runners.add("690790");
    runners.add("1198163");

    runnersEvents = new HashMap<>();
    names = new ArrayList<>();

    for (String runner : runners) {

      String url = String.format(URL_FORMAT, URL_EVENT_NAME, runner);
      System.out.println(url);
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format(URL_FORMAT, URL_EVENT_NAME, runner)))
        .setHeader("User-Agent", USER_AGENT)
        .build();

      String responseString = "";
      try {
        HttpResponse<String> response = HttpClient
          .newBuilder()
          .build()
          .send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HttpResponse " + response.statusCode());
        responseString = response.body();
        //System.out.println(responseString);
      } catch (IOException | InterruptedException ex) {
        System.out.println("HTTP Request Exception " + ex);
        return;
      }

      StringReader r = new StringReader(responseString);
      HTMLEditorKit.Parser parse = new RunPageParser().getParser();

      RunPageParserCallback callback = new RunPageParserCallback();
      try {
        parse.parse(r, callback, true);
      } catch (IOException ex) {
        System.out.println("Parser Exception " + ex);
        return;
      }

      System.out.println("Events found " + callback.getParsedEventData().size());
      runnersEvents.put(runner, callback.getParsedEventData());
      names.add(callback.getParsedName());
    }
    
    dumpAllRunners();
  }
  
  private void dumpAllRunners() {
    
    List<JSONObject> runnerData = new ArrayList<>();
    for (int i = 0; i < runners.size(); ++i) {
      String runner = runners.get(i);
      JSONObject jsonRunner = new JSONObject();
      jsonRunner.put("id", runner);
      jsonRunner.put("name", names.get(i).trim());
      List<JSONObject> eventData = new ArrayList<>();
      for (EventData event : runnersEvents.get(runner)) {
        JSONObject jsonEvent = new JSONObject(); 
        jsonEvent.put("event", event.eventNumber);
        jsonEvent.put("time", event.time);
        eventData.add(jsonEvent);
      }
      JSONArray jsonEventArray = new JSONArray(eventData);
      jsonRunner.put("events", jsonEventArray);
      runnerData.add(jsonRunner);
      System.out.println("Single runner:\n" + jsonRunner);
    }
    JSONArray jsonRunnerArray = new JSONArray(runnerData);
    JSONObject all = new JSONObject();
    all.put("runners", jsonRunnerArray);
    System.out.println("All runners:\n" + all);
    
    try {
      FileWriter fileWriter = new FileWriter("RunnerData.json");
      all.write(fileWriter);
      fileWriter.close();
    } catch (IOException ex) {
        System.out.println("File Writer Exception " + ex);
        return;
    }
  }

  public static void main(String args[])
  {
    ReadRunData readRunData = new ReadRunData();
    readRunData.run();
  }
}