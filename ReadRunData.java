import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.MutableAttributeSet;

class ReadRunData
{
  static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";

  List<String> runners;
  Map<String, List<EventData>> runnersEvents;

  class EventData {
    public final String eventNumber;
    public final String time;

    EventData(String eventNumber, String time){
      this.eventNumber = eventNumber;
      this.time = time;
    }
  }

  List<EventData> currentEventData;

  class RunPageParserCallback extends HTMLEditorKit.ParserCallback {

	static final String DIV1_MAIN = "main";
	static final String DIV2_PRIMARY = "primary";
	static final String DIV3_CONTENT = "content";

	static final int EVENT_NUMBER_COLUMN = 2;
	static final int TIME_COLUMN = 4;

	String currentEventNumber;
	String currentTime;

	String currentDiv = null;
	int divDepth = 0;

	boolean inContentDiv = false;
	int contentDivDepth = 0;
	boolean inTable = false;
	boolean inTableRow = false;
  boolean inTableData = false;
	int columnNumber = 0;
	boolean inCaption = false;
	boolean inAllResultsTable = false;

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
			    System.out.println(currentEventNumber + " " + currentTime);
			    currentEventData.add(new EventData(currentEventNumber, currentTime));
		    }
	    }
	    if (t == HTML.Tag.TD) {
		    inTableData = false;
	    }
	  }

	  public void handleText(char[] data, int pos){
		  if (inContentDiv && inTable && inCaption) {
		    String captionData = new String(data);
		    System.out.println(captionData);
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
			    currentEventNumber = new String(tableData);
		    } else if (columnNumber == TIME_COLUMN){
		      currentTime = new String(tableData);
		    }
		  }
	  }
  }
  
  private void run(){
    System.out.println("Run");

	  runners = new ArrayList<String>();
	  runners.add("690790");
	
	  runnersEvents = new HashMap<>();

	
    for (String runner : runners) {
	
	    currentEventData = new ArrayList<>();
	
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(String.format("https://www.parkrun.org.uk/dulwich/parkrunner/%s/",runner)))
        .setHeader("User-Agent", USER_AGENT)
        .build();

      String responseString = "";
      try {
        HttpResponse<String> response = HttpClient
          .newBuilder()
          .build()
          .send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HttpResponse " + response.statusCode());
        responseString = (String) response.body();
        System.out.println(responseString);
      } catch (IOException | InterruptedException ex) {
      }

      StringReader r = new StringReader(responseString);
      HTMLEditorKit.Parser parse = new RunPageParser().getParser();

      try {
        parse.parse(r, new RunPageParserCallback(), true);
      } catch (IOException ex) {
      }

	    System.out.println("Events found " + currentEventData.size());
      runnersEvents.put(runner, currentEventData);
	  }
  }

  public static void main(String args[])
  {
    ReadRunData readRunData = new ReadRunData();
    readRunData.run();
  }
}